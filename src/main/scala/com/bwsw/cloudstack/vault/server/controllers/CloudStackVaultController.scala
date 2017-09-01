package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackVaultController(vaultService: VaultService,
                                cloudStackService: CloudStackService,
                                zooKeeperService: ZooKeeperService) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val accountEntityName = "account"
  private val vmEntityName = "vm"
  initializeZooKeeperNodes()

  def handleAccountDelete(accountId: UUID): Unit = {
    logger.debug(s"handleAccountDelete(accountId: $accountId)")
    val defaultSecretPath = s"${RequestPath.accountSecret}$accountId"
    deleteTokenAndAssociatedData(accountId, accountEntityName, defaultSecretPath)
    logger.info(s"Account deletion was processed, accountId: $accountId)")
  }

  def handleVmDelete(vmId: UUID): Unit = {
    logger.debug(s"handleVmDelete(vmId: $vmId)")
    val defaultSecretPath = s"${RequestPath.vmSecret}$vmId"
    deleteTokenAndAssociatedData(vmId, vmEntityName, defaultSecretPath)
    logger.info(s"Vm deletion was processed, vmId: $vmId)")
  }

  def handleUserCreate(userId: UUID): Unit = {
    logger.debug(s"handleUserCreate(userId: $userId)")

    val accountId = cloudStackService.getAccountIdByUserId(userId)
    val accountTags = cloudStackService.getUserTagsByAccountId(accountId)

    val newTags = createMissingAccountTokenTags(accountId, accountTags)

    if (newTags.nonEmpty) {
      cloudStackService.setResourseTag(userId, Tag.Type.User, newTags)
    }
  }

  def handleAccountCreate(accountId: UUID): Unit = {
    logger.debug(s"handleAccountCreate(accountId: $accountId)")

    val usersIds = cloudStackService.getUserIdsByAccountId(accountId)
    val accountTags = usersIds.flatMap { x =>
      cloudStackService.getUserTagsByUserId(x)
    }

    val newTags = createMissingAccountTokenTags(accountId, accountTags)

    if (newTags.nonEmpty) {
      usersIds.foreach { userId =>
        cloudStackService.setResourseTag(userId, Tag.Type.User, newTags)
      }
    }
  }

  def handleVmCreate(vmId: UUID): Unit = {
    logger.debug(s"handleVmCreate(vmId: $vmId)")

    val accountId = cloudStackService.getAccountIdByVmId(vmId)

    val policyList = List(
      Policy.createVmReadPolicy(accountId, vmId),
      Policy.createVmWritePolicy(accountId, vmId)
    )

    val pathToEntityNode = getPathToZookeeperEntityNode(vmId.toString, vmEntityName)
    val tokenTagList = if (!zooKeeperService.isExistNode(pathToEntityNode)) {
      zooKeeperService.createNodeWithData(pathToEntityNode, "")
      policyList.map { x =>
        val token = vaultService.createToken(x :: Nil)
        val tag = Tag.createTag(getTagKeyByPolicyACL(x.acl), token.toString)
        writeTokenToZooKeeperNode(tag, vmId, vmEntityName)
        tag
      }
    } else {
      policyList.map { x =>
        val pathToData = getPathToZookeeperNodeWithToken(vmId.toString, vmEntityName, getTagKeyByPolicyACL(x.acl))
        if (zooKeeperService.isExistNode(pathToData)) {
          val token = UUID.fromString(zooKeeperService.getData(pathToData))
          Tag.createTag(getTagKeyByPolicyACL(x.acl), token.toString)
        } else {
          val token = vaultService.createToken(x :: Nil)
          val tag = Tag.createTag(getTagKeyByPolicyACL(x.acl), token.toString)
          writeTokenToZooKeeperNode(tag, vmId, vmEntityName)
          tag
        }
      }
    }

    cloudStackService.setResourseTag(vmId, Tag.Type.UserVM, tokenTagList)
  }


  private def getTagKeyByPolicyACL(acl: Policy.ACL): Tag.Key = {
    acl match {
      case Policy.ACL.Read => Tag.Key.VaultRO
      case Policy.ACL.Write => Tag.Key.VaultRW
    }
  }

  private def createMissingAccountTokenTags(accountId: UUID, accountTags: List[Tag]): List[Tag] = {
    import Tag.Key
    val absentTagKeyList = List(Key.VaultRW, Key.VaultRO).filterNot { x =>
      accountTags.exists(_.key == x)
    }

    val pathToEntityNode = getPathToZookeeperEntityNode(accountId.toString, accountEntityName)
    val isExistEntityNode = zooKeeperService.isExistNode(pathToEntityNode)
    if (!isExistEntityNode) {
      zooKeeperService.createNodeWithData(pathToEntityNode, "")
    }

    val newTags = absentTagKeyList.map { x =>
      val pathToToken = getPathToZookeeperNodeWithToken(accountId.toString, vmEntityName, x)
      if (isExistEntityNode && zooKeeperService.isExistNode(pathToToken)) {
        val token = UUID.fromString(zooKeeperService.getData(pathToToken))
        Tag.createTag(x, token.toString)
      } else {
        val policy = x match {
          case Key.VaultRO =>
            Policy.createAccountReadPolicy(accountId)
          case Key.VaultRW =>
            Policy.createAccountWritePolicy(accountId)
        }
        val token = vaultService.createToken(policy :: Nil)
        val tag = Tag.createTag(x, token.toString)
        writeTokenToZooKeeperNode(tag, accountId, accountEntityName)
        tag
      }
    }

    newTags
  }

  private def deleteTokenAndAssociatedData(entityId: UUID, entityName: String, defaultSecretPath: String): Unit = {
    var secretPath = defaultSecretPath
    val pathToEntityNode = getPathToZookeeperEntityNode(entityId.toString, entityName)
    if (zooKeeperService.isExistNode(pathToEntityNode)) {
      val pathsToTokenData = List(Tag.Key.VaultRO, Tag.Key.VaultRW).map { x =>
        getPathToZookeeperNodeWithToken(entityId.toString, entityName, x)
      }
      pathsToTokenData.foreach { path =>
        if (zooKeeperService.isExistNode(path)) {
          val tokenFromNode = UUID.fromString(zooKeeperService.getData(path))
          secretPath = vaultService.revokeToken(tokenFromNode)
          zooKeeperService.deleteNode(path)
        }
      }
      zooKeeperService.deleteNode(pathToEntityNode)
    }
    vaultService.deleteSecret(secretPath)
  }

  private def getPathToZookeeperNodeWithToken(entityId: String, entityName: String, tagKey: Tag.Key) =
    s"${getPathToZookeeperEntityNode(entityId, entityName)}/${Tag.Key.toString(tagKey)}"

  private def getPathToZookeeperEntityNode(entityId: String, entityName: String) =
    s"${RequestPath.zooKeeperRootNode}/$entityName/$entityId"
/*
  private def createTokenListByPolicies(policiesWithTagKey: List[(Policy, Tag.Key)]): List[(UUID, Tag.Key)] = {
    logger.debug(s"createTokenListByPolicies(policiesWithTagKey: $policiesWithTagKey)")
    var createdTokens = List.empty[(UUID, Tag.Key)]
    Try {
      policiesWithTagKey.foreach {
        case (policy, tokenTagKey) =>
          val token = vaultService.createToken(policy :: Nil)
          createdTokens = createdTokens ::: (token, tokenTagKey) :: Nil
      }
    } match {
      case Success(x) => x
      case Failure(e: VaultCriticalException) =>
        logger.error(s"Token could not created, exception was thrown ${e.exception}. Tokens which already created will be revoked")
        createdTokens.foreach {
          case (token, tokenTagKey) =>
            vaultService.revokeToken(token)
        }
    }

    createdTokens
  }*/
  private def initializeZooKeeperNodes(): Unit = {
    if (!zooKeeperService.isExistNode(RequestPath.zooKeeperRootNode)) {
      zooKeeperService.createNodeWithData(RequestPath.zooKeeperRootNode, "")
    }
    if (!zooKeeperService.isExistNode(s"${RequestPath.zooKeeperRootNode}/$accountEntityName")) {
      zooKeeperService.createNodeWithData(s"${RequestPath.zooKeeperRootNode}/$accountEntityName", "")
    }
    if (!zooKeeperService.isExistNode(s"${RequestPath.zooKeeperRootNode}/$vmEntityName")) {
      zooKeeperService.createNodeWithData(s"${RequestPath.zooKeeperRootNode}/$vmEntityName", "")
    }
  }


  private def writeTokenToZooKeeperNode(tag: Tag, entityId: UUID, entityName: String) = {
    logger.debug(s"writeTokensToZooKeeperNodes(entityId: $entityId, entityName: $entityName)")
    Try {
      zooKeeperService.createNodeWithData(getPathToZookeeperNodeWithToken(entityId.toString, entityName, tag.key), s"${tag.value}")
    } match {
      case Success(_) =>
      case Failure(e: ZooKeeperCriticalException) =>
        logger.error(s"Path node could not to create into zooKeeper, exception was thrown: ${e.exception}")
        logger.warn(s"token will be revoked")
        vaultService.revokeToken(UUID.fromString(tag.value))
        throw e
    }
  }
}
