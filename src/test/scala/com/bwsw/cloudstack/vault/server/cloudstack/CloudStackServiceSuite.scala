package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 25.08.17.
  */
class CloudStackServiceSuite extends FlatSpec with TestData {

  "getUserTagsByAccountId" should "return user tags by AccountId" in {
    val key = "key1"
    val value = "value1"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator {
      override def createGetEntityTask(parameterValue: String, parameterName: String, command: String): () => String = {
        assert(parameterValue == accountId.toString, "parameterValue is wrong")
        assert(parameterName == idParameter, "parameterName is wrong")
        assert(command == listAccountsCommand, "command is wrong")
        () => getAccountResponseJson(accountId.toString, userId.toString)
      }

      override def createGetTagTask(resourceType: String, resourceId: UUID): () => String = {
        assert(resourceType == userResourseType, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        () => getTagResponseJson(key, value)
      }
    }
    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator)

    val tags = cloudStackService.getUserTagsByAccountId(accountId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getUserTagsByUserId" should "return user tags by UserId" in {
    val key = "key2"
    val value = "value2"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator {
      override def createGetTagTask(resourceType: String, resourceId: UUID): () => String = {
        assert(resourceType == userResourseType, "resourceType is wrong")
        assert(resourceId == userId, "resourceId is wrong")
        () => getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator)

    val tags = cloudStackService.getUserTagsByUserId(userId)
    assert(Tag(key,value) :: Nil == tags)
  }

  "getVmTagsById" should "return virtual machines tags by id" in {
    val key = "key3"
    val value = "value3"

    val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator {
      override def createGetTagTask(resourceType: String, resourceId: UUID): () => String = {
        assert(resourceType == vmUserResourseType, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        () => getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(apacheCloudStackTaskCreator)

    val tags = cloudStackService.getVmTagsById(vmId)
    assert(Tag(key,value) :: Nil == tags)
  }

}
