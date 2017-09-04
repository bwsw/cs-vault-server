package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Type
import com.bwsw.cloudstack.vault.server.controllers.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.scalatest.FlatSpec

class CloudStackVaultControllerTestSuite extends FlatSpec with BaseTestSuite  with TestData {
  var checkedIsExistNodePaths = List.empty[String]
  var checkedDeletionNodePaths = List.empty[String]
  var checkedCreationNodePaths = List.empty[String]
  var checkedRevokedTokens = List.empty[UUID]
  var checkedResourseIds = List.empty[UUID]
  var checkedUserIds = List.empty[UUID]

  "handleAccountDelete" should "get token from Zookeeper node, revoke it and delete secret by path in token information" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val tokenReadPath = getAccountTokenReadNodePath(accountId.toString)
    val tokenWritePath = getAccountTokenWriteNodePath(accountId.toString)
    val testSecretPathFromToken = getTestPathToAccountSecretFromToken(accountId)

    //exists data
    val tokensForCheck = List(readToken, writeToken)
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath, tokenReadPath, tokenWritePath)
    val pathForCheckDeletionNode = List(tokenReadPath, tokenWritePath, entityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedDeletionNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val services = getServicesForTestDeletionEvent(tokenReadPath, tokenWritePath, testSecretPathFromToken)

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

  "handleAccountDelete" should "delete secret by default path" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val defaultPath = getDefaultAccountSecretPath(accountId)

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val zooKeeperService = new MockZooKeeperService {
      override def isExistNode(path: String): Boolean = {
        path match {
          case x if path == entityPath =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            false
          case _ =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            true
        }
      }
    }

    val vaultService = new MockVaultService {
      override def deleteSecret(pathToSecret: String): Unit = {
        assert(pathToSecret == defaultPath, "pathToSecret is wrong")
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      vaultService,
      new MockCloudStackService,
      zooKeeperService
    )

    cloudStackVaultController.handleAccountDelete(accountId)

    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
  }

  "handleVmDelete" should "get token from Zookeeper node, revoke it and delete secret by path in token information" in {
    val entityPath = getVmEntityNodePath(vmId.toString)
    val tokenReadPath = getVmTokenReadNodePath(vmId.toString)
    val tokenWritePath = getVmTokenWriteNodePath(vmId.toString)
    val testSecretPathFromToken = getTestPathToVmSecretFromToken(vmId)

    //exists data
    val tokensForCheck = List(readToken, writeToken)
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath, tokenReadPath, tokenWritePath)
    val pathForCheckDeletionNode = List(tokenReadPath, tokenWritePath, entityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedDeletionNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val services = getServicesForTestDeletionEvent(tokenReadPath, tokenWritePath, testSecretPathFromToken)

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

  "handleVmDelete" should "delete secret by default path" in {
    val entityPath = getVmEntityNodePath(vmId.toString)
    val defaultPath = getDefaultVmSecretPath(vmId)

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val zooKeeperService = new MockZooKeeperService {
      override def isExistNode(path: String): Boolean = {
        path match {
          case x if path == entityPath =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            false
          case _ =>
            checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
            true
        }
      }
    }

    val vaultService = new MockVaultService {
      override def deleteSecret(pathToSecret: String): Unit = {
        assert(pathToSecret == defaultPath, "pathToSecret is wrong")
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      vaultService,
      new MockCloudStackService,
      zooKeeperService
    )

    cloudStackVaultController.handleVmDelete(vmId)

    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths, "checkedIsExistNodePaths is wrong")
  }

  //*************************************
  //*                                   *
  //*           User Creation           *
  //*                                   *
  //*************************************
  "handleUserCreate" should "creates new tokens (read, write) and put it into zooKeeper nodes and cloudStack user tags" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserId = firstUserId
    val expectedResourseType = Type.User
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)
    val expectedWritePolicy: Policy = Policy.createAccountWritePolicy(accountId)
    val expectedTagsWithTokens = List(
      Tag.createTag(Tag.Key.VaultRO, readToken.toString),
      Tag.createTag(Tag.Key.VaultRW, writeToken.toString)
    )

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath)
    val pathsForCheckCreationNode = List(entityPath, readTokenNodePath, writeTokenNodePath)

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

      override def setResourseTag(resourseId: UUID, resourseType: Type, tagList: List[Tag]): Unit = {
        assert(resourseId == expectedUserId, "resourse id is wrong")
        assert(resourseType == Type.User, "resourse type is wrong")
        assert(tagList == expectedTagsWithTokens, "tokenList is wrong")
      }
    }

    val vaultService = new MockVaultService {
      override def createToken(policies: List[Policy])(): UUID = {
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
      override def isExistNode(path: String): Boolean = path match {
        case x if x == entityPath =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          false
        case _ =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          true
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

    val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService, zooKeeperService)

    cloudStackVaultController.handleUserCreate(firstUserId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
  }

  "handleUserCreate" should "gets tokens (read, write) from zooKeeper nodes and write it into cloudStack user tags" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserId = firstUserId
    val expectedResourseType = Type.User
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)
    val expectedWritePolicy: Policy = Policy.createAccountWritePolicy(accountId)
    val expectedTagsWithTokens = List(
      Tag.createTag(Tag.Key.VaultRO, readToken.toString),
      Tag.createTag(Tag.Key.VaultRW, writeToken.toString)
    )

    //exists data
    val pathsForCheckIsExistNode = List(
      rootNodePath,
      rootNodeAccountPath,
      rootNodeVmPath,
      entityPath,
      readTokenNodePath,
      writeTokenNodePath
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

      override def setResourseTag(resourseId: UUID, resourseType: Type, tagList: List[Tag]): Unit = {
        assert(resourseId == expectedUserId, "resourse id is wrong")
        assert(resourseType == Type.User, "resourse type is wrong")
        assert(tagList == expectedTagsWithTokens, "tokenList is wrong")
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def isExistNode(path: String): Boolean = path match {
        case _ =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          true
      }

      override def getData(path: String): String = {
        path match {
          case x if x == readTokenNodePath => readToken.toString
          case x if x == writeTokenNodePath => writeToken.toString
          case _ =>
            assert(false, s"Unknown path: $path")
            ""
        }
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService)

    cloudStackVaultController.handleUserCreate(firstUserId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
  }

  "handleUserCreate" should "gets tokens (read, write) from cloudStack user tags" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserId = firstUserId
    val expectedResourseType = Type.User
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)
    val expectedWritePolicy: Policy = Policy.createAccountWritePolicy(accountId)
    val expectedTagsWithTokens = List(
      Tag.createTag(Tag.Key.VaultRW, writeToken.toString),
      Tag.createTag(Tag.Key.VaultRO, readToken.toString)
    )

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath)

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

      override def setResourseTag(resourseId: UUID, resourseType: Type, tagList: List[Tag]): Unit = {
        assert(resourseId == expectedUserId, "resourse id is wrong")
        assert(resourseType == Type.User, "resourse type is wrong")
        assert(tagList == expectedTagsWithTokens, "tokenList is wrong")
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def isExistNode(path: String): Boolean = {
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

  "handleUserCreate" should "writing to zookeeper node throw ZooKeeperCriticalException" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserId = firstUserId
    val expectedToken = readToken
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath, readTokenNodePath)
    val pathsForCheckCreationNode = List(readTokenNodePath)
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

    val vaultService = new MockVaultService {
      override def createToken(policies: List[Policy])(): UUID = {
        if (policies == List(expectedReadPolicy)){
          readToken
        } else {
          UUID.randomUUID()
        }
      }

      override def revokeToken(tokenId: UUID)(): String = {
        assert(tokenId == expectedToken)
        checkedRevokedTokens = checkedRevokedTokens ::: tokenId :: Nil
        ""
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def isExistNode(path: String): Boolean = path match {
        case x if x == readTokenNodePath  || x == writeTokenNodePath =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          false
        case _ =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          true
      }

      override def createNodeWithData(path: String, data: String): Unit = path match {
        case x if x == readTokenNodePath =>
          assert(data == readToken.toString, "read token is wrong")
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
          throw new ZooKeeperCriticalException(new Exception("creation node exception"))
        case _ =>
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService, zooKeeperService)

    assertThrows[ZooKeeperCriticalException] {
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
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserIds = List(firstUserId, secondUserId)
    val expectedResourseType = Type.User
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)
    val expectedWritePolicy: Policy = Policy.createAccountWritePolicy(accountId)
    val expectedTagsWithTokens = List(
      Tag.createTag(Tag.Key.VaultRO, readToken.toString),
      Tag.createTag(Tag.Key.VaultRW, writeToken.toString)
    )

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath)
    val pathsForCheckCreationNode = List(entityPath, readTokenNodePath, writeTokenNodePath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedResourseIds = List.empty[UUID]
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

      override def setResourseTag(resourseId: UUID, resourseType: Type, tagList: List[Tag]): Unit = {
        checkedResourseIds = checkedResourseIds ::: resourseId :: Nil
        assert(resourseType == Type.User, "resourse type is wrong")
        assert(tagList == expectedTagsWithTokens, "tokenList is wrong")
      }
    }

    val vaultService = new MockVaultService {
      override def createToken(policies: List[Policy])(): UUID = {
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
      override def isExistNode(path: String): Boolean = path match {
        case x if x == entityPath =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          false
        case _ =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          true
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

    val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService, zooKeeperService)

    cloudStackVaultController.handleAccountCreate(accountId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(checkedResourseIds == expectedUserIds)
    assert(checkedUserIds == expectedUserIds)
  }

  "handleAccountCreate" should "gets tokens (read, write) from zooKeeper nodes and write it into cloudStack user tags" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserIds = List(firstUserId, secondUserId)
    val expectedResourseType = Type.User
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)
    val expectedWritePolicy: Policy = Policy.createAccountWritePolicy(accountId)
    val expectedTagsWithTokens = List(
      Tag.createTag(Tag.Key.VaultRO, readToken.toString),
      Tag.createTag(Tag.Key.VaultRW, writeToken.toString)
    )

    //exists data
    val pathsForCheckIsExistNode = List(
      rootNodePath,
      rootNodeAccountPath,
      rootNodeVmPath,
      entityPath,
      readTokenNodePath,
      writeTokenNodePath
    )

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedResourseIds = List.empty[UUID]
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

      override def setResourseTag(resourseId: UUID, resourseType: Type, tagList: List[Tag]): Unit = {
        checkedResourseIds = checkedResourseIds ::: resourseId :: Nil
        assert(resourseType == Type.User, "resourse type is wrong")
        assert(tagList == expectedTagsWithTokens, "tokenList is wrong")
      }
    }

    val zooKeeperService = new MockZooKeeperService {
      override def isExistNode(path: String): Boolean = path match {
        case _ =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          true
      }

      override def getData(path: String): String = {
        path match {
          case x if x == readTokenNodePath => readToken.toString
          case x if x == writeTokenNodePath => writeToken.toString
          case _ =>
            assert(false, s"Unknown path: $path")
            ""
        }
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new MockVaultService,
      cloudStackService,
      zooKeeperService)

    cloudStackVaultController.handleAccountCreate(accountId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(expectedUserIds == checkedResourseIds)
    assert(expectedUserIds == checkedUserIds)
  }
/*
  "handleUserCreate" should "gets tokens (read, write) from cloudStack user tags" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserId = firstUserId
    val expectedResourseType = Type.User
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)
    val expectedWritePolicy: Policy = Policy.createAccountWritePolicy(accountId)
    val expectedTagsWithTokens = List(
      Tag.createTag(Tag.Key.VaultRW, writeToken.toString),
      Tag.createTag(Tag.Key.VaultRO, readToken.toString)
    )

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath)

    //actual data
    checkedIsExistNodePaths = List.empty[String]

    val cloudStackService = new CloudStackService(
      new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings),
      settings.cloudStackServiceSettings
    ){
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

      override def setResourseTag(resourseId: UUID, resourseType: Type, tagList: List[Tag]): Unit = {
        assert(resourseId == expectedUserId, "resourse id is wrong")
        assert(resourseType == Type.User, "resourse type is wrong")
        assert(tagList == expectedTagsWithTokens, "tokenList is wrong")
      }
    }

    val zooKeeperService = new ZooKeeperService(
      new ZooKeeperTaskCreator(settings.zooKeeperTaskCreatorSettings),
      settings.zooKeeperServiceSettings
    ) {
      override def isExistNode(path: String): Boolean = {
        checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
        true
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(
      new VaultService(
        new VaultRestRequestCreator(settings.vaultRestRequestCreatorSettings),
        settings.vaultServiceSettings
      ),
      cloudStackService,
      zooKeeperService
    )

    cloudStackVaultController.handleUserCreate(firstUserId)
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
  }

  "handleUserCreate" should "writing to zookeeper node throw ZooKeeperCriticalException" in {
    val entityPath = getAccountEntityNodePath(accountId.toString)
    val readTokenNodePath = getAccountTokenReadNodePath(accountId.toString)
    val writeTokenNodePath = getAccountTokenWriteNodePath(accountId.toString)

    val expectedAccountId = accountId
    val expectedUserId = firstUserId
    val expectedToken = readToken
    val expectedReadPolicy: Policy = Policy.createAccountReadPolicy(accountId)

    //exists data
    val pathsForCheckIsExistNode = List(rootNodePath, rootNodeAccountPath, rootNodeVmPath, entityPath, readTokenNodePath)
    val pathsForCheckCreationNode = List(readTokenNodePath)
    val revokedTokenList = List(readToken)

    //actual data
    checkedIsExistNodePaths = List.empty[String]
    checkedCreationNodePaths = List.empty[String]
    checkedRevokedTokens = List.empty[UUID]

    val cloudStackService = new CloudStackService(
      new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings),
      settings.cloudStackServiceSettings
    ){
      override def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
        assert(accountId == expectedAccountId, "account id is wrong")
        List(Tag.createTag(Tag.Key.Other, "value1"), Tag.createTag(Tag.Key.Other, "value2"))
      }

      override def getAccountIdByUserId(userId: UUID): UUID = {
        assert(userId == expectedUserId, "user id is wrong")
        accountId
      }
    }

    val vaultService = new VaultService(
      new VaultRestRequestCreator(settings.vaultRestRequestCreatorSettings),
      settings.vaultServiceSettings
    ){
      override def createToken(policies: List[Policy])(): UUID = {
        if (policies == List(expectedReadPolicy)){
          readToken
        } else {
          UUID.randomUUID()
        }
      }

      override def revokeToken(tokenId: UUID)(): String = {
        assert(tokenId == expectedToken)
        checkedRevokedTokens = checkedRevokedTokens ::: tokenId :: Nil
        ""
      }
    }

    val zooKeeperService = new ZooKeeperService(
      new ZooKeeperTaskCreator(settings.zooKeeperTaskCreatorSettings),
      settings.zooKeeperServiceSettings
    ){
      override def isExistNode(path: String): Boolean = path match {
        case x if x == readTokenNodePath  || x == writeTokenNodePath =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          false
        case _ =>
          checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
          true
      }

      override def createNodeWithData(path: String, data: String): Unit = path match {
        case x if x == readTokenNodePath =>
          assert(data == readToken.toString, "read token is wrong")
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
          throw new ZooKeeperCriticalException(new Exception("creation node exception"))
        case _ =>
          checkedCreationNodePaths = checkedCreationNodePaths ::: path :: Nil
      }
    }

    val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService, zooKeeperService)

    assertThrows[ZooKeeperCriticalException] {
      cloudStackVaultController.handleUserCreate(firstUserId)
    }
    assert(pathsForCheckIsExistNode == checkedIsExistNodePaths)
    assert(pathsForCheckCreationNode == checkedCreationNodePaths)
    assert(revokedTokenList == checkedRevokedTokens)
  }
  */

  private def getServicesForTestDeletionEvent(tokenReadPath: String,
                                              tokenWritePath: String,
                                              testSecretPathFromToken: String)
  :(VaultService, ZooKeeperService) = {
    val zooKeeperService = new MockZooKeeperService {
      override def isExistNode(path: String): Boolean = {
        checkedIsExistNodePaths = checkedIsExistNodePaths ::: path :: Nil
        true
      }

      override def deleteNode(path: String): Unit = {
        checkedDeletionNodePaths = checkedDeletionNodePaths ::: path :: Nil
      }

      override def getData(path: String): String = {
        path match {
          case x if x == tokenReadPath => readToken.toString
          case x if x == tokenWritePath => writeToken.toString
          case _ =>
            assert(false, "Unknown path")
            ""
        }
      }
    }

    val vaultService = new MockVaultService {
      override def revokeToken(tokenId: UUID)(): String = {
        checkedRevokedTokens = checkedRevokedTokens ::: tokenId :: Nil
        testSecretPathFromToken
      }

      override def deleteSecret(pathToSecret: String): Unit = {
        assert(pathToSecret == testSecretPathFromToken, "pathToSecret is wrong")
      }
    }

    (vaultService, zooKeeperService)
  }
}
