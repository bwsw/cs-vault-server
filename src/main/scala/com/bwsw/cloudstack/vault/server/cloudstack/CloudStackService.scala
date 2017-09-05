package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackCriticalException
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util.TaskRunner
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackService(apacheCloudStackTaskCreator: ApacheCloudStackTaskCreator,
                        settings: CloudStackService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsonSerializer = new JsonSerializer(true)

  /**
    * Gets all tags of account's users which has "User" type.
    * Will be restarted if cloudstack server is unavailable.
    *
    * @param accountId id of account for gets user's tags
    *
    * @return List with Tag
    * @throws CloudStackCriticalException if account with specified id does not exist.
    */
  def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByAccountId(accountId: $accountId)")

    val allUsersIdInAccount = getUserIdsByAccountId(accountId)

    val tags = allUsersIdInAccount.flatMap { userId =>
      getUserTagsByUserId(userId)
    }

    logger.debug(s"Tags were got for account: $accountId)")
    tags
  }

  /**
    * Gets all tags of users which has "User" type.
    * Will be restarted if cloudstack server is unavailable.
    *
    * @param userId id of user for gets user's tags
    *
    * @return List with Tag
    * @throws CloudStackCriticalException if user with specified id does not exist.
    */
  def getUserTagsByUserId(userId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByUserId(userId: $userId)")

    val tagResponse = getTagsJson(Tag.Type.User, userId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags.getOrElse(List.empty[Tag])

    logger.debug(s"Tags were got for user: $userId)")
    tags
  }

  /**
    * Gets all tags of virtual machine which has "UserVM" type.
    * Will be restarted if cloudstack server is unavailable.
    *
    * @param vmId id of virtual mashine for gets user's tags
    *
    * @return List with Tag
    * @throws CloudStackCriticalException if virtual machine with specified id does not exist.
    */
  def getVmTagsById(vmId: UUID): List[Tag] = {
    logger.debug(s"getVmTagsById(vmId: $vmId)")

    val tagResponse = getTagsJson(Tag.Type.UserVM, vmId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags.getOrElse(List.empty[Tag])

    logger.debug(s"Tags were got for vm: $vmId)")

    tags
  }

  /**
    * Gets account id for the virtual machine.
    * Will be restarted if cloudstack server is unavailable.
    *
    * @param vmId id of virtual machine for gets account name
    *
    * @return UUID of account which name indicate in virtual machine
    * @throws CloudStackCriticalException if virtual machine with specified id does not exist,
    *                                     or if account with specified name in virtual machine does not exist.
    */
  def getAccountIdByVmId(vmId: UUID): UUID = {
    logger.debug(s"getAccountIdByVmId(vmId: $vmId)")

    val accountName = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(vmId.toString, apacheCloudStackTaskCreator.idParameter, Command.ListVirtualMachines)
    ).virtualMashineList.virtualMashines.getOrElse(
      throw new CloudStackCriticalException(new NoSuchElementException(s"Virtual machine with id: $vmId does not exist"))
    ).map(_.accountName).head

    val accountId: UUID = jsonSerializer.deserialize[AccountResponse](
      getEntityJson(accountName, apacheCloudStackTaskCreator.nameParameter, Command.ListAccounts)
    ).accountList.accounts.getOrElse(
      throw new CloudStackCriticalException(new NoSuchElementException(s"The vm: $vmId does not include account with name: $accountName"))
    ).map(_.id).head

    logger.debug(s"accountId was got for vm: $vmId)")
    accountId
  }

  /**
    * Gets account id for the user.
    * Will be restarted if cloudstack server is unavailable.
    *
    * @param userId id of user for gets account id
    *
    * @return UUID of account which include user with indicate id
    * @throws CloudStackCriticalException if user with specified id does not exist.
    */
  def getAccountIdByUserId(userId: UUID): UUID = {
    logger.debug(s"getAccountIdByUserId(userId: $userId)")

    val accountId = jsonSerializer.deserialize[UserResponse](
      getEntityJson(userId.toString, apacheCloudStackTaskCreator.idParameter, Command.ListUsers)
    ).userList.users.getOrElse(
      throw new CloudStackCriticalException(new NoSuchElementException(s"User with id: $userId does not exist"))
    ).map(_.accountid).head

    logger.debug(s"accountId was got for user: $userId)")
    accountId
  }

  /**
    * Gets user ids for the account.
    * Will be restarted if cloudstack server is unavailable.
    *
    * @param accountId id of user for gets account id
    *
    * @return List with UUID of users which are included in account
    * @throws CloudStackCriticalException if account with specified id does not exist.
    */
  def getUserIdsByAccountId(accountId: UUID): List[UUID] = {
    logger.debug(s"getUserIdsForAccount(accountId: $accountId)")
    val jsonSerializer = new JsonSerializer(true)

    val accountResponse = getEntityJson(
      accountId.toString,
      apacheCloudStackTaskCreator.idParameter,
      Command.ListAccounts
    )

    val allUsersIdInAccount = jsonSerializer.deserialize[AccountResponse](accountResponse)
      .accountList
      .accounts.getOrElse(
        throw new CloudStackCriticalException(new NoSuchElementException(s"Account with id: $accountId does not exist"))
      ).flatMap { x =>
        x.users.map(_.id)
      }

    logger.debug(s"Users were got for account: $accountId)")
    allUsersIdInAccount
  }

  /**
    * Sets tag to specified entity.
    * Will be restarted if cloudstack server is unavailable.
    *
    * @param resourceId id of entity for set tag
    * @param resourceType "User" or "UserVM" type of tags
    * @param tagList List with tags to add to the resource
    */
  def setResourceTag(resourceId: UUID, resourceType: Tag.Type, tagList: List[Tag]): Unit = {
    logger.debug(s"setResourceTag(resourceId: $resourceId, resourceType: $resourceType)")
    def task = apacheCloudStackTaskCreator.createSetResourceTagTask(resourceId, resourceType, tagList)

    TaskRunner.tryRunUntilSuccess[String](task, settings.cloudStackRetryDelay)
    logger.debug(s"Tag was set to resource: $resourceId, $resourceType")
  }

  private def getEntityJson(parameterValue: String, parameterName: String, command: Command): String = {
    def task = apacheCloudStackTaskCreator.createGetEntityTask(parameterValue, parameterName, command)

    TaskRunner.tryRunUntilSuccess[String](task, settings.cloudStackRetryDelay)
  }

  private def getTagsJson(resourceType: Tag.Type, resourceId: UUID): String = {
    def task = apacheCloudStackTaskCreator.createGetTagTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, settings.cloudStackRetryDelay)
  }
}

object CloudStackService {
  case class Settings(cloudStackRetryDelay: Int)
}
