package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskWrapper
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, TaskRunner}
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val apacheCloudStackTaskWrapper= new ApacheCloudStackTaskWrapper
  private val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)

  def getUserTagByAccountId(accountId: UUID): List[Tag] = {
    logger.debug(s"getUserTagByAccountId(accountId: $accountId)")
    val jsonSerializer = new JsonSerializer(true)

    val listAccountResponse = getEntityJson(accountId.toString, "id", "listAccounts")
    val allUsersIdInAccount = jsonSerializer.deserialize[ListAccountResponse](listAccountResponse)
      .accountResponse
      .accounts
      .flatMap { x =>
        x.users.map(_.id)
      }

    val r = allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getTagsJson("User", userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for account: $accountId)")
    r
  }

  def getUserTagByUserId(userId: UUID): List[Tag] = {
    logger.debug(s"getUserTagByUserId(userId: $userId)")
    val jsonSerializer = new JsonSerializer(true)

    val userList = jsonSerializer.deserialize[ListUserResponse](
      getEntityJson(userId.toString, "id", "listUsers")
    ).userResponse.users

    val allUsersIdInAccount: List[UUID] = userList.flatMap { x =>
      val listAccountResponse = getEntityJson(x.accountid.toString, "id", "listAccounts")
      jsonSerializer.deserialize[ListAccountResponse](listAccountResponse).accountResponse.accounts.flatMap { x =>
        x.users.map(_.id)
      }
    }

    val r = allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getTagsJson("User", userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for user: $userId)")
    r
  }

  def getVmTagById(vmId: UUID): List[Tag] = {
    logger.debug(s"getVmTagById(vmId: $vmId)")
    val jsonSerializer = new JsonSerializer(true)

    val vmIdList = jsonSerializer.deserialize[ListVirtualMachinesResponse](
      getEntityJson(vmId.toString, "id", "listVirtualMachines")
    ).vmResponse.virtualMashines.map(_.id)

    val r = vmIdList.flatMap { vmId =>
      val listTagResponse = getTagsJson("UserVM", vmId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for vm: $vmId)")
    r
  }

  def setTagToResourse(tag: Tag, resourseId: UUID, resourseType: String): Unit = {
    logger.debug(s"setTagToResourse(resourseId: $resourseId, resourseType: $resourseType)")
    def task = apacheCloudStackTaskWrapper.setTagToResourseTask(tag, resourseId, resourseType)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
    logger.debug(s"Tag was set to resourse: $resourseId, $resourseType")
  }

  def getAccountIdForVm(vmId: UUID): UUID = {
    logger.debug(s"getAccountIdForVm(vmId: $vmId)")
    val jsonSerializer = new JsonSerializer(true)

    val accountName = jsonSerializer.deserialize[ListVirtualMachinesResponse](
      getEntityJson(vmId.toString, "id", "listVirtualMachines")
    ).vmResponse.virtualMashines.map(_.accountName)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountName for VM with id: $vmId"))

    val accountId: UUID = jsonSerializer.deserialize[ListAccountResponse](
      getEntityJson(accountName, "name", "listAccounts")
    ).accountResponse.accounts.map(_.id)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find account by name: $accountName"))

    logger.debug(s"accountId was got for vm: $vmId)")
    accountId
  }

  def getAccountIdForUser(userId: UUID): UUID = {
    logger.debug(s"getAccountIdForUser(userId: $userId)")
    val jsonSerializer = new JsonSerializer(true)

    val accountId = jsonSerializer.deserialize[ListUserResponse](
      getEntityJson(userId.toString, "id", "listUsers")
    ).userResponse.users.map(_.accountid)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountId for user: $userId"))

    logger.debug(s"accountId was got for user: $userId)")
    accountId
  }

  private def getEntityJson(parameterValue: String, parameterName: String, command: String): String = {
    def task = apacheCloudStackTaskWrapper.getEntityTask(parameterValue, parameterName, command)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

  private def getTagsJson(resourceType: String, resourceId: UUID): String = {
    def task = apacheCloudStackTaskWrapper.getTagTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

}
