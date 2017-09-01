package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Key
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
    import Tag.Key
    logger.debug(s"handleUserCreate(userId: $userId)")

    val accountId = cloudStackService.getAccountIdByUserId(userId)
    val userTags = cloudStackService.getUserTagsByAccountId(accountId)

    val optionReadToken: Option[UUID] = userTags.find(_.key == Key.VaultRO).map { x =>
      UUID.fromString(x.value)
    }
    val optionWriteToken: Option[UUID] = userTags.find(_.key == Key.VaultRW).map { x =>
      UUID.fromString(x.value)
    }

    val optionTokenWithTagKeyList = List(
      (optionReadToken, Key.VaultRO),
      (optionWriteToken, Key.VaultRW)
    )

    val tokenWithTagKey: List[(UUID, Tag.Key)] = optionTokenWithTagKeyList.map {
      case (None, key) =>
        logger.info("Tag with token has not existed")
        val pathToData = s"${RequestPath.zooKeeperRootNode}/$accountId/${Key.toString(key)}"
        val isExistNodeWithToken = zooKeeperService.isExistNode(pathToData)

        if (isExistNodeWithToken) {
          val tokenFromNode = UUID.fromString(zooKeeperService.getData(pathToData))
          logger.info("Token was got from node of zooKeeper")
          (tokenFromNode, key)
        } else {
          logger.info("Node with token has not existed")
          val tokenWithKey = key match {
            case Key.VaultRO =>
              val readPolicy = Policy.createAccountReadPolicy(accountId)
              val readToken = vaultService.createToken(readPolicy :: Nil)
              logger.info("readToken was created by vault")
              (readToken, key)
            case Key.VaultRW =>
              val writePolicy = Policy.createAccountWritePolicy(accountId)
              val writeToken = vaultService.createToken(writePolicy :: Nil)
              logger.info("writeToken was created by vault")
              (writeToken, key)
          }

          //writeTokensToZooKeeperNodes(tokenWithKey :: Nil, accountId)
          logger.info("Tokens were wrote into node of zooKeeper")
          tokenWithKey
        }

      case (Some(entity), key) =>
        logger.info("Tag with token has existed in cloudStack user tags")
        (entity, key)
    }

  //  writeTokensToCloudStackTags(tokenWithTagKey, userId, Tag.Type.User)
    logger.info("Token was wrote into cloudStack user tag")
  }

  def handleAccountCreate(accountId: UUID): Unit = {
    logger.debug(s"handleAccountCreate(accountId: $accountId)")
    import Tag.Key

    val usersIds = cloudStackService.getUserIdsByAccountId(accountId)
    val accountTags = usersIds.flatMap { x =>
      cloudStackService.getUserTagsByUserId(x)
    }

    if(!accountTags.exists(_.key == Key.VaultRO)) {
      val tag = createAbsentTag(Key.VaultRO)
      usersIds.foreach { x =>
        cloudStackService.setResourseTag(x, Tag.Type.User, tag :: Nil)
      }
    }

    if(!accountTags.exists(_.key == Key.VaultRW)) {
      val tag = createAbsentTag(Key.VaultRW)
      usersIds.foreach { x =>
        cloudStackService.setResourseTag(x, Tag.Type.User, tag :: Nil)
      }
    }

    def createAbsentTag(tagKey: Tag.Key) = {
      val policy = tagKey match {
        case Key.VaultRO =>
          Policy.createAccountReadPolicy(accountId)
        case Key.VaultRW =>
          Policy.createAccountWritePolicy(accountId)
      }
      val token = getToken(accountId, accountEntityName, policy)
      writeTokenToZooKeeperNode(token, policy.acl, accountId, accountEntityName)
      Tag.createTag(tagKey, token.toString)
    }
  }

  def handleVmCreate(vmId: UUID): Unit = {
    logger.debug(s"handleVmCreate(vmId: $vmId)")
    import Tag.Key

    val accountId = cloudStackService.getAccountIdByVmId(vmId)

    def createAbsentTag(tagKey: Tag.Key) = {
      val policy = tagKey match {
        case Key.VaultRO =>
          Policy.createVmReadPolicy(accountId, vmId)
        case Key.VaultRW =>
          Policy.createVmWritePolicy(accountId, vmId)
      }
      val token = getToken(vmId, vmEntityName, policy)
      writeTokenToZooKeeperNode(token, policy.acl, vmId, vmEntityName)
      Tag.createTag(tagKey, token.toString)
    }

    val tagTokenRO: Tag = createAbsentTag(Key.VaultRO)
    val tagTokenRW: Tag = createAbsentTag(Key.VaultRW)

    cloudStackService.setResourseTag(vmId, Tag.Type.UserVM, List(tagTokenRO, tagTokenRW))
  }

  private def getToken(entityId: UUID, entityName: String, policy: Policy): UUID = {
    val pathToData = getPathToZookeeperNodeWithToken(entityId.toString, entityName, policy.acl)
    if(zooKeeperService.isExistNode(pathToData)) {
      UUID.fromString(zooKeeperService.getData(pathToData))
    } else {
      vaultService.createToken(policy :: Nil)
    }
  }

  private def deleteTokenAndAssociatedData(entityId: UUID, entityName: String, defaultPathToSecret: String): Unit = {
    val pathsToTokenData = List(Policy.ACL.Read, Policy.ACL.Write).map { x =>
      getPathToZookeeperNodeWithToken(entityId.toString, entityName, x)
    }
    pathsToTokenData.foreach { path =>
      if (zooKeeperService.isExistNode(path)) {
        val tokenFromNode = UUID.fromString(zooKeeperService.getData(path))
        logger.debug("Token was got from node of zooKeeper")
        val pathToSecretFromToken = vaultService.revokeToken(tokenFromNode)
        vaultService.deleteSecret(pathToSecretFromToken)
        zooKeeperService.deleteNode(path)
      } else {
        logger.error(s"Token has not exist in zooKeeper by path: $path")
        vaultService.deleteSecret(defaultPathToSecret)
      }
    }
  }

  private def getPathToZookeeperNodeWithToken(entityId: String, entityName: String, policyAcl: Policy.ACL) =
    s"${RequestPath.zooKeeperRootNode}/$entityName/$entityId/${Policy.ACL.toString(policyAcl)}"
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


  private def writeTokenToZooKeeperNode(token: UUID, policyAcl: Policy.ACL, entityId: UUID, entityName: String) = {
    logger.debug(s"writeTokensToZooKeeperNodes(entityId: $entityId, entityName: $entityName)")
    Try {
      zooKeeperService.createNodeWithData(s"${RequestPath.zooKeeperRootNode}/$entityName/$entityId", "")
      zooKeeperService.createNodeWithData(getPathToZookeeperNodeWithToken(entityId.toString, entityName, policyAcl), s"$token")
    } match {
      case Success(_) =>
      case Failure(e: ZooKeeperCriticalException) =>
        logger.error(s"Path node could not to create into zooKeeper, exception was thrown: ${e.exception}")
        logger.warn(s"token will be revoked")
        vaultService.revokeToken(token)
        throw e
    }
  }
}
