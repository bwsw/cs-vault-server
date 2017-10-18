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
  * @param cloudStackTaskCreator enables creation tasks for interaction with CloudStack
  * @param settings contains settings for interaction with CloudStack
  */
class CloudStackService(cloudStackTaskCreator: CloudStackTaskCreator,
                        settings: CloudStackService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsonSerializer = new JsonSerializer(ignoreUnknownProperties = true)

  /**
    * Retrieves all tags of account's users.
    *
    * @param accountId id of account to retrieve user tags
    *
    * @return Set of tags
    * @throws CloudStackEntityDoesNotExistException if account with specified id does not exist.
    */
  def getUserTagsByAccount(accountId: UUID): Set[Tag] = {
    logger.debug(s"getUserTagsByAccount(accountId: $accountId)")

    val allUsersIdInAccount = getUsersByAccount(accountId)

    val tags = allUsersIdInAccount.flatMap { userId =>
      getUserTags(userId)
    }.toSet

    logger.debug(s"Tags: $tags retrieved for account: $accountId)")
    tags
  }

  /**
    * Retrieves all tags of users.
    *
    * @param userId id of user to retrieve user tags
    *
    * @return Set of tags
    * @throws CloudStackEntityDoesNotExistException if user with specified id does not exist.
    */
  def getUserTags(userId: UUID): Set[Tag] = {
    logger.debug(s"getUserTags(userId: $userId)")

    val tagResponse = getTagsJson(Tag.Type.User, userId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagSet.tags.getOrElse(Set.empty[Tag])

    logger.debug(s"Tags: $tags retrieved for user: $userId)")
    tags
  }

  /**
    * Retrieves all tags of virtual machine.
    *
    * @param vmId id of virtual machine to retrieve its tags
    *
    * @return Set of tags
    * @throws CloudStackEntityDoesNotExistException if virtual machine with specified id does not exist.
    */
  def getVmTags(vmId: UUID): Set[Tag] = {
    logger.debug(s"getVmTags(vmId: $vmId)")

    val tagResponse = getTagsJson(Tag.Type.UserVM, vmId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagSet.tags.getOrElse(Set.empty[Tag])

    logger.debug(s"Tags: $tags retrieved for vm: $vmId)")

    tags
  }

  /**
    * Retrieves account id for virtual machine.
    *
    * @param vmId id of virtual machine to retrieve account name
    *
    * @return UUID of account which name is indicated in virtual machine
    * @throws CloudStackEntityDoesNotExistException if virtual machine with specified id does not exist,
    *                                               or if account with specified name in virtual machine does not exist.
    */
  def getVmOwnerAccount(vmId: UUID): UUID = {
    logger.debug(s"getVmOwnerAccount(vmId: $vmId)")

    val vm = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(Command.ListVirtualMachines, Map(cloudStackTaskCreator.idParameter -> vmId.toString))
    ).virtualMashineList.virtualMashines.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"Vm with id: $vmId does not exist")
    ).head

    val accountId = jsonSerializer.deserialize[AccountResponse](
      getEntityJson(Command.ListAccounts, Map(
                cloudStackTaskCreator.nameParameter -> vm.accountName,
                cloudStackTaskCreator.domainParameter -> vm.domainId.toString
              ))
    ).accountList.accounts.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"Vm: $vmId does not include an account with " +
        s"name: ${vm.accountName} within domain: ${vm.domainId}")
    ).map(_.id).head

    logger.debug(s"Account id: $accountId retrieved for vm: $vmId)")
    accountId
  }

  /**
    * Retrieves account id for user.
    *
    * @param userId id of user to retrieve account id
    *
    * @return UUID of account which includes a user with indicated id
    * @throws CloudStackEntityDoesNotExistException if user with specified id does not exist.
    */
  def getAccountByUser(userId: UUID): UUID = {
    logger.debug(s"getAccountByUser(userId: $userId)")

    val accountId = jsonSerializer.deserialize[UserResponse](
      getEntityJson(Command.ListUsers, Map(cloudStackTaskCreator.idParameter -> userId.toString))
    ).userList.users.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"User with id: $userId does not exist")
    ).map(_.accountid).head

    logger.debug(s"Account id: $accountId retrieved for user: $userId)")
    accountId
  }

  /**
    * Retrieves user ids for account.
    *
    * @param accountId id of account to retrieve users ids
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

    logger.debug(s"Users: $allUsersIdInAccount retrieved for account: $accountId)")
    allUsersIdInAccount
  }

  /**
    * Includes tags to specified entity.
    *
    * @param resourceId id of entity to include tag
    * @param resourceType "User" or "UserVM" type of tags
    * @param tagSet Set of tags to include them into resource
    */
  def setResourceTags(resourceId: UUID, resourceType: Tag.Type, tagSet: Set[Tag]): Unit = {
    logger.debug(s"setResourceTags(resourceId: $resourceId, resourceType: $resourceType)")
    def task = cloudStackTaskCreator.createSetResourceTagsTask(resourceId, resourceType, tagSet)

    TaskRunner.tryRunUntilSuccess[Unit](task, settings.retryDelay)
    logger.debug(s"Tags: $tagSet included into resource: $resourceId, $resourceType")
  }

  private def getEntityJson(command: Command, parameters: Map[String, String]) = {
    def task = cloudStackTaskCreator.createGetEntityTask(command, parameters)

    TaskRunner.tryRunUntilSuccess[String](task, settings.retryDelay)
  }

  private def getTagsJson(resourceType: Tag.Type, resourceId: UUID): String = {
    def task = cloudStackTaskCreator.createGetTagsTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, settings.retryDelay)
  }
}

object CloudStackService {
  case class Settings(retryDelay: Int)
}
