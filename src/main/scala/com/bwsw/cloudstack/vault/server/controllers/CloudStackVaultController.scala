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

import com.bwsw.cloudstack.entities.responses.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for execution of business logic (see docs/logic.md).
  *
  * @param vaultService provides interaction with Vault server
  * @param cloudStackService provides interaction with CloudStack server
  * @param zooKeeperService provides interaction with ZooKeeper server
  */
class CloudStackVaultController(vaultService: VaultService,
                                cloudStackService: CloudStackService,
                                zooKeeperService: ZooKeeperService,
                                settings: CloudStackVaultController.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val accountEntityName = "accounts"
  private val vmEntityName = "vms"
  private val vaultApiPathsString = vaultService.endpoints.map { endpoint =>
    s"$endpoint${RequestPath.vaultRoot}"
  }.mkString(",")

  /**
    * Revokes tokens and deletes secrets in Vault server.
    * For details see part "Account or VM deletion event processing" in docs/logic.md
    *
    * @param accountId account id in CloudStack server for which vault token is created
    */
  def handleAccountDelete(accountId: UUID): Unit = {
    logger.trace(s"handleAccountDelete(accountId: $accountId)")
    val requestSecretPath = s"${RequestPath.vaultRoot}${getAccountEntitySecretPath(accountId)}"
    deleteTokenAndAppropriateSecret(accountId, accountEntityName, requestSecretPath)
    logger.debug(s"Account deletion has been processed, accountId: $accountId)")
  }

  /**
    * Revokes tokens and deletes secrets in Vault server.
    * For details see part "Account or VM deletion event processing" in docs/logic.md
    *
    * @param vmId VM id in CloudStack server for which vault token is created
    */
  def handleVmDelete(vmId: UUID): Unit = {
    logger.trace(s"handleVmDelete(vmId: $vmId)")
    val requestSecretPath = s"${RequestPath.vaultRoot}${getVmEntitySecretPath(vmId)}"
    deleteTokenAndAppropriateSecret(vmId, vmEntityName, requestSecretPath)
    logger.debug(s"VM deletion has been processed, vmId: $vmId)")
  }

  /**
    * Processes tokens creation for account.
    * For details see part "Account creation event processing" in docs/logic.md
    *
    * @param accountId account id in CloudStack server
    */
  def handleAccountCreate(accountId: UUID): Unit = {
    logger.trace(s"handleAccountCreate(accountId: $accountId)")

    if(cloudStackService.doesAccountExist(accountId)) {
      val policyList = List(
        Policy.createAccountReadPolicy(accountId, settings.accountSecretPath),
        Policy.createAccountWritePolicy(accountId, settings.accountSecretPath)
      )

      val vaultTokenTags = policyList.map { x =>
        val pathToData = createTokenEntityNodePath(accountId.toString, accountEntityName, getTagKeyByPolicyACL(x.acl))
        zooKeeperService.getNodeData(pathToData) match {
          case Some(token) =>
            Tag(VaultTagKey.toString(getTagKeyByPolicyACL(x.acl)), token)
          case None =>
            val token = vaultService.createToken(x :: Nil)
            val tag = Tag(VaultTagKey.toString(getTagKeyByPolicyACL(x.acl)), token.toString)
            writeTokenToZooKeeperNode(pathToData, token)
            tag
        }
      }.toSet

      val vaultKeyspaceTags = Set(
        Tag(VaultTagKey.toString(VaultTagKey.VaultHosts), vaultApiPathsString),
        Tag(VaultTagKey.toString(VaultTagKey.VaultPrefix), getAccountEntitySecretPath(accountId))
      )

      cloudStackService.setAccountTags(accountId, vaultTokenTags ++ vaultKeyspaceTags)

      logger.debug(s"Account creation has been processed, accountId: $accountId)")
    } else {
      logger.warn(s"Account with id: $accountId does not exist")
    }
  }

  /**
    * Processes tokens creation for the VM.
    * For details see part "Virtual machine creation event processing" in docs/logic.md
    *
    * @param vmId VM id in CloudStack server
    */
  def handleVmCreate(vmId: UUID): Unit = {
    logger.trace(s"handleVmCreate(vmId: $vmId)")

    val accountId = cloudStackService.getVmOwnerAccount(vmId) //here is thrown CloudStackEntityDoesNotExistException if vm does not exist.
                                                              //If it method will not be used, then you should use
                                                              //doesVirtualMachineExist method for checking vm existence before
                                                              //vm token creation

    val policyList = List(
      Policy.createVmReadPolicy(accountId, vmId, settings.vmSecretPath),
      Policy.createVmWritePolicy(accountId, vmId, settings.vmSecretPath)
    )

    val vaultTokenTags = policyList.map { x =>
      val pathToData = createTokenEntityNodePath(vmId.toString, vmEntityName, getTagKeyByPolicyACL(x.acl))
      zooKeeperService.getNodeData(pathToData) match {
        case Some(token) =>
          Tag(VaultTagKey.toString(getTagKeyByPolicyACL(x.acl)), token)
        case None =>
          val token = vaultService.createToken(x :: Nil)
          val tag = Tag(VaultTagKey.toString(getTagKeyByPolicyACL(x.acl)), token.toString)
          writeTokenToZooKeeperNode(pathToData, token)
          tag
      }
    }.toSet

    val vaultKeyspaceTags = Set(
      Tag(VaultTagKey.toString(VaultTagKey.VaultHosts), vaultApiPathsString),
      Tag(VaultTagKey.toString(VaultTagKey.VaultPrefix), getVmEntitySecretPath(vmId))
    )

    cloudStackService.setVmTags(vmId, vaultTokenTags ++ vaultKeyspaceTags)
    logger.debug(s"VM creation has been processed, vmId: $vmId)")
  }

  /**
    * Associates policy acl in Vault with tag key in CloudStack
    */
  private def getTagKeyByPolicyACL(acl: Policy.ACL): VaultTagKey = {
    acl match {
      case Policy.ACL.Read => VaultTagKey.VaultRO
      case Policy.ACL.Write => VaultTagKey.VaultRW
      case _ => throw new IllegalArgumentException(s"unknown policy ACL: $acl")
    }
  }


  /**
    * Create missing token tags after creating tokens in Vault or retrieving them from ZooKeeper node
    */
  private def createMissingAccountTokenTag(accountId: UUID, absentTagKey: VaultTagKey): Tag = {
    logger.trace(s"createMissingAccountTokenTag(accountId: $accountId, absentTagKey: $absentTagKey)")

    val pathToToken = createTokenEntityNodePath(accountId.toString, accountEntityName, absentTagKey)
    zooKeeperService.getNodeData(pathToToken) match {
      case Some(token) =>
        Tag(VaultTagKey.toString(absentTagKey), token)
      case None =>
        val policy = absentTagKey match {
          case VaultTagKey.VaultRO =>
            Policy.createAccountReadPolicy(accountId, settings.accountSecretPath)
          case VaultTagKey.VaultRW =>
            Policy.createAccountWritePolicy(accountId, settings.accountSecretPath)
          case _ =>
            throw new IllegalArgumentException(s"tag key: $absentTagKey is wrong")
        }
        val token = vaultService.createToken(policy :: Nil)
        val tag = Tag(VaultTagKey.toString(absentTagKey), token.toString)
        writeTokenToZooKeeperNode(pathToToken, token)
        tag
    }
  }

  /**
    * Revokes token and deletes secret in Vault, and removes entity node from ZooKeeper
    */
  private def deleteTokenAndAppropriateSecret(entityId: UUID, entityName: String, secretPath: String): Unit = {
    logger.trace(s"deleteTokenAndAppropriateSecret(entityId: $entityId, entityName: $entityName)")
    val pathToEntityNode = createEntityNodePath(entityId.toString, entityName)

    if (zooKeeperService.doesNodeExist(pathToEntityNode)) {
      val pathsToTokenData = List(VaultTagKey.VaultRO, VaultTagKey.VaultRW).map { x =>
        createTokenEntityNodePath(entityId.toString, entityName, x)
      }
      pathsToTokenData.foreach { path =>
        zooKeeperService.getNodeData(path) match {
          case Some(token) =>
            val policyNames = vaultService.revokeToken(UUID.fromString(token))
            policyNames.foreach(vaultService.deletePolicy)
          case None =>
            logger.warn(s"Token's znode by path: $path does not exist for entity: $entityId")
        }
      }
      zooKeeperService.deleteNode(pathToEntityNode)
    } else {
      logger.warn(s"Node by path: $pathToEntityNode does not exist for entity: $entityId")
    }
    vaultService.deleteSecretsRecursively(secretPath)
  }

  private def createTokenEntityNodePath(entityId: String, entityName: String, tagKey: VaultTagKey) =
    s"${createEntityNodePath(entityId, entityName)}/${tagKey.toString.toLowerCase()}"

  private def createEntityNodePath(entityId: String, entityName: String) =
    s"${settings.zooKeeperRootNode}/$entityName/$entityId"

  private def getVmEntitySecretPath(vmId: UUID): String = {
    s"${settings.vmSecretPath}$vmId"
  }

  private def getAccountEntitySecretPath(accountId: UUID): String = {
    s"${settings.accountSecretPath}$accountId"
  }

  private def writeTokenToZooKeeperNode(path: String, token: UUID): Unit = {
    logger.trace(s"writeTokensToZooKeeperNode(path: $path)")
    Try {
      zooKeeperService.createNodeWithData(path, token.toString)
    } match {
      case Success(_) =>
      case Failure(e: Throwable) =>
        logger.warn(s"Could not create znode by path: $path, the exception: $e occurred, token is revoked")
        vaultService.revokeToken(token)
        throw e
    }
  }
}

object CloudStackVaultController {
  case class Settings(vmSecretPath: String, accountSecretPath: String, zooKeeperRootNode: String)
}
