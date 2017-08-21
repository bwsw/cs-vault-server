package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, TaskRunner}
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val apacheCloudStackTaskCreator= new ApacheCloudStackTaskCreator
  private val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)

  def getUserTagByAccountId(accountId: UUID): List[Tag] = {
    logger.debug(s"getUserTagByAccountId(accountId: $accountId)")
    val jsonSerializer = new JsonSerializer(true)

    val listAccountResponse = getEntityJsonById(accountId, "id", "listAccounts")
    val allUsersIdInAccount = jsonSerializer.deserialize[ListAccountResponse](listAccountResponse)
      .accountResponse
      .accounts
      .flatMap { x =>
        x.users.map(_.id)
      }

    val r = allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getJsonTags("User", userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for account: $accountId)")
    r
  }

  def getUserTagByUserId(userId: UUID): List[Tag] = {
    logger.debug(s"getUserTagByUserId(userId: $userId)")
    val jsonSerializer = new JsonSerializer(true)

    val userList = jsonSerializer.deserialize[ListUserResponse](
      getEntityJsonById(userId, "id", "listUsers")
    ).userResponse.users

    val allUsersIdInAccount: List[UUID] = userList.flatMap { x =>
      val listAccountResponse = getEntityJsonById(x.accountid, "id", "listAccounts")
      jsonSerializer.deserialize[ListAccountResponse](listAccountResponse).accountResponse.accounts.flatMap { x =>
        x.users.map(_.id)
      }
    }

    val r = allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getJsonTags("User", userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for user: $userId)")
    r
  }

  def getVmTagById(vmId: UUID): List[Tag] = {
    logger.debug(s"getVmTagById(vmId: $vmId)")
    val jsonSerializer = new JsonSerializer(true)

    val vmIdList = jsonSerializer.deserialize[ListVirtualMachinesResponse](
      getEntityJsonById(vmId, "id", "listVirtualMachines")
    ).vmResponse.virtualMashines.map(_.id)

    val r = vmIdList.flatMap { vmId =>
      val listTagResponse = getJsonTags("UserVM", vmId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for vm: $vmId)")
    r
  }

  def setTagToResourse(tag: Tag, resourseId: UUID, resourseType: String): Unit = {
    logger.debug(s"setTagToResourse(resourseId: $resourseId, resourseType: $resourseType)")
    def task = apacheCloudStackTaskCreator.createSetTagToResourseTask(tag, resourseId, resourseType)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
    logger.debug(s"Tag was set to resourse: $resourseId, $resourseType")
  }

  private def getEntityJsonById(id: UUID, idEntity: String, command: String): String = {
    def task = apacheCloudStackTaskCreator.createGetEntityTask(id, idEntity, command)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

  private def getJsonTags(resourceType: String, resourceId: UUID): String = {
    def task = apacheCloudStackTaskCreator.createGetTagTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

}
