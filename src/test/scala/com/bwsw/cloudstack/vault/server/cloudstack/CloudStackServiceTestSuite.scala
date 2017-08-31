package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.BaseSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackCriticalException
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 25.08.17.
  */
class CloudStackServiceTestSuite extends FlatSpec with TestData with BaseSuite {

  //Positive tests
  "getUserTagsByAccountId" should "return user tags by AccountId" in {
    val key = Tag.Key.VaultRO
    val value = "value1"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings) {
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
    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    val tags = cloudStackService.getUserTagsByAccountId(accountId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getUserTagsByUserId" should "return user tags by UserId" in {
    val key = Tag.Key.VaultRW
    val value = "value1"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.User, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        () => Response.getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    val tags = cloudStackService.getUserTagsByUserId(userId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getVmTagsById" should "return virtual machines tags by id" in {
    val key = Tag.Key.VaultRW
    val value = "value3"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        () => Response.getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    val tags = cloudStackService.getVmTagsById(vmId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getAccountIdByVmId" should "return account id by virtual machine id" in {
    val accountName = "admin"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
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

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    val expectedAccountId: UUID = cloudStackService.getAccountIdByVmId(vmId)
    assert(expectedAccountId == accountId)
  }

  "getAccountIdByUserId" should "return account id by user id" in {
    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == userId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListUsers, "command is wrong")
        () => Response.getUserResponseJson(userId.toString, accountId.toString)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    val expectedAccountId: UUID = cloudStackService.getAccountIdByUserId(userId)
    assert(expectedAccountId == accountId)
  }

  "getUserIdsByAccountId" should "return user ids by account id" in {
    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        () => Response.getAccountResponseJson(accountId.toString, userId.toString)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    val expectedUserIds: List[UUID] = cloudStackService.getUserIdsByAccountId(accountId)
    assert(expectedUserIds == userId :: Nil)
  }

  //Negative tests
  "getUserTagsByAccountId" should "throw CloudStackCriticalException" in {
    val key = Tag.Key.VaultRO
    val value = "value1"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings) {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        throw new CloudStackCriticalException(new Exception("test exception"))
      }
    }
    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    assertThrows[CloudStackCriticalException] {
      cloudStackService.getUserTagsByAccountId(accountId)
    }
  }

  "getUserTagsByUserId" should "throw CloudStackCriticalException" in {
    val key = Tag.Key.VaultRW
    val value = "value1"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.User, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        throw new CloudStackCriticalException(new Exception("test exception"))
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    assertThrows[CloudStackCriticalException] {
      cloudStackService.getUserTagsByUserId(userId)
    }
  }

  "getVmTagsById" should "throw CloudStackCriticalException" in {
    val key = Tag.Key.VaultRW
    val value = "value3"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        throw new CloudStackCriticalException(new Exception("test exception"))
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    assertThrows[CloudStackCriticalException] {
      cloudStackService.getVmTagsById(vmId)
    }
  }

  "getAccountIdByVmId" should "throw CloudStackCriticalException" in {
    val accountName = "admin"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            assert(parameterValue == vmId.toString, "parameterValue is wrong")
            assert(parameterName == idParameter, "parameterName is wrong")
            throw new CloudStackCriticalException(new Exception("test exception"))
        }
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    assertThrows[CloudStackCriticalException] {
      cloudStackService.getAccountIdByVmId(vmId)
    }
  }

  "getAccountIdByUserId" should "throw CloudStackCriticalException" in {
    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == userId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListUsers, "command is wrong")
        throw new CloudStackCriticalException(new Exception("test exception"))
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    assertThrows[CloudStackCriticalException] {
      cloudStackService.getAccountIdByUserId(userId)
    }
  }

  "getUserIdsByAccountId" should "throw CloudStackCriticalException" in {
    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        throw new CloudStackCriticalException(new Exception("test exception"))
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, settings.cloudStackServiceSettings)

    assertThrows[CloudStackCriticalException] {
      cloudStackService.getUserIdsByAccountId(accountId)
    }
  }

}
