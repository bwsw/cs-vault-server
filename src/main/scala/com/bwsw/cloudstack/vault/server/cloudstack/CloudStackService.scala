/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackEntityDoesNotExistException
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util.TaskRunner
import org.slf4j.LoggerFactory

/**
  * Class is responsible for interaction with CloudStack server with help of CloudStackTaskCreator
  *
  * @param cloudStackTaskCreator allows for creating task for interaction with CloudStack
  * @param settings contains the settings for interaction with CloudStack
  */
class CloudStackService(cloudStackTaskCreator: CloudStackTaskCreator,
                        settings: CloudStackService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsonSerializer = new JsonSerializer(ignoreUnknownProperties = true)

  /**
    * Gets all tags of account's users which has "User" type.
    *
    * @param accountId id of account for gets user's tags
    *
    * @return Set with Tag
    * @throws CloudStackEntityDoesNotExistException if account with specified id does not exist.
    */
  def getUserTagsByAccount(accountId: UUID): Set[Tag] = {
    logger.debug(s"getUserTagsByAccount(accountId: $accountId)")

    val allUsersIdInAccount = getUsersByAccount(accountId)

    val tags = allUsersIdInAccount.flatMap { userId =>
      getUserTags(userId)
    }.toSet

    logger.debug(s"Tags were got for account: $accountId)")
    tags
  }

  /**
    * Gets all tags of users which has "User" type.
    *
    * @param userId id of user for gets user's tags
    *
    * @return Set with Tag
    * @throws CloudStackEntityDoesNotExistException if user with specified id does not exist.
    */
  def getUserTags(userId: UUID): Set[Tag] = {
    logger.debug(s"getUserTags(userId: $userId)")

    val tagResponse = getTagsJson(Tag.Type.User, userId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagSet.tags.getOrElse(Set.empty[Tag])

    logger.debug(s"Tags were got for user: $userId)")
    tags
  }

  /**
    * Gets all tags of virtual machine which has "UserVM" type.
    *
    * @param vmId id of virtual mashine for gets user's tags
    *
    * @return Set with Tag
    * @throws CloudStackEntityDoesNotExistException if virtual machine with specified id does not exist.
    */
  def getVmTags(vmId: UUID): Set[Tag] = {
    logger.debug(s"getVmTags(vmId: $vmId)")

    val tagResponse = getTagsJson(Tag.Type.UserVM, vmId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagSet.tags.getOrElse(Set.empty[Tag])

    logger.debug(s"Tags were got for vm: $vmId)")

    tags
  }

  /**
    * Gets account id for the virtual machine.
    *
    * @param vmId id of virtual machine for gets account name
    *
    * @return UUID of account which name indicate in virtual machine
    * @throws CloudStackEntityDoesNotExistException if virtual machine with specified id does not exist,
    *                                               or if account with specified name in virtual machine does not exist.
    */
  def getVmOwnerAccount(vmId: UUID): UUID = {
    logger.debug(s"getVmOwnerAccount(vmId: $vmId)")

    val vm = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(Command.ListVirtualMachines, Map(cloudStackTaskCreator.idParameter -> vmId.toString))
    ).virtualMashineList.virtualMashines.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"Virtual machine with id: $vmId does not exist")
    ).head

    val accountId = jsonSerializer.deserialize[AccountResponse](
      getEntityJson(Command.ListAccounts, Map(
                cloudStackTaskCreator.nameParameter -> vm.accountName,
                cloudStackTaskCreator.domainParameter -> vm.domainId.toString
              ))
    ).accountList.accounts.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"The vm: $vmId does not include account with " +
        s"name: ${vm.accountName} within domain: ${vm.domainId}")
    ).map(_.id).head

    logger.debug(s"accountId was got for vm: $vmId)")
    accountId
  }

  /**
    * Gets account id for the user.
    *
    * @param userId id of user for gets account id
    *
    * @return UUID of account which include user with indicate id
    * @throws CloudStackEntityDoesNotExistException if user with specified id does not exist.
    */
  def getAccountByUser(userId: UUID): UUID = {
    logger.debug(s"getAccountByUser(userId: $userId)")

    val accountId = jsonSerializer.deserialize[UserResponse](
      getEntityJson(Command.ListUsers, Map(cloudStackTaskCreator.idParameter -> userId.toString))
    ).userList.users.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"User with id: $userId does not exist")
    ).map(_.accountid).head

    logger.debug(s"accountId was got for user: $userId)")
    accountId
  }

  /**
    * Gets user ids for the account.
    *
    * @param accountId id of user for gets account id
    *
    * @return List with UUID of users which are included in account
    * @throws CloudStackEntityDoesNotExistException if account with specified id does not exist.
    */
  def getUsersByAccount(accountId: UUID): List[UUID] = {
    logger.debug(s"getUsersByAccount(accountId: $accountId)")
    val jsonSerializer = new JsonSerializer(ignoreUnknownProperties = true)

    val accountResponse = getEntityJson(Command.ListAccounts, Map(cloudStackTaskCreator.idParameter -> accountId.toString))

    val allUsersIdInAccount = jsonSerializer.deserialize[AccountResponse](accountResponse)
      .accountList
      .accounts.getOrElse(
        throw new CloudStackEntityDoesNotExistException(s"Account with id: $accountId does not exist")
      ).flatMap { x =>
        x.users.map(_.id)
      }

    logger.debug(s"Users were got for account: $accountId)")
    allUsersIdInAccount
  }

  /**
    * Sets tag to specified entity.
    *
    * @param resourceId   id of entity for set tag
    * @param resourceType "User" or "UserVM" type of tags
    * @param tagSet Set with tags to add to the resource
    */
  def setResourceTags(resourceId: UUID, resourceType: Tag.Type, tagSet: Set[Tag]): Unit = {
    logger.debug(s"setResourceTags(resourceId: $resourceId, resourceType: $resourceType)")
    def task = cloudStackTaskCreator.createSetResourceTagsTask(resourceId, resourceType, tagSet)

    TaskRunner.tryRunUntilSuccess[Unit](task, settings.retryDelay)
    logger.debug(s"Tag was set to resource: $resourceId, $resourceType")
  }

  private def getEntityJson(command: Command, parameters: Map[String, String]) = {
    def task = cloudStackTaskCreator.createGetEntityTask(command, parameters)

    TaskRunner.tryRunUntilSuccess[String](task, settings.retryDelay)
  }

  private def getTagsJson(resourceType: Tag.Type, resourceId: UUID): String = {
    def task = cloudStackTaskCreator.createGetTagTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, settings.retryDelay)
  }
}

object CloudStackService {
  case class Settings(retryDelay: Int)
}
