package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig._
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Type
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 25.08.17.
  */
class CloudStackServiceTestSuite extends FlatSpec with TestData with BaseTestSuite {

  //Positive tests
  "getUserTagsByAccountId" should "return user tags by AccountId" in {
    val key = Tag.Key.VaultRO
    val value = "value1"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        () => Response.getAccountResponseJson(accountId.toString, userId.toString)
      }

      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.User, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        () => Response.getTagResponseJson(key, value)
      }
    }
    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val tags = cloudStackService.getUserTagsByAccount(accountId)
    assert(Set(Tag(key,value)) == tags)
  }

  "getUserTagsByUserId" should "return user tags by UserId" in {
    val key = Tag.Key.VaultRW
    val value = "value1"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.User, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        () => Response.getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val tags = cloudStackService.getUserTags(userId)
    assert(Set(Tag(key,value)) == tags)
  }

  "getVmTagsById" should "return virtual machines tags by id" in {
    val key = Tag.Key.VaultRW
    val value = "value3"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        () => Response.getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val tags = cloudStackService.getVmTags(vmId)
    assert(Set(Tag(key,value)) == tags)
  }

  "getAccountIdByVmId" should "return account id by virtual machine id" in {
    val accountName = "admin"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            assert(parameterValue == vmId.toString, "parameterValue is wrong")
            assert(parameterName == idParameter, "parameterName is wrong")
            () => Response.getVmResponseJson(vmId.toString, accountName)
          case Command.ListAccounts =>
            assert(parameterValue == accountName, "parameterValue is wrong")
            assert(parameterName == nameParameter, "parameterName is wrong")
            () => Response.getAccountResponseJson(accountId.toString, userId.toString)
          case _ =>
            assert(false, "command is wrong")
            () => ""
        }
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val expectedAccountId = cloudStackService.getVmOwnerAccount(vmId)
    assert(expectedAccountId == accountId)
  }

  "getAccountIdByUserId" should "return account id by user id" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == userId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListUsers, "command is wrong")
        () => Response.getUserResponseJson(userId.toString, accountId.toString)
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val actualAccountId = cloudStackService.getAccountByUser(userId)
    assert(actualAccountId == accountId)
  }

  "getUsersByAccount" should "return user ids by account id" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        () => Response.getAccountResponseJson(accountId.toString, userId.toString)
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val actualUserIds = cloudStackService.getUsersByAccount(accountId)
    assert(actualUserIds == userId :: Nil)
  }

  "setResourceTag" should "create CloudStack request for creating new tag in vm" in {
    val key = Tag.Key.VaultRW
    val value = "value1"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override def createSetResourceTagsTask(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): () => Unit = {
        assert(resourceId == vmId, "resourceId is wrong")
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(tagSet == Set(Tag.createTag(key, value)), "set of tags is wrong")
        () => Unit
      }
    }
    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    assert(cloudStackService.setResourceTags(vmId, Tag.Type.UserVM, Set(Tag.createTag(key, value))).isInstanceOf[Unit])
  }

  //Negative tests
  "getUserTagsByAccountId" should "The CloudStackFatalException thrown by cloudStackTaskCreator must not be swallowed" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        throw new CloudStackFatalException("test exception")
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getUserTagsByAccount(accountId)
    }
  }

  "getUserTagsByUserId" should "The CloudStackFatalException thrown by cloudStackTaskCreator must not be swallowed" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.User, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        throw new CloudStackFatalException("test exception")
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getUserTags(userId)
    }
  }

  "getVmTagsById" should "The CloudStackFatalException thrown by cloudStackTaskCreator must not be swallowed" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        throw new CloudStackFatalException("test exception")
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getVmTags(vmId)
    }
  }

  "getAccountIdByVmId" should "The CloudStackFatalException thrown by cloudStackTaskCreator must not be swallowed" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            assert(parameterValue == vmId.toString, "parameterValue is wrong")
            assert(parameterName == idParameter, "parameterName is wrong")
            throw new CloudStackFatalException("test exception")
        }
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getAccountIdByUserId" should "The CloudStackFatalException thrown by cloudStackTaskCreator must not be swallowed" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == userId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListUsers, "command is wrong")
        throw new CloudStackFatalException("test exception")
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getAccountByUser(userId)
    }
  }

  "getUsersByAccount" should "The CloudStackFatalException thrown by cloudStackTaskCreator must not be swallowed" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        throw new CloudStackFatalException("test exception")
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getUsersByAccount(accountId)
    }
  }

  "getAccountIdByVmId" should "The CloudStackEntityDoesNotExistException must be thrown if vm with specified id does not exist" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == vmId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListVirtualMachines, "command is wrong")
        () => Response.getResponseWithEmptyVmList
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)
    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getAccountIdByVmId" should "The CloudStackEntityDoesNotExistException must be thrown if account with specified name does not exist" in {
    val accountName = "accountName"

    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            () => Response.getVmResponseJson(vmId.toString, accountName)
          case Command.ListAccounts =>
            assert(parameterValue == accountName, "parameterValue is wrong")
            assert(parameterName == nameParameter, "parameterName is wrong")
            () => Response.getResponseWithEmptyAccountList
        }
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)
    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getAccountIdByUserId" should "The CloudStackEntityDoesNotExistException must be thrown if user with specified id does not exist" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == userId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListUsers, "command is wrong")
        () => Response.getResponseWithEmptyUserList
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)
    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getAccountByUser(userId)
    }
  }

  "getUsersByAccount" should "The CloudStackEntityDoesNotExistException must be thrown if account with specified id does not exist" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        () => Response.getResponseWithEmptyAccountList
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)
    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getUsersByAccount(accountId)
    }
  }

}
