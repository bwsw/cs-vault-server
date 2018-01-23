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

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Type
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackEntityDoesNotExistException
import com.bwsw.cloudstack.vault.server.mocks.MockConfig
import com.bwsw.cloudstack.vault.server.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import org.scalatest.{FlatSpec, PrivateMethodTester}

class CloudStackVaultControllerTestSuite extends FlatSpec with BaseTestSuite with TestData with PrivateMethodTester {
  val controllerSettings = settings.cloudStackVaultControllerSettings
  var checkedIsExistNodePaths = List.empty[String]
  var checkedDeletionNodePaths = List.empty[String]
  var checkedCreationNodePaths = List.empty[String]
  var checkedRevokedTokens = List.empty[UUID]

  //account expected data
  val accountEntityPath = getAccountEntityNodePath(accountId.toString)
  val readAccountTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
  val writeAccountTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)
  val expectedAccountId = accountId
  val expectedAccountResourceType = Type.Account
  val expectedAccountReadPolicy = Policy.createAccountReadPolicy(accountId, controllerSettings.accountSecretPath)
  val expectedAccountWritePolicy = Policy.createAccountWritePolicy(accountId, controllerSettings.accountSecretPath)

  //vm expected data
  val vmEntityPath = getVmEntityNodePath(vmId.toString)
  val readVmTokenNodePath = getVmTokenReadNodePath(vmId.toString)
  val writeVmTokenNodePath = getVmTokenWriteNodePath(vmId.toString)
  val expectedVmId = vmId
  val expectedVmResourceType = Type.UserVM
  val expectedVmReadPolicy = Policy.createVmReadPolicy(accountId, vmId, controllerSettings.vmSecretPath)
  val expectedVmWritePolicy = Policy.createVmWritePolicy(accountId, vmId, controllerSettings.vmSecretPath)

  val expectedVaultTagsForAccount = List(
    Tag.createTag(Tag.Key.VaultRO, readToken.toString),
    Tag.createTag(Tag.Key.VaultRW, writeToken.toString),
    Tag.createTag(Tag.Key.VaultPrefix, s"${controllerSettings.accountSecretPath}$accountId"),
    Tag.createTag(Tag.Key.VaultHost, s"${MockConfig.vaultRestRequestExecutorSettings.endpoints.map { x =>
      s"$x${RequestPath.vaultRoot}"
    }.mkString(",")}")
  )

  val expectedVaultTagsForVm = List(
    Tag.createTag(Tag.Key.VaultRO, readToken.toString),
    Tag.createTag(Tag.Key.VaultRW, writeToken.toString),
    Tag.createTag(Tag.Key.VaultPrefix, s"${controllerSettings.vmSecretPath}$vmId"),
    Tag.createTag(Tag.Key.VaultHost,  s"${MockConfig.vaultRestRequestExecutorSettings.endpoints.map { x =>
      s"$x${RequestPath.vaultRoot}"
    }.mkString(",")}")
  )

  "handleAccountDelete" should "get account tokens from ZooKeeper, revoke them and then delete secret and policies" in {
    val testSecretPath = getDefaultRequestAccountSecretPath(accountId)

    //exists data
    val tokensForCheck = List(readToken, writeToken)
    val pathsForCheckIsExistNode = List(accountEntityPath, readAccountTokenNodePath, writeAccountTokenNodePath)
    val pathForCheckDeletionNode = List(accountEntityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedDeletionNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val services = getServicesForTestDeletionEvent(
      readAccountTokenNodePath,
      writeAccountTokenNodePath,
      testSecretPath,
      expectedAccountReadPolicy.name
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          new MockCloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    cloudStackVaultController.handleAccountDelete(accountId)

    assert(tokensForCheck == checkedRevokedTokens, "revokedTokens is wrong")
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
    assert(pathForCheckDeletionNode == checkedDeletionNodePaths, "checkedDeletionNodePaths is wrong")
  }


  "handleAccountDelete" should "delete secret if account znode does not exist" in {
    val defaultPath = getDefaultRequestAccountSecretPath(accountId)

    //exists data
    val pathsForCheckIsExistNode = List(accountEntityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val services = getServicesForTestDeletionEventIfZNodeNotExist(accountEntityPath, defaultPath)

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          new MockCloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    cloudStackVaultController.handleAccountDelete(accountId)

    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
  }

  "handleVmDelete" should "get VM tokens from ZooKeeper, revoke them and then delete secret and policies" in {
    val testSecretPath = getDefaultRequestVmSecretPath(vmId)

    //exists data
    val tokensForCheck = List(readToken, writeToken)
    val pathsForCheckIsExistNode = List(vmEntityPath, readVmTokenNodePath, writeVmTokenNodePath)
    val pathForCheckDeletionNode = List(vmEntityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedDeletionNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val services = getServicesForTestDeletionEvent(
      readVmTokenNodePath,
      writeVmTokenNodePath,
      testSecretPath,
      expectedVmReadPolicy.name
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          new MockCloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    cloudStackVaultController.handleVmDelete(vmId)

    assert(tokensForCheck == checkedRevokedTokens, "revokedTokens is wrong")
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
    assert(pathForCheckDeletionNode == checkedDeletionNodePaths, "checkedDeletionNodePaths is wrong")
  }

  "handleVmDelete" should "delete secret if VM znode does not exist" in {
    val defaultPath = getDefaultRequestVmSecretPath(vmId)

    //exists data
    val pathsForCheckIsExistNode = List(vmEntityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val services = getServicesForTestDeletionEventIfZNodeNotExist(vmEntityPath, defaultPath)

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          new MockCloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    cloudStackVaultController.handleVmDelete(vmId)

    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
  }

  //*************************************
  //*                                   *
  //*         Account Creation          *
  //*                                   *
  //*************************************
  "handleAccountCreate" should "create new tokens (read, write) and put them into ZooKeeper nodes and CloudStack tags" in {
    //exists data
    val pathsForCheckIsExistNode = List(readAccountTokenNodePath, writeAccountTokenNodePath)
    val pathsForCheckCreationNode = List(readAccountTokenNodePath, writeAccountTokenNodePath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]

    val services = getServicesForTestEntityCreationIfTokensDoNotExist(
      expectedAccountReadPolicy,
      expectedAccountWritePolicy,
      readAccountTokenNodePath,
      writeAccountTokenNodePath
    )

    val cloudStackService = new MockCloudStackService {
      override def doesAccountExist(accountId: UUID): Boolean = {
        assert(accountId == expectedAccountId)
        true
      }

      override def setResourceTags(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): Unit = {
        assert(resourceId == expectedAccountId, "resourceId is wrong")
        assert(resourceType == expectedAccountResourceType, "resource type is wrong")
        assert(tagSet == expectedVaultTagsForAccount.toSet, "set of tags is wrong")
      }
    }

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    cloudStackVaultController.handleAccountCreate(accountId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
  }

  "handleAccountCreate" should "get tokens (read, write) from ZooKeeper nodes" in {
    //exists data
    val pathsForCheckIsExistNode = List(
      readAccountTokenNodePath,
      writeAccountTokenNodePath
    )

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val zooKeeperService = getZooKeeperServiceForTestGetTokenFromZooKeeperNode(readAccountTokenNodePath, writeAccountTokenNodePath)

    val cloudStackService = new MockCloudStackService {
      override def doesAccountExist(accountId: UUID): Boolean =
      {
        assert(accountId == expectedAccountId)
        true
      }

      override def setResourceTags(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): Unit = {
        assert(resourceId == expectedAccountId, "resourceId is wrong")
        assert(resourceType == expectedAccountResourceType, "resource type is wrong")
        assert(tagSet == expectedVaultTagsForAccount.toSet, "set of tags is wrong")
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService,
      controllerSettings
    )

    cloudStackVaultController.handleAccountCreate(accountId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
  }

  "handleAccountCreate" should "throw exception when try to put token into ZooKeeper node" in {
    val expectedToken = readToken
    val expectedPolicy = Policy.createAccountReadPolicy(accountId, controllerSettings.accountSecretPath)

    //exists data
    val pathsForCheckIsExistNode = List(readAccountTokenNodePath)
    val pathsForCheckCreationNode = List(readAccountTokenNodePath)
    val revokedTokenList = List(readToken)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val services = getServicesForTestZooKeeperWritingToNodeThrowException(
      readAccountTokenNodePath,
      writeAccountTokenNodePath,
      expectedPolicy,
      expectedToken
    )

    val cloudStackService = new MockCloudStackService {
      override def doesAccountExist(accountId: UUID): Boolean =
      {
        assert(accountId == expectedAccountId)
        true
      }
    }

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    assertThrows[Exception] {
      cloudStackVaultController.handleAccountCreate(accountId)
    }
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(revokedTokenList == checkedRevokedTokens)
  }

  "handleAccountCreate" should "not create tokens if the account does not exist" in {
    val cloudStackService = new MockCloudStackService {
      override def doesAccountExist(accountId: UUID): Boolean =
      {
        assert(accountId == expectedAccountId)
        false
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      new MockZooKeeperService,
      controllerSettings
    )

    assert {
      cloudStackVaultController.handleAccountCreate(accountId).isInstanceOf[Unit]
    }
  }

  //*************************************
  //*                                   *
  //*            VM Creation            *
  //*                                   *
  //*************************************
  "handleVmCreate" should "create new tokens (read, write) and put them into ZooKeeper nodes and CloudStack VM tags" in {
    //exists data
    val pathsForCheckIsExistNode = List(readVmTokenNodePath, writeVmTokenNodePath)
    val pathsForCheckCreationNode = List(readVmTokenNodePath, writeVmTokenNodePath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]

    val cloudStackService = new MockCloudStackService {
      override def getVmOwnerAccount(vmId: UUID): UUID = {
        assert(vmId == expectedVmId, "vmId is wrong")
        expectedAccountId
      }

      override def setResourceTags(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): Unit = {
        assert(resourceId == expectedVmId, "resourceId is wrong")
        assert(resourceType == expectedVmResourceType, "resource type is wrong")
        assert(tagSet == expectedVaultTagsForVm.toSet, "set of tags is wrong")
      }
    }

    val services = getServicesForTestEntityCreationIfTokensDoNotExist(
      expectedVmReadPolicy,
      expectedVmWritePolicy,
      readVmTokenNodePath,
      writeVmTokenNodePath
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    cloudStackVaultController.handleVmCreate(vmId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
  }

  "handleVmCreate" should "get tokens (read, write) from ZooKeeper nodes and put them into CloudStack VM tags" in {
    //exists data
    val pathsForCheckIsExistNode = List(
      readVmTokenNodePath,
      writeVmTokenNodePath
    )

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val cloudStackService = new MockCloudStackService {
      override def getVmOwnerAccount(vmId: UUID): UUID = {
        assert(vmId == expectedVmId, "vmId is wrong")
        expectedAccountId
      }

      override def setResourceTags(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): Unit = {
        assert(resourceId == expectedVmId, "resourceId is wrong")
        assert(resourceType == expectedVmResourceType, "resource type is wrong")
        assert(tagSet == expectedVaultTagsForVm.toSet, "set of tags is wrong")
      }
    }

    val zooKeeperService = getZooKeeperServiceForTestGetTokenFromZooKeeperNode(readVmTokenNodePath, writeVmTokenNodePath)

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService,
      controllerSettings
    )

    cloudStackVaultController.handleVmCreate(vmId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
  }

  "handleVmCreate" should "throw exception when try to put token into ZooKeeper node" in {
    val expectedToken = readToken
    val expectedPolicy: Policy = Policy.createVmReadPolicy(accountId, vmId, controllerSettings.vmSecretPath)

    //exists data
    val pathsForCheckIsExistNode = List(readVmTokenNodePath)
    val pathsForCheckCreationNode = List(readVmTokenNodePath)
    val revokedTokenList = List(readToken)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val cloudStackService = new MockCloudStackService {
      override def getVmOwnerAccount(vmId: UUID): UUID = {
        assert(vmId == expectedVmId, "vmId is wrong")
        expectedAccountId
      }
    }

    val services = getServicesForTestZooKeeperWritingToNodeThrowException(
      readVmTokenNodePath,
      writeVmTokenNodePath,
      expectedPolicy,
      expectedToken
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService,
          controllerSettings
        )
    }

    assertThrows[Exception] {
      cloudStackVaultController.handleVmCreate(vmId)
    }
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(revokedTokenList == checkedRevokedTokens)
  }

  "handleVmCreate" should "throw CloudStackEntityDoesNotExistException when try to retrieve account id by vm id" in {
    val cloudStackService = new MockCloudStackService {
      override def getVmOwnerAccount(vmId: UUID): UUID = {
        assert(vmId == expectedVmId, "vmId is wrong")
        throw new CloudStackEntityDoesNotExistException("")
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      new MockZooKeeperService,
      controllerSettings
    )

    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackVaultController.handleVmCreate(vmId)
    }
  }

  private def getZooKeeperServiceForTestGetTokenFromZooKeeperNode(readTokenNodePath: String, writeTokenNodePath: String) = {
    val zooKeeperService = new MockZooKeeperService {
      override def getNodeData(path: String): Option[String] = {
        path match {
          case x if x == readTokenNodePath =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            Some(readToken.toString)
          case x if x == writeTokenNodePath =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            Some(writeToken.toString)
          case _ =>
            assert(false, s"Unknown path: $path")
            None
        }
      }
    }
    zooKeeperService
  }

  private def getServicesForTestZooKeeperWritingToNodeThrowException(readTokenPath: String,
                                                                     writeTokenPath: String,
                                                                     exeptedPolicy: Policy,
                                                                     expectedToken: UUID) = {
    val vaultService = new MockVaultService {
      override def createToken(policies: List[Policy]): UUID = {
        if (policies == List(exeptedPolicy)){
          readToken
        } else {
          UUID.randomUUID()
        }
      }

      override def revokeToken(tokenId: UUID): List[String] = {
        assert(tokenId == expectedToken)
        checkedRevokedTokens = checkedRevokedTokens ::: tokenId :: Nil
        List(exeptedPolicy.name)
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def getNodeData(path: String): Option[String] = {
        path match {
          case x if x == readTokenPath =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            None
          case _ =>
            assert(false, "Unknown path")
            None
        }
      }

      override def createNodeWithData(path: String, data: String): Unit = path match {
        case x if x == readTokenPath =>
          assert(data == expectedToken.toString, "read token is wrong")
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
          throw new Exception("creation znode exception")
        case _ =>
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
      }
    }

    (vaultService, zooKeeperService)
  }


  private def getServicesForTestDeletionEventIfZNodeNotExist(entityPath: String, defaultPath: String) = {
    val zooKeeperService = new MockZooKeeperService {
      override def doesNodeExist(path: String): Boolean = {
        assert(path == entityPath, "path is wrong")
        checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
        false
      }
    }

    val vaultService = new MockVaultService {
      override def deleteSecretsRecursively(pathToRootSecret: String): Unit = {
        assert(pathToRootSecret == defaultPath, "pathToSecret is wrong")
      }
    }
    (vaultService, zooKeeperService)
  }

  private def getServicesForTestEntityCreationIfTokensDoNotExist(expectedReadPolicy: Policy,
                                                                   expectedWritePolicy: Policy,
                                                                   readTokenNodePath: String,
                                                                   writeTokenNodePath: String) = {
    val vaultService = new MockVaultService {
      override def createToken(policies: List[Policy]): UUID = {
        if (policies == List(expectedReadPolicy)){
          readToken
        } else if (policies == List(expectedWritePolicy)) {
          writeToken
        } else {
          UUID.randomUUID()
        }
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def getNodeData(path: String): Option[String] = {
        checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
        None
      }

      override def createNodeWithData(path: String, data: String): Unit = path match {
        case x if x == readTokenNodePath =>
          assert(data == readToken.toString, "read token is wrong")
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
        case x if x == writeTokenNodePath =>
          assert(data == writeToken.toString, "write token is wrong")
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
        case _ =>
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
      }
    }

    (vaultService, zooKeeperService)
  }

  private def getServicesForTestDeletionEvent(tokenReadPath: String,
                                              tokenWritePath: String,
                                              defaultPath: String,
                                              tokenPolicyName: String)
  :(VaultService, ZooKeeperService) = {
    val zooKeeperService = new MockZooKeeperService {
      override def doesNodeExist(path: String): Boolean = {
        checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
        true
      }

      override def deleteNode(path: String): Unit = {
        checkedDeletionNodePaths = checkedDeletionNodePaths ::: path :: Nil
      }

      override def getNodeData(path: String): Option[String] = {
        path match {
          case x if x == tokenReadPath =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            Some(readToken.toString)
          case x if x == tokenWritePath =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            Some(writeToken.toString)
          case _ =>
            assert(false, "Unknown path")
            None
        }
      }
    }

    val vaultService = new MockVaultService {
      override def revokeToken(tokenId: UUID): List[String] = {
        checkedRevokedTokens = checkedRevokedTokens ::: tokenId :: Nil
        List(tokenPolicyName)
      }

      override def deleteSecretsRecursively(pathToRootSecret: String): Unit = {
        assert(pathToRootSecret == defaultPath, "pathToSecret is wrong")
      }

      override def deletePolicy(policyName: String): Unit = {
        assert(policyName == tokenPolicyName, "policy name is wrong")
      }
    }

    (vaultService, zooKeeperService)
  }
}
