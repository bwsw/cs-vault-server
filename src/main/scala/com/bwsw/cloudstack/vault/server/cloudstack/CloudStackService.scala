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
  private val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator
  private val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
  private val threadLocalJsonSerializer: ThreadLocal[JsonSerializer] = new ThreadLocal
  threadLocalJsonSerializer.get.setIgnoreUnknown(true)

  def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByAccountId(accountId: $accountId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val accountResponse = getEntityJson(accountId.toString, "id", "listAccounts")
    val allUsersIdInAccount = jsonSerializer.deserialize[AccountResponse](accountResponse)
      .accountList
      .accounts
      .flatMap { x =>
        x.users.map(_.id)
      }

    val tags = allUsersIdInAccount.flatMap { userId =>
      val tagResponse = getTagsJson("User", userId)
      jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags
    }

    logger.debug(s"Tags were got for account: $accountId)")
    tags
  }

  def getUserTagsByUserId(userId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByUserId(userId: $userId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val users = jsonSerializer.deserialize[UserResponse](
      getEntityJson(userId.toString, "id", "listUsers")
    ).userList.users

    val allUsersIdInAccount: List[UUID] = users.flatMap { x =>
      val accountResponse = getEntityJson(x.accountid.toString, "id", "listAccounts")
      jsonSerializer.deserialize[AccountResponse](accountResponse).accountList.accounts.flatMap { x =>
        x.users.map(_.id)
      }
    }

    val tags = allUsersIdInAccount.flatMap { userId =>
      val tagResponse = getTagsJson("User", userId)
      jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags
    }

    logger.debug(s"Tags were got for user: $userId)")
    tags
  }

  def getVmTagsById(vmId: UUID): List[Tag] = {
    logger.debug(s"getVmTagsById(vmId: $vmId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val vmIds = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(vmId.toString, "id", "listVirtualMachines")
    ).virtualMashineList.virtualMashines.map(_.id)

    val tags = vmIds.flatMap { vmId =>
      val tagResponse = getTagsJson("UserVM", vmId)
      jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags
    }

    logger.debug(s"Tags were got for vm: $vmId)")
    tags
  }

  def setResourseTag(resourseId: UUID, resourseType: String, tag: Tag): Unit = {
    logger.debug(s"setTagToResourse(resourseId: $resourseId, resourseType: $resourseType)")
    def task = apacheCloudStackTaskCreator.createSetResourseTagTask(resourseId, resourseType, tag)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
    logger.debug(s"Tag was set to resourse: $resourseId, $resourseType")
  }

  def getAccountIdByVmId(vmId: UUID): UUID = {
    logger.debug(s"getAccountIdForVm(vmId: $vmId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val accountName = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(vmId.toString, "id", "listVirtualMachines")
    ).virtualMashineList.virtualMashines.map(_.accountName)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountName for VM with id: $vmId"))

    val accountId: UUID = jsonSerializer.deserialize[AccountResponse](
      getEntityJson(accountName, "name", "listAccounts")
    ).accountList.accounts.map(_.id)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find account by name: $accountName"))

    logger.debug(s"accountId was got for vm: $vmId)")
    accountId
  }

  def getAccountIdByUserId(userId: UUID): UUID = {
    logger.debug(s"getAccountIdForUser(userId: $userId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val accountId = jsonSerializer.deserialize[UserResponse](
      getEntityJson(userId.toString, "id", "listUsers")
    ).userList.users.map(_.accountid)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountId for user: $userId"))

    logger.debug(s"accountId was got for user: $userId)")
    accountId
  }

  private def getEntityJson(parameterValue: String, parameterName: String, command: String): String = {
    def task = apacheCloudStackTaskCreator.createGetEntityTask(parameterValue, parameterName, command)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

  private def getTagsJson(resourceType: String, resourceId: UUID): String = {
    def task = apacheCloudStackTaskCreator.createGetTagTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

}
