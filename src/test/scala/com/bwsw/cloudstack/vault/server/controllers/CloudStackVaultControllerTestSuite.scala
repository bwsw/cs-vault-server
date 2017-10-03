package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Type
import com.bwsw.cloudstack.vault.server.common.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import com.bwsw.cloudstack.vault.server.util.{DataPath, URL}
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import org.scalatest.{FlatSpec, PrivateMethodTester}

class CloudStackVaultControllerTestSuite extends FlatSpec with BaseTestSuite with TestData with PrivateMethodTester {
  var checkedIsExistNodePaths = List.empty[String]
  var checkedDeletionNodePaths = List.empty[String]
  var checkedCreationNodePaths = List.empty[String]
  var checkedRevokedTokens = List.empty[UUID]
  var checkedResourceIds = List.empty[UUID]
  var checkedUserIds = List.empty[UUID]

  //user, account expected data
  val accountEntityPath = getAccountEntityNodePath(accountId.toString)
  val readAccountTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
  val writeAccountTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)
  val expectedAccountId = accountId
  val expectedUserId = firstUserId
  val expectedUserResourceType = Type.User
  val expectedAccountReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)
  val expectedAccountWritePolicy: Policy = Policy.createAccountWritePolicy(accountId)

  //vm expected data
  val vmEntityPath = getVmEntityNodePath(vmId.toString)
  val readVmTokenNodePath = getVmTokenReadNodePath(vmId.toString)
  val writeVmTokenNodePath = getVmTokenWriteNodePath(vmId.toString)
  val expectedVmId = vmId
  val expectedVmResourceType = Type.UserVM
  val expectedVmReadPolicy: Policy = Policy.createVmReadPolicy(accountId, vmId)
  val expectedVmWritePolicy: Policy = Policy.createVmWritePolicy(accountId, vmId)

  val expectedTagsWithTokens = List(
    Tag.createTag(Tag.Key.VaultRO, readToken.toString),
    Tag.createTag(Tag.Key.VaultRW, writeToken.toString),
    Tag.createTag(Tag.Key.VaultPrefix, DataPath.accountSecretDefaultPath),
    Tag.createTag(Tag.Key.VaultHost, URL.vaultUrl)
  )

  "handleAccountDelete" should "get token from Zookeeper node, revoke it and then delete secret and policy" in {
    val testSecretPath = getDefaultAccountSecretPath(accountId)

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
          zooKeeperService
        )
    }

    cloudStackVaultController.handleAccountDelete(accountId)

    assert(tokensForCheck == checkedRevokedTokens, "revokedTokens is wrong")
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
    assert(pathForCheckDeletionNode == checkedDeletionNodePaths, "checkedDeletionNodePaths is wrong")
  }


  "handleAccountDelete" should "delete secret by default path if node with token does not exist" in {
    val defaultPath = getDefaultAccountSecretPath(accountId)

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
          zooKeeperService
        )
    }

    cloudStackVaultController.handleAccountDelete(accountId)

    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
  }

  "handleVmDelete" should "get token from Zookeeper node, revoke it and delete secret by path in token information" in {
    val testSecretPath = getDefaultVmSecretPath(vmId)

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
          zooKeeperService
        )
    }

    cloudStackVaultController.handleVmDelete(vmId)

    assert(tokensForCheck == checkedRevokedTokens, "revokedTokens is wrong")
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
    assert(pathForCheckDeletionNode == checkedDeletionNodePaths, "checkedDeletionNodePaths is wrong")
  }

  "handleVmDelete" should "delete secret by default path if node with token does not exist" in {
    val defaultPath = getDefaultVmSecretPath(vmId)

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
          zooKeeperService
        )
    }

    cloudStackVaultController.handleVmDelete(vmId)

    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
  }

  //*************************************
  //*                                   *
  //*           User Creation           *
  //*                                   *
  //*************************************
  "handleUserCreate" should "creates new tokens (read, write) and put it into zooKeeper nodes and cloudStack user tags" in {
    //exists data
    val pathsForCheckIsExistNode = List(readAccountTokenNodePath, writeAccountTokenNodePath)
    val pathsForCheckCreationNode = List(readAccountTokenNodePath, writeAccountTokenNodePath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]

    val cloudStackService = new MockCloudStackService {
      override def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        List(Tag.createTag(Tag.Key.Other, "value1"), Tag.createTag(Tag.Key.Other, "value2"))
      }

      override def getAccountIdByUserId(userId: UUID): UUID = {
        assert(userId == expectedUserId, "user id is wrong")
        accountId
      }

      override def setResourceTag(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = {
        assert(resourceId == expectedUserId, "resource id is wrong")
        assert(resourceType == expectedUserResourceType, "resource type is wrong")
        assert(tagList.toSet == expectedTagsWithTokens.toSet, "tokenList is wrong")
      }
    }

    val services = getServicesForTestEntityCreationIfTokensDoNotExist(
      expectedAccountReadPolicy,
      expectedAccountWritePolicy,
      readAccountTokenNodePath,
      writeAccountTokenNodePath
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService
        )
    }

    cloudStackVaultController.handleUserCreate(firstUserId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
  }

  "handleUserCreate" should "gets tokens (read, write) from zooKeeper nodes and write it into cloudStack user tags" in {
    //exists data
    val pathsForCheckIsExistNode = List(
      readAccountTokenNodePath,
      writeAccountTokenNodePath
    )

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val cloudStackService = new MockCloudStackService {
      override def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        List(Tag.createTag(Tag.Key.Other, "value1"), Tag.createTag(Tag.Key.Other, "value2"))
      }

      override def getAccountIdByUserId(userId: UUID): UUID = {
        assert(userId == expectedUserId, "user id is wrong")
        accountId
      }

      override def setResourceTag(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = {
        assert(resourceId == expectedUserId, "resource id is wrong")
        assert(resourceType == Type.User, "resource type is wrong")
        assert(tagList.toSet == expectedTagsWithTokens.toSet, "tokenList is wrong")
      }
    }

    val zooKeeperService = getZooKeeperServiceForTestGetTokenFromZooKeeperNode(readAccountTokenNodePath, writeAccountTokenNodePath)

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService)

    cloudStackVaultController.handleUserCreate(firstUserId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
  }

  "handleUserCreate" should "gets tokens (read, write) from cloudStack user tags" in {
    //exists data
    val pathsForCheckIsExistNode = List()

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val cloudStackService = new MockCloudStackService {
      override def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        List(
          Tag.createTag(Tag.Key.Other, "value1"),
          Tag.createTag(Tag.Key.Other, "value2"),
          Tag.createTag(Tag.Key.VaultRW, writeToken.toString),
          Tag.createTag(Tag.Key.VaultRO, readToken.toString),
          Tag.createTag(Tag.Key.VaultRW, writeToken.toString)
        )
      }

      override def getAccountIdByUserId(userId: UUID): UUID = {
        assert(userId == expectedUserId, "user id is wrong")
        accountId
      }

      override def setResourceTag(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = {
        assert(resourceId == expectedUserId, "resource id is wrong")
        assert(resourceType == Type.User, "resource type is wrong")
        assert(tagList.toSet == expectedTagsWithTokens.toSet, "tokenList is wrong")
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def doesNodeExist(path: String): Boolean = {
        checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
        true
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService
    )

    cloudStackVaultController.handleUserCreate(firstUserId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
  }

  "handleUserCreate" should "writing to zookeeper node throw exception" in {
    val expectedToken = readToken
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)

    //exists data
    val pathsForCheckIsExistNode = List(readAccountTokenNodePath)
    val pathsForCheckCreationNode = List(readAccountTokenNodePath)
    val revokedTokenList = List(readToken)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val cloudStackService = new MockCloudStackService {
      override def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        List(Tag.createTag(Tag.Key.Other, "value1"), Tag.createTag(Tag.Key.Other, "value2"))
      }

      override def getAccountIdByUserId(userId: UUID): UUID = {
        assert(userId == expectedUserId, "user id is wrong")
        accountId
      }
    }

    val services = getServicesForTestZooKeeperWritingToNodeThrowException(
      readAccountTokenNodePath,
      writeAccountTokenNodePath,
      expectedReadPolicy,
      expectedToken
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService
        )
    }

    assertThrows[Exception] {
      cloudStackVaultController.handleUserCreate(firstUserId)
    }
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(revokedTokenList == checkedRevokedTokens)
  }


  //*************************************
  //*                                   *
  //*         Account Creation          *
  //*                                   *
  //*************************************
  "handleAccountCreate" should "creates new tokens (read, write) and put it into zooKeeper nodes and cloudStack user tags" in {
    val expectedUserIds = List(firstUserId, secondUserId)

    //exists data
    val pathsForCheckIsExistNode = List(readAccountTokenNodePath, writeAccountTokenNodePath)
    val pathsForCheckCreationNode = List(readAccountTokenNodePath, writeAccountTokenNodePath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedResourceIds = List.empty[UUID]
    checkedUserIds = List.empty[UUID]

    val cloudStackService = new MockCloudStackService {
      override def getUserIdsByAccountId(accountId: UUID): List[UUID] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        expectedUserIds
      }

      override def getUserTagsByUserId(userId: UUID): List[Tag] = {
        checkedUserIds = checkedUserIds ::: userId :: Nil
        List(
          Tag.createTag(Tag.Key.Other, "value1"),
          Tag.createTag(Tag.Key.Other, "value2")
        )
      }

      override def setResourceTag(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = {
        checkedResourceIds = checkedResourceIds ::: resourceId :: Nil
        assert(resourceType == expectedUserResourceType, "resource type is wrong")
        assert(tagList.toSet == expectedTagsWithTokens.toSet, "tokenList is wrong")
      }
    }

    val services = getServicesForTestEntityCreationIfTokensDoNotExist(
      expectedAccountReadPolicy,
      expectedAccountWritePolicy,
      readAccountTokenNodePath,
      writeAccountTokenNodePath
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService
        )
    }

    cloudStackVaultController.handleAccountCreate(accountId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(checkedResourceIds == expectedUserIds)
    assert(checkedUserIds == expectedUserIds)
  }

  "handleAccountCreate" should "gets tokens (read, write) from zooKeeper nodes and write it into cloudStack user tags" in {
    val expectedUserIds = List(firstUserId, secondUserId)

    //exists data
    val pathsForCheckIsExistNode = List(
      readAccountTokenNodePath,
      writeAccountTokenNodePath
    )

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedResourceIds = List.empty[UUID]
    checkedUserIds = List.empty[UUID]

    val cloudStackService = new MockCloudStackService {
      override def getUserIdsByAccountId(accountId: UUID): List[UUID] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        expectedUserIds
      }

      override def getUserTagsByUserId(userId: UUID): List[Tag] = {
        checkedUserIds = checkedUserIds ::: userId :: Nil
        List(
          Tag.createTag(Tag.Key.Other, "value1"),
          Tag.createTag(Tag.Key.Other, "value2")
        )
      }

      override def setResourceTag(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = {
        checkedResourceIds = checkedResourceIds ::: resourceId :: Nil
        assert(resourceType == expectedUserResourceType, "resource type is wrong")
        assert(tagList.toSet == expectedTagsWithTokens.toSet, "tokenList is wrong")
      }
    }

    val zooKeeperService = getZooKeeperServiceForTestGetTokenFromZooKeeperNode(readAccountTokenNodePath, writeAccountTokenNodePath)

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService)

    cloudStackVaultController.handleAccountCreate(accountId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(expectedUserIds == checkedResourceIds)
    assert(expectedUserIds == checkedUserIds)
  }

  "handleAccountCreate" should "gets tokens (read, write) from cloudStack user tags" in {
    val expectedUserIds = List(firstUserId, secondUserId)
    val allTagsWithTokens = List(
      Tag.createTag(Tag.Key.VaultRW, writeToken.toString),
      Tag.createTag(Tag.Key.Other, "test"),
      Tag.createTag(Tag.Key.VaultRO, readToken.toString)
    )

    //exists data
    val pathsForCheckIsExistNode = List()

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedUserIds = List.empty[UUID]

    val cloudStackService = new MockCloudStackService {
      override def getUserIdsByAccountId(accountId: UUID): List[UUID] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        expectedUserIds
      }

      override def getUserTagsByUserId(userId: UUID): List[Tag] = {
        checkedUserIds = checkedUserIds ::: userId :: Nil
        allTagsWithTokens
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def doesNodeExist(path: String): Boolean = {
        checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
        true
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService
    )

    cloudStackVaultController.handleAccountCreate(accountId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(expectedUserIds == checkedUserIds)
  }

  "handleAccountCreate" should "writing to zookeeper node throw exception" in {
    val expectedUserIds = List(firstUserId, secondUserId)
    val expectedToken = readToken
    val expectedPolicy: Policy = Policy.createAccountReadPolicy(accountId)

    //exists data
    val pathsForCheckIsExistNode = List(readAccountTokenNodePath)
    val pathsForCheckCreationNode = List(readAccountTokenNodePath)
    val revokedTokenList = List(readToken)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val cloudStackService = new MockCloudStackService {
      override def getUserIdsByAccountId(accountId: UUID): List[UUID] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        expectedUserIds
      }

      override def getUserTagsByUserId(userId: UUID): List[Tag] = {
        checkedUserIds = checkedUserIds ::: userId :: Nil
        List(
          Tag.createTag(Tag.Key.Other, "value1"),
          Tag.createTag(Tag.Key.Other, "value2")
        )
      }
    }

    val services = getServicesForTestZooKeeperWritingToNodeThrowException(
      readAccountTokenNodePath,
      writeAccountTokenNodePath,
      expectedPolicy,
      expectedToken
    )

    val cloudStackVaultController = services match {
      case (vaultService, zooKeeperService) =>
        new CloudStackVaultController(
          vaultService,
          cloudStackService,
          zooKeeperService
        )
    }

    assertThrows[Exception] {
      cloudStackVaultController.handleAccountCreate(accountId)
    }
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(revokedTokenList == checkedRevokedTokens)
  }

  //*************************************
  //*                                   *
  //*            VM Creation            *
  //*                                   *
  //*************************************
  "handleVmCreate" should "creates new tokens (read, write) and put it into zooKeeper nodes and cloudStack vm tags" in {
    //exists data
    val pathsForCheckIsExistNode = List(readVmTokenNodePath, writeVmTokenNodePath)
    val pathsForCheckCreationNode = List(readVmTokenNodePath, writeVmTokenNodePath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]

    val cloudStackService = new MockCloudStackService {
      override def getAccountIdByVmId(vmId: UUID): UUID = {
        assert(vmId == expectedVmId, "vmId is wrong")
        expectedAccountId
      }

      override def setResourceTag(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = {
        assert(resourceId == expectedVmId, "resourceId is wrong")
        assert(resourceType == expectedVmResourceType, "resource type is wrong")
        assert(tagList.toSet == expectedTagsWithTokens.toSet, "tokenList is wrong")
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
          zooKeeperService
        )
    }

    cloudStackVaultController.handleVmCreate(vmId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
  }

  "handleVmCreate" should "gets tokens (read, write) from zooKeeper nodes and write it into cloudStack user tags" in {
    //exists data
    val pathsForCheckIsExistNode = List(
      readVmTokenNodePath,
      writeVmTokenNodePath
    )

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val cloudStackService = new MockCloudStackService {
      override def getAccountIdByVmId(vmId: UUID): UUID = {
        assert(vmId == expectedVmId, "vmId is wrong")
        expectedAccountId
      }

      override def setResourceTag(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = {
        assert(resourceId == expectedVmId, "resourceId is wrong")
        assert(resourceType == expectedVmResourceType, "resource type is wrong")
        assert(tagList.toSet == expectedTagsWithTokens.toSet, "tokenList is wrong")
      }
    }

    val zooKeeperService = getZooKeeperServiceForTestGetTokenFromZooKeeperNode(readVmTokenNodePath, writeVmTokenNodePath)

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService)

    cloudStackVaultController.handleVmCreate(vmId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
  }

  "handleVmCreate" should "writing to zookeeper node throw exception" in {
    val expectedToken = readToken
    val expectedPolicy: Policy = Policy.createVmReadPolicy(accountId, vmId)

    //exists data
    val pathsForCheckIsExistNode = List(readVmTokenNodePath)
    val pathsForCheckCreationNode = List(readVmTokenNodePath)
    val revokedTokenList = List(readToken)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val cloudStackService = new MockCloudStackService {
      override def getAccountIdByVmId(vmId: UUID): UUID = {
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
          zooKeeperService
        )
    }

    assertThrows[Exception] {
      cloudStackVaultController.handleVmCreate(vmId)
    }
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(revokedTokenList == checkedRevokedTokens)
  }

  "createMissingAccountTokenTags" should "throw IllegalArgumentException if tag key is not one of (VaultRO, VaultRW)" in {
    val otherTagKey = Tag.Key.Other
    val createMissingAccountTokenTags = PrivateMethod[List[Tag]]('createMissingAccountTokenTags)

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService {
        override def getNodeData(path: String): Option[String] = {
          None
        }
      }
    )
    assertThrows[IllegalArgumentException](
      cloudStackVaultController invokePrivate createMissingAccountTokenTags(accountId, List(otherTagKey))
    )
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
          throw new Exception("creation node exception")
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
      override def deleteSecret(pathToSecret: String): Unit = {
        assert(pathToSecret == defaultPath, "pathToSecret is wrong")
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

      override def deleteSecret(pathToSecret: String): Unit = {
        assert(pathToSecret == defaultPath, "pathToSecret is wrong")
      }

      override def deletePolicy(policyName: String): Unit = {
        assert(policyName == tokenPolicyName, "policy name is wrong")
      }
    }

    (vaultService, zooKeeperService)
  }
}
