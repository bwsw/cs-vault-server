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
class CloudStackService(apacheCloudStackTaskCreator: ApacheCloudStackTaskCreator) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
  private val jsonSerializer = new JsonSerializer(true)

  def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByAccountId(accountId: $accountId)")

    val allUsersIdInAccount = getUserIdsByAccountId(accountId)

    val tags = allUsersIdInAccount.flatMap { userId =>
      getUserTagsByUserId(userId)
    }

    logger.debug(s"Tags were got for account: $accountId)")
    tags
  }

  def getUserTagsByUserId(userId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByUserId(userId: $userId)")

    val tagResponse = getTagsJson("User", userId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags

    logger.debug(s"Tags were got for user: $userId)")
    tags
  }

  def getVmTagsById(vmId: UUID): List[Tag] = {
    logger.debug(s"getVmTagsById(vmId: $vmId)")

    val tagResponse = getTagsJson("UserVM", vmId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags

    logger.debug(s"Tags were got for vm: $vmId)")
    tags
  }

  def getAccountIdByVmId(vmId: UUID): UUID = {
    logger.debug(s"getAccountIdByVmId(vmId: $vmId)")

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
    logger.debug(s"getAccountIdByUserId(userId: $userId)")

    val accountId = jsonSerializer.deserialize[UserResponse](
      getEntityJson(userId.toString, "id", "listUsers")
    ).userList.users.map(_.accountid)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountId for user: $userId"))

    logger.debug(s"accountId was got for user: $userId)")
    accountId
  }

  def getUserIdsByAccountId(accountId: UUID): List[UUID] = {
    logger.debug(s"getUserIdsForAccount(accountId: $accountId)")
    val jsonSerializer = new JsonSerializer(true)

    val accountResponse = getEntityJson(accountId.toString, "id", "listAccounts")
    val allUsersIdInAccount = jsonSerializer.deserialize[AccountResponse](accountResponse)
      .accountList
      .accounts
      .flatMap { x =>
        x.users.map(_.id)
      }

    logger.debug(s"Users were got for account: $accountId)")
    allUsersIdInAccount
  }

  def setResourseTag(resourseId: UUID, resourseType: String, tag: Tag): Unit = {
    logger.debug(s"setResourseTag(resourseId: $resourseId, resourseType: $resourseType)")
    def task = apacheCloudStackTaskCreator.createSetResourseTagTask(resourseId, resourseType, tag)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
    logger.debug(s"Tag was set to resourse: $resourseId, $resourseType")
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
