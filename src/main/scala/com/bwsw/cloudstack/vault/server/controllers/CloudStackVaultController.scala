package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Key
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackVaultController(vaultService: VaultService,
                                cloudStackService: CloudStackService,
                                zooKeeperService: ZooKeeperService) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def handleAccountDelete(accountId: UUID): Unit = {
    logger.debug(s"handleAccountDelete(accountId: $accountId)")

    val defaultPath = s"${RequestPath.accountSecret}$accountId"
    deleteTokenAndAssociatedData(accountId, defaultPath)
    logger.info(s"Vm deletion was processed, accountId: $accountId)")
  }

  def handleVmDelete(vmId: UUID): Unit = {
    logger.debug(s"handleVmDelete(vmId: $vmId)")
    val defaultPath = s"${RequestPath.vmSecret}$vmId"
    deleteTokenAndAssociatedData(vmId, defaultPath)
    logger.info(s"Vm deletion was processed, vmId: $vmId)")
  }

  def handleUserCreate(userId: UUID): Unit = {
    import Tag.Key
    logger.debug(s"handleUserCreate(userId: $userId)")

    val userTags = cloudStackService.getUserTagByUserId(userId)

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
        val accountId = cloudStackService.getAccountIdForUser(userId)
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

          writeTokensToZooKeeperNodes(tokenWithKey :: Nil, accountId)
          logger.info("Tokens were wrote into node of zooKeeper")
          tokenWithKey
        }

      case (Some(entity), key) =>
        logger.info("Tag with token has existed in cloudStack user tags")
        (entity, key)
    }

    writeTokensToCloudStackTags(tokenWithTagKey, userId, Tag.Type.User)
    logger.info("Token was wrote into cloudStack user tag")
  }

  def handleAccountCreate(accountId: UUID): Unit = {
    import Tag.Key
    logger.debug(s"handleAccountCreate(accountId: $accountId)")

    val readPolicy = Policy.createAccountReadPolicy(accountId)
    val writePolicy = Policy.createAccountWritePolicy(accountId)

    val tokenWithTagKeyList = createTokenListByPolicies(List(
      (readPolicy, Key.VaultRO),
      (writePolicy, Key.VaultRW)
    ))
    logger.info("Tokens were created by vault")

    val userIdList = cloudStackService.getUserIdListForAccount(accountId)

    writeTokensToZooKeeperNodes(tokenWithTagKeyList, accountId)
    logger.info("Tokens were wrote into zooKeeper's nodes")

    userIdList.foreach { x =>
      writeTokensToCloudStackTags(tokenWithTagKeyList, x, Tag.Type.UserVM)
      logger.info("Token were wrote into cloudStack user's tags")
    }
  }

  def handleVmCreate(vmId: UUID): Unit = {
    import Tag.Key
    logger.debug(s"handleVmCreate(vmId: $vmId)")

    val accountId = cloudStackService.getAccountIdForVm(vmId)
    val readPolicy = Policy.createVmReadPolicy(accountId, vmId)
    val writePolicy = Policy.createVmWritePolicy(accountId, vmId)

    val tokenWithTagKeyList = createTokenListByPolicies(List(
      (readPolicy, Key.VaultRO),
      (writePolicy, Key.VaultRW)
    ))
    logger.info("Tokens were created by vault")

    writeTokensToZooKeeperNodes(tokenWithTagKeyList, vmId)
    logger.info("Tokens were wrote into zooKeeper's nodes")

    writeTokensToCloudStackTags(tokenWithTagKeyList, vmId, Tag.Type.UserVM)
    logger.info("Token were wrote into cloudStack userVM's tags")
  }

  private def deleteTokenAndAssociatedData(entityId: UUID, defaultPathToSecret: String): Unit = {
    val pathToTokenDataList = List(Key.VaultRO, Key.VaultRW).map { x =>
      s"${RequestPath.zooKeeperRootNode}/$entityId/${Key.toString(x)}"
    }

    pathToTokenDataList.foreach { path =>
      val isExistNodeWithToken = zooKeeperService.isExistNode(path)
      if (isExistNodeWithToken) {
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

  private def getTokensFromZooKeeperNodes(entityId: UUID, keyList: List[Tag.Key]): List[UUID] = {
    keyList.map { key =>
      s"${RequestPath.zooKeeperRootNode}/$entityId/${Tag.Key.toString(key)}"
    }.map { path =>
      UUID.fromString(zooKeeperService.getData(path))
    }
  }

  private def createTokenListByPolicies(policiesWithTagKey: List[(Policy, Tag.Key)]): List[(UUID, Tag.Key)] = {
    logger.debug(s"createTokenListByPolicies(policiesWithTagKey: $policiesWithTagKey)")
    policiesWithTagKey.map {
      case (policy, tokenTagKey) =>
        val token = vaultService.createToken(policy :: Nil)
        (token, tokenTagKey)
    }
  }

  private def writeTokensToCloudStackTags(tokenWithTagKeyList: List[(UUID, Tag.Key)],
                                          entityId: UUID,
                                          tagType: Tag.Type) = {
    logger.debug(s"writeTokensToCloudStackTags(" +
      s"tokenWithTagKeyList: $tokenWithTagKeyList, " +
      s"entityId: $entityId, " +
      s"tagType: $tagType)")

    val tagList = tokenWithTagKeyList.map {
      case (tokenId, tokenTagKey) =>
        Tag.createTag(tokenTagKey, tokenId.toString)
    }

    tagList.foreach { x =>
      cloudStackService.setTagToResourse(x, entityId, tagType)
    }
  }

  private def writeTokensToZooKeeperNodes(tokenWithTagKeyList: List[(UUID, Tag.Key)], entityId: UUID) = {
    logger.debug(s"writeTokensToZooKeeperNodes(" +
      s"tokenWithTagKeyList: $tokenWithTagKeyList, " +
      s"entityId $entityId)")

    if (!zooKeeperService.isExistNode(RequestPath.zooKeeperRootNode)) {
      zooKeeperService.createNode(RequestPath.zooKeeperRootNode, "")
    }
    zooKeeperService.createNode(s"${RequestPath.zooKeeperRootNode}/$entityId", "")

    tokenWithTagKeyList.foreach {
      case (tokenId, tokenTagKey) =>
        Try {
          zooKeeperService.createNode(s"${RequestPath.zooKeeperRootNode}/$entityId/$tokenTagKey", s"$tokenId")
        } match {
          case Success(_) =>
          case Failure(e: Throwable) =>
            logger.error(s"Token could not to wrote into zooKeeper node, exception was thrown: $e")
            logger.debug(s"Token will be revoked")
            vaultService.revokeToken(tokenId)
            throw e
        }
    }
  }
}
