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

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for interaction with CloudStack server with help of CloudStackTaskCreator
  *
  * @param cloudStackTaskCreator enables tasks creation for interaction with CloudStack
  * @param settings contains settings for interaction with CloudStack
  */
class CloudStackService(cloudStackTaskCreator: CloudStackTaskCreator,
                        settings: CloudStackService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsonSerializer = new JsonSerializer(ignoreUnknownProperties = true)

  /**
    * Retrieves all tags of account.
    *
    * @param accountId id of account to retrieve its tags
    *
    * @return Set of tags
    * @throws CloudStackEntityDoesNotExistException if account with specified id does not exist.
    */
  def getAccountTags(accountId: UUID): Set[Tag] = {
    logger.debug(s"getAccountTags(accountId: $accountId)")

    val tagResponse = getTagsJson(Tag.Type.Account, accountId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagSet.tags.getOrElse(Set.empty[Tag])

    logger.debug(s"Tags: $tags are retrieved for account: $accountId)")
    tags
  }

  /**
    * Retrieves all tags of VM.
    *
    * @param vmId id of VM to retrieve its tags
    *
    * @return Set of tags
    * @throws CloudStackEntityDoesNotExistException if VM with specified id does not exist.
    */
  def getVmTags(vmId: UUID): Set[Tag] = {
    logger.debug(s"getVmTags(vmId: $vmId)")

    val tagResponse = getTagsJson(Tag.Type.UserVM, vmId)
    val tags = jsonSerializer.deserialize[TagResponse](tagResponse).tagSet.tags.getOrElse(Set.empty[Tag])

    logger.debug(s"Tags: $tags are retrieved for VM: $vmId)")

    tags
  }

  /**
    * Retrieves account id for VM.
    *
    * @param vmId id of VM to retrieve account name
    *
    * @return id of account which name is indicated in VM
    * @throws CloudStackEntityDoesNotExistException if VM with specified id does not exist,
    *                                               or if account with specified name in VM does not exist.
    */
  def getVmOwnerAccount(vmId: UUID): UUID = {
    logger.debug(s"getVmOwnerAccount(vmId: $vmId)")

    val vm = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(Command.ListVirtualMachines, Map(cloudStackTaskCreator.idParameter -> vmId.toString))
    ).virtualMashineList.virtualMashines.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"VM with id: $vmId does not exist")
    ).head

    val accountId = jsonSerializer.deserialize[AccountResponse](
      getEntityJson(Command.ListAccounts, Map(
                cloudStackTaskCreator.nameParameter -> vm.accountName,
                cloudStackTaskCreator.domainParameter -> vm.domainId.toString
              ))
    ).accountList.accounts.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"VM: $vmId does not include the account with " +
        s"name: ${vm.accountName} within domain: ${vm.domainId}")
    ).map(_.id).head

    logger.debug(s"Account id: $accountId are retrieved for VM: $vmId)")
    accountId
  }

  def doesAccountExist(accountId: UUID): Boolean = {
    logger.debug(s"doesAccountExist(accountId: $accountId)")
    Try {
      jsonSerializer.deserialize[AccountResponse](getEntityJson(Command.ListAccounts, Map(
        cloudStackTaskCreator.idParameter -> accountId.toString
      ))).accountList.accounts.getOrElse(List.empty[Account]).nonEmpty
    } match {
      case Success(x) =>
        logger.debug(s"does account with id: $accountId exist: $x")
        x
      case Failure(e: CloudStackEntityDoesNotExistException) =>
        logger.debug(s"account with id: $accountId does not exist")
        false
      case Failure(e: Throwable) =>
        logger.error(s"exception: $e was thrown")
        throw e
    }
  }

  def doesVirtualMachineExist(vmId: UUID): Boolean = {
    logger.debug(s"doesVirtualMachineExist(vmId: $vmId)")
    Try {
      jsonSerializer.deserialize[VirtualMachinesResponse](getEntityJson(Command.ListVirtualMachines, Map(
        cloudStackTaskCreator.idParameter -> vmId.toString
      ))).virtualMashineList.virtualMashines.getOrElse(List.empty[VirtualMashine]).nonEmpty
    } match {
      case Success(x) =>
        logger.debug(s"does vm with id: $vmId exist: $x")
        x
      case Failure(e: CloudStackEntityDoesNotExistException) =>
        logger.debug(s"vm with id: $vmId does not exist")
        false
      case Failure(e: Throwable) =>
        logger.error(s"exception: $e was thrown")
        throw e
    }
  }
  /**
    * Includes tags to specified entity.
    *
    * @param resourceId id of entity to include tag
    * @param resourceType "User" or "UserVM" type of tags
    * @param tagSet Set of tags to include into resource
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
