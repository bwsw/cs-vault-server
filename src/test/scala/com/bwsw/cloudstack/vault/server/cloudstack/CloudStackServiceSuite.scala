package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.BaseSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 25.08.17.
  */
class CloudStackServiceSuite extends FlatSpec with TestData with BaseSuite {

  "getUserTagsByAccountId" should "return user tags by AccountId" in {
    val key = Tag.Key.VaultRO
    val value = "value1"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        () => getAccountResponseJson(accountId.toString, userId.toString)
      }

      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.User, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        () => getTagResponseJson(key, value)
      }
    }
    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, cloudStackSettings)

    val tags = cloudStackService.getUserTagsByAccountId(accountId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getUserTagsByUserId" should "return user tags by UserId" in {
    val key = Tag.Key.VaultRW
    val value = "value1"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.User, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        () => getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, cloudStackSettings)

    val tags = cloudStackService.getUserTagsByUserId(userId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getVmTagsById" should "return virtual machines tags by id" in {
    val key = Tag.Key.VaultRW
    val value = "value3"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        () => getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, cloudStackSettings)

    val tags = cloudStackService.getVmTagsById(vmId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getAccountIdByVmId" should "return account id by virtual machine id" in {
    val accountName = "admin"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            assert(parameterValue == vmId.toString, "parameterValue is wrong")
            assert(parameterName == idParameter, "parameterName is wrong")
            () => getVmResponseJson(vmId.toString, accountName)
          case Command.ListAccounts =>
            assert(parameterValue == accountName, "parameterValue is wrong")
            assert(parameterName == nameParameter, "parameterName is wrong")
            () => getAccountResponseJson(accountId.toString, userId.toString)
          case _ =>
            assert(false, "command is wrong")
            () => ""
        }
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, cloudStackSettings)

    val expectedAccountId: UUID = cloudStackService.getAccountIdByVmId(vmId)
    assert(expectedAccountId == accountId)
  }

  "getAccountIdByUserId" should "return account id by user id" in {
    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == userId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListUsers, "command is wrong")
        () => getUserResponseJson(userId.toString, accountId.toString)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, cloudStackSettings)

    val expectedAccountId: UUID = cloudStackService.getAccountIdByUserId(userId)
    assert(expectedAccountId == accountId)
  }

  "getUserIdsByAccountId" should "return user ids by account id" in {
    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == Command.ListAccounts, "command is wrong")
        () => getAccountResponseJson(accountId.toString, userId.toString)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator, cloudStackSettings)

    val expectedUserIds: List[UUID] = cloudStackService.getUserIdsByAccountId(accountId)
    assert(expectedUserIds == userId :: Nil)
  }

}
