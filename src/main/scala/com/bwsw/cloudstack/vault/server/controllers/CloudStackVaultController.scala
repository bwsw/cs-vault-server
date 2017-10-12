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
                                zooKeeperService: ZooKeeperService,
                                settings: CloudStackVaultController.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val accountEntityName = "accounts"
  private val vmEntityName = "vms"
  private val vaultApiPath = s"${vaultService.vaultUrl}${RequestPath.vaultRoot}"

  /**
    * Revoke token and delete secret in Vault server.
    * For details see part "Account or VM deletion event processing" in docs/logic.md
    *
    * @param accountId account id in CloudStack server for which vault token was created
    */
  def handleAccountDelete(accountId: UUID): Unit = {
    logger.debug(s"handleAccountDelete(accountId: $accountId)")
    val requestSecretPath = s"${RequestPath.vaultRoot}${getAccountEntitySecretPath(accountId)}"
    deleteTokenAndAppropriateSecret(accountId, accountEntityName, requestSecretPath)
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
    val requestSecretPath = s"${RequestPath.vaultRoot}${getVmEntitySecretPath(vmId)}"
    deleteTokenAndAppropriateSecret(vmId, vmEntityName, requestSecretPath)
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
    val usersIds = cloudStackService.getUserIdsByAccountId(accountId)

    val currentVaultTags = getCurrentVaultTagsOfUsers(usersIds)

    val newVaultTokenTagKeyList = Set(Tag.Key.VaultRO, Tag.Key.VaultRW).collect {
      case tagKey if !currentVaultTags.exists(_.key == tagKey) => createMissingAccountTokenTag(accountId, tagKey)
    }

    val newVaultKeyspaceTagKeyList = Set(Tag.Key.VaultHost, Tag.Key.VaultPrefix).collect {
      case tagKey if !currentVaultTags.exists(_.key == tagKey) =>
        tagKey match {
          case Tag.Key.VaultHost => Tag.createTag(Tag.Key.VaultHost, vaultApiPath)
          case Tag.Key.VaultPrefix => Tag.createTag(Tag.Key.VaultPrefix, getAccountEntitySecretPath(accountId))
        }
    }

    val completeVaultTags = currentVaultTags ++ newVaultTokenTagKeyList ++ newVaultKeyspaceTagKeyList

    if (currentVaultTags.isEmpty) {
      usersIds.foreach { id =>
        cloudStackService.setResourceTags(id, Tag.Type.User, completeVaultTags)
      }
    } else {
      cloudStackService.setResourceTags(userId, Tag.Type.User, completeVaultTags)
    }
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

    val currentVaultTags = getCurrentVaultTagsOfUsers(usersIds)

    if (currentVaultTags.isEmpty) {
      val newVaultTokenTagKeySet = Set(Tag.Key.VaultRO, Tag.Key.VaultRW).collect {
        case tagKey if !currentVaultTags.exists(_.key == tagKey) => createMissingAccountTokenTag(accountId, tagKey)
      }

      if (usersIds.nonEmpty) {
        val newVaultKeyspaceTagKeySet = Set(Tag.Key.VaultHost, Tag.Key.VaultPrefix).collect {
          case tagKey if !currentVaultTags.exists(_.key == tagKey) =>
            tagKey match {
              case Tag.Key.VaultHost => Tag.createTag(Tag.Key.VaultHost, vaultApiPath)
              case Tag.Key.VaultPrefix => Tag.createTag(Tag.Key.VaultPrefix, getAccountEntitySecretPath(accountId))
            }
        }
        val completeVaultTags: Set[Tag] = newVaultTokenTagKeySet ++ newVaultKeyspaceTagKeySet

        usersIds.foreach { id =>
          cloudStackService.setResourceTags(id, Tag.Type.User, completeVaultTags)
        }
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
      Policy.createVmReadPolicy(accountId, vmId, settings.vmSecretPath),
      Policy.createVmWritePolicy(accountId, vmId, settings.vmSecretPath)
    )

    val vaultTokenTags = policyList.map { x =>
      val pathToData = createTokenEntityNodePath(vmId.toString, vmEntityName, getTagKeyByPolicyACL(x.acl))
      zooKeeperService.getNodeData(pathToData) match {
        case Some(token) =>
          Tag.createTag(getTagKeyByPolicyACL(x.acl), token)
        case None =>
          val token = vaultService.createToken(x :: Nil)
          val tag = Tag.createTag(getTagKeyByPolicyACL(x.acl), token.toString)
          writeTokenToZooKeeperNode(pathToData, token)
          tag
      }
    }.toSet

    val vaultKeyspaceTags = Set(
      Tag.createTag(Tag.Key.VaultHost, vaultApiPath),
      Tag.createTag(Tag.Key.VaultPrefix, getVmEntitySecretPath(vmId))
    )

    cloudStackService.setResourceTags(vmId, Tag.Type.UserVM, vaultTokenTags ++ vaultKeyspaceTags)
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
    * Get token tags for users
    */
  private def getCurrentVaultTagsOfUsers(usersIds: List[UUID]): Set[Tag] = {

    usersIds.flatMap { userId =>
      cloudStackService.getUserTagsByUserId(userId).filter { tag =>
        tag.key.oneOf(Tag.Key.VaultRO, Tag.Key.VaultRW, Tag.Key.VaultHost, Tag.Key.VaultPrefix)
      }
    }.toSet
  }

  /**
    * Creates token in Vault or gets it from ZooKeeper node for CloudStack entity which does not includes tag with it
    */
  private def createMissingAccountTokenTag(accountId: UUID, absentTagKey: Tag.Key): Tag = {
    logger.debug(s"createMissingAccountTokenTag(accountId: $accountId, absentTagKey: $absentTagKey)")
    import Tag.Key

    val pathToToken = createTokenEntityNodePath(accountId.toString, accountEntityName, absentTagKey)
    zooKeeperService.getNodeData(pathToToken) match {
      case Some(token) =>
        Tag.createTag(absentTagKey, token)
      case None =>
        val policy = absentTagKey match {
          case Key.VaultRO =>
            Policy.createAccountReadPolicy(accountId, settings.accountSecretPath)
          case Key.VaultRW =>
            Policy.createAccountWritePolicy(accountId, settings.accountSecretPath)
          case _ =>
            throw new IllegalArgumentException("tag key is wrong")
        }
        val token = vaultService.createToken(policy :: Nil)
        val tag = Tag.createTag(absentTagKey, token.toString)
        writeTokenToZooKeeperNode(pathToToken, token)
        tag
    }
  }

  /**
    * Revokes token and deletes secret in Vault, and remove entity node from ZooKeeper
    */
  private def deleteTokenAndAppropriateSecret(entityId: UUID, entityName: String, secretPath: String): Unit = {
    logger.debug(s"deleteTokenAndAppropriateSecret(entityId: $entityId, entityName: $entityName)")
    val pathToEntityNode = createEntityNodePath(entityId.toString, entityName)

    if (zooKeeperService.doesNodeExist(pathToEntityNode)) {
      val pathsToTokenData = List(Tag.Key.VaultRO, Tag.Key.VaultRW).map { x =>
        createTokenEntityNodePath(entityId.toString, entityName, x)
      }
      pathsToTokenData.foreach { path =>
        zooKeeperService.getNodeData(path) match {
          case Some(token) =>
            val policyNames = vaultService.revokeToken(UUID.fromString(token))
            policyNames.foreach(vaultService.deletePolicy)
          case None =>
            logger.warn(s"Node with token does not exist for entity: $entityId")
        }
      }
      zooKeeperService.deleteNode(pathToEntityNode)
    }
    vaultService.deleteSecretsRecursively(secretPath)
  }

  private def createTokenEntityNodePath(entityId: String, entityName: String, tagKey: Tag.Key) =
    s"${createEntityNodePath(entityId, entityName)}/${tagKey.toString.toLowerCase()}"

  private def createEntityNodePath(entityId: String, entityName: String) =
    s"${settings.zooKeeperRootNode}/$entityName/$entityId"

  private def getVmEntitySecretPath(vmId: UUID): String = {
    s"${settings.vmSecretPath}$vmId"
  }

  private def getAccountEntitySecretPath(accountId: UUID): String = {
    s"${settings.accountSecretPath}$accountId"
  }

  private def writeTokenToZooKeeperNode(path: String, token: UUID) = {
    logger.debug(s"writeTokensToZooKeeperNode(path: $path)")
    Try {
      zooKeeperService.createNodeWithData(path, token.toString)
    } match {
      case Success(_) =>
      case Failure(e: Throwable) =>
        logger.warn(s"Path node could not to create into zooKeeper, exception was thrown: $e, token will be revoked")
        vaultService.revokeToken(token)
        throw e
    }
  }
}

object CloudStackVaultController {
  case class Settings(vmSecretPath: String, accountSecretPath: String, zooKeeperRootNode: String)
}
