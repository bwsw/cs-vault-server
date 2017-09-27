/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for executing business logic (see docs/logic.md).
  *
  * @param vaultService allows for interaction with the Vault server
  * @param cloudStackService allows for interaction with the CloudStack server
  * @param zooKeeperService allows for interaction with the ZooKeeper server
  */
class CloudStackVaultController(vaultService: VaultService,
                                cloudStackService: CloudStackService,
                                zooKeeperService: ZooKeeperService) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val accountEntityName = "account"
  private val vmEntityName = "vm"

  /**
    * Revoke token and delete secret in Vault server.
    * For details see part "Account or VM deletion event processing" in docs/logic.md
    *
    * @param accountId account id in CloudStack server for which vault token was created
    */
  def handleAccountDelete(accountId: UUID): Unit = {
    logger.debug(s"handleAccountDelete(accountId: $accountId)")
    val defaultSecretPath = s"${RequestPath.accountSecret}$accountId"
    deleteTokenAndAssociatedData(accountId, accountEntityName, defaultSecretPath)
    logger.info(s"Account deletion was processed, accountId: $accountId)")
  }

  /**
    * Revoke token and delete secret in Vault server.
    * For details see part "Account or VM deletion event processing" in docs/logic.md
    *
    * @param vmId virtual machine id in CloudStack server for which vault token was created
    */
  def handleVmDelete(vmId: UUID): Unit = {
    logger.debug(s"handleVmDelete(vmId: $vmId)")
    val defaultSecretPath = s"${RequestPath.vmSecret}$vmId"
    deleteTokenAndAssociatedData(vmId, vmEntityName, defaultSecretPath)
    logger.info(s"Vm deletion was processed, vmId: $vmId)")
  }

  /**
    * Handles token creating for the user account.
    * For details see part "User creation event processing" in docs/logic.md
    *
    * @param userId user id in CloudStack server
    */
  def handleUserCreate(userId: UUID): Unit = {
    logger.debug(s"handleUserCreate(userId: $userId)")

    val accountId = cloudStackService.getAccountIdByUserId(userId)
    val accountTags = cloudStackService.getUserTagsByAccountId(accountId)

    val currentTokenTags = accountTags.filter { tag =>
      tag.key == Tag.Key.VaultRO || tag.key == Tag.Key.VaultRW
    }.toSet

    val absentTokenTagKeyList = List(Tag.Key.VaultRO, Tag.Key.VaultRW).filterNot { x =>
      currentTokenTags.exists(_.key == x)
    }

    val newTags = if (absentTokenTagKeyList.nonEmpty) {
      createMissingAccountTokenTags(accountId, absentTokenTagKeyList)
    } else {
      List.empty[Tag]
    }

    cloudStackService.setResourceTag(userId, Tag.Type.User, currentTokenTags.toList ::: newTags)
    logger.info(s"User creation was processed, userId: $userId)")
  }

  /**
    * Handles token creating for account.
    * For details see part "Account creation event processing" in docs/logic.md
    *
    * @param accountId account id in CloudStack server
    */
  def handleAccountCreate(accountId: UUID): Unit = {
    logger.debug(s"handleAccountCreate(accountId: $accountId)")

    val usersIds = cloudStackService.getUserIdsByAccountId(accountId)
    val accountTags = usersIds.flatMap { x =>
      cloudStackService.getUserTagsByUserId(x)
    }

    val currentTokenTags = accountTags.filter { tag =>
      tag.key == Tag.Key.VaultRO || tag.key == Tag.Key.VaultRW
    }.toSet

    val absentTokenTagKeyList = List(Tag.Key.VaultRO, Tag.Key.VaultRW).filterNot { x =>
      currentTokenTags.exists(_.key == x)
    }

    if (absentTokenTagKeyList.nonEmpty) {
      val newTags = createMissingAccountTokenTags(accountId, absentTokenTagKeyList)
      usersIds.foreach { userId =>
        cloudStackService.setResourceTag(userId, Tag.Type.User, newTags)
      }
    }
    logger.info(s"Account creation was processed, accountId: $accountId)")
  }

  /**
    * Handles token creating for the virtual machine.
    * For details see part "Virtual machine creation event processing" in docs/logic.md
    *
    * @param vmId virtual machine id in CloudStack server
    */
  def handleVmCreate(vmId: UUID): Unit = {
    logger.debug(s"handleVmCreate(vmId: $vmId)")

    val accountId = cloudStackService.getAccountIdByVmId(vmId)

    val policyList = List(
      Policy.createVmReadPolicy(accountId, vmId),
      Policy.createVmWritePolicy(accountId, vmId)
    )

    val tokenTagList = policyList.map { x =>
      val pathToData = getPathToZookeeperNodeWithToken(vmId.toString, vmEntityName, getTagKeyByPolicyACL(x.acl))
      zooKeeperService.getDataIfNodeExist(pathToData) match {
        case Some(token) =>
          Tag.createTag(getTagKeyByPolicyACL(x.acl), token)
        case None =>
          val token = vaultService.createToken(x :: Nil)
          val tag = Tag.createTag(getTagKeyByPolicyACL(x.acl), token.toString)
          writeTokenToZooKeeperNode(pathToData, token)
          tag
      }
    }

    cloudStackService.setResourceTag(vmId, Tag.Type.UserVM, tokenTagList)
    logger.info(s"VM creation was processed, vmId: $vmId)")
  }

  /**
    * Aligns policy acl in Vault and tag key in CloudStack
    */
  private def getTagKeyByPolicyACL(acl: Policy.ACL): Tag.Key = {
    acl match {
      case Policy.ACL.Read => Tag.Key.VaultRO
      case Policy.ACL.Write => Tag.Key.VaultRW
    }
  }

  /**
    * Creates token in Vault or gets it from ZooKeeper node for CloudStack entity which does not includes tag with it
    */
  private def createMissingAccountTokenTags(accountId: UUID, absentTagKeyList: List[Tag.Key]): List[Tag] = {
    logger.debug(s"createMissingAccountTokenTags(accountId: $accountId, absentTagKeyList: $absentTagKeyList)")
    import Tag.Key

    val newTags = absentTagKeyList.map { x =>
      val pathToToken = getPathToZookeeperNodeWithToken(accountId.toString, accountEntityName, x)
      zooKeeperService.getDataIfNodeExist(pathToToken) match {
        case Some(token) =>
          Tag.createTag(x, token)
        case None =>
          val policy = x match {
            case Key.VaultRO =>
              Policy.createAccountReadPolicy(accountId)
            case Key.VaultRW =>
              Policy.createAccountWritePolicy(accountId)
          }
          val token = vaultService.createToken(policy :: Nil)
          val tag = Tag.createTag(x, token.toString)
          writeTokenToZooKeeperNode(pathToToken, token)
          tag
      }
    }

    newTags
  }

  /**
    * Revokes token and deletes secret in Vault
    */
  private def deleteTokenAndAssociatedData(entityId: UUID, entityName: String, defaultSecretPath: String): Unit = {
    logger.debug(s"deleteTokenAndAssociatedData(entityId: $entityId, entityName: $entityName)")
    var secretPath = defaultSecretPath
    val pathToEntityNode = getPathToZookeeperEntityNode(entityId.toString, entityName)

    if (zooKeeperService.isExistNode(pathToEntityNode)) {
      val pathsToTokenData = List(Tag.Key.VaultRO, Tag.Key.VaultRW).map { x =>
        getPathToZookeeperNodeWithToken(entityId.toString, entityName, x)
      }
      pathsToTokenData.foreach { path =>
        zooKeeperService.getDataIfNodeExist(path) match {
          case Some(token) =>
            secretPath = vaultService.revokeToken(UUID.fromString(token))
          case None =>
            logger.warn(s"Node with token does not exist for entity: $entityId")
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

  private def writeTokenToZooKeeperNode(path: String, token: UUID) = {
    logger.debug(s"writeTokensToZooKeeperNode(path: $path)")
    Try {
      zooKeeperService.createNodeWithData(path, token.toString)
    } match {
      case Success(_) =>
      case Failure(e: Throwable) =>
        logger.error(s"Path node could not to create into zooKeeper, exception was thrown: $e")
        logger.warn(s"token will be revoked")
        vaultService.revokeToken(token)
        throw e
    }
  }
}
