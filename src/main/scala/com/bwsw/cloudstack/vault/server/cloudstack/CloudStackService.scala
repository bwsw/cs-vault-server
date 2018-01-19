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

import com.bwsw.cloudstack.entities.dao.{AccountDao, TagDao, VirtualMachineDao}
import com.bwsw.cloudstack.entities.requests.account.AccountFindRequest
import com.bwsw.cloudstack.entities.requests.tag.{TagCreateRequest, TagFindRequest}
import com.bwsw.cloudstack.entities.requests.tag.types.{AccountTagType, VmTagType}
import com.bwsw.cloudstack.entities.requests.vm.VmFindRequest
import com.bwsw.cloudstack.entities.responses.{Account, Tag, VirtualMachine}
import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for interaction with CloudStack server with help of implementations of
  * [[com.bwsw.cloudstack.entities.dao.GenericDao]]
  *
  * @param accountDao see [[com.bwsw.cloudstack.entities.dao.AccountDao]]
  * @param tagDao see [[com.bwsw.cloudstack.entities.dao.TagDao]]
  * @param vmDao see [[com.bwsw.cloudstack.entities.dao.VirtualMachineDao]]
  */
class CloudStackService(accountDao: AccountDao,
                        tagDao: TagDao,
                        vmDao: VirtualMachineDao) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Retrieves all tags of account.
    *
    * @param accountId id of account to retrieve its tags
    *
    * @return Set of tags
    */
  def getVaultAccountTags(accountId: UUID): Set[VaultTag] = {
    logger.debug(s"getAccountTags(accountId: $accountId)")

    val tagFindRequest = new TagFindRequest().withResourceType(AccountTagType).withResource(accountId)

    def requestExecution(): Set[Tag] = tagDao.find(tagFindRequest)

    val vaultTags = tryExecuteRequest(requestExecution).map { tag =>
      VaultTag(VaultTag.Key.fromString(tag.key), tag.value)
    }

    logger.debug(s"VaultTags: $vaultTags are retrieved for account: $accountId)")
    vaultTags
  }

  /**
    * Retrieves all tags of VM.
    *
    * @param vmId id of VM to retrieve its tags
    *
    * @return Set of tags
    */
  def getVaultVmTags(vmId: UUID): Set[VaultTag] = {
    logger.debug(s"getVmTags(vmId: $vmId)")

    val tagFindRequest = new TagFindRequest().withResourceType(VmTagType).withResource(vmId)

    def requestExecution(): Set[Tag] = tagDao.find(tagFindRequest)

    val vaultTags = tryExecuteRequest(requestExecution).map { tag =>
      VaultTag(VaultTag.Key.fromString(tag.key), tag.value)
    }

    logger.debug(s"VaultTags: $vaultTags are retrieved for VM: $vmId)")
    vaultTags
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

    val vmFindRequest = new VmFindRequest().withId(vmId)

    def vmFindRequestExecution(): List[VirtualMachine] = vmDao.find(vmFindRequest)

    val vm = tryExecuteRequest(vmFindRequestExecution).headOption.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"VM with id: $vmId does not exist")
    )

    val accountFindRequest = new AccountFindRequest()
      .withName(vm.accountName)
      .withDomain(vm.domainId)

    def accountFindRequestExecution(): List[Account] = accountDao.find(accountFindRequest)

    val accountId = tryExecuteRequest(accountFindRequestExecution).headOption.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"VM: $vmId does not include the account with " +
        s"name: ${vm.accountName} within domain: ${vm.domainId}")
    ).id

    logger.debug(s"Account id: $accountId are retrieved for VM: $vmId)")
    accountId
  }

  /**
    * Check account existence.
    *
    * @param accountId id of account for check
    *
    * @return account existence check result
    */
  def doesAccountExist(accountId: UUID): Boolean = {
    logger.trace(s"doesAccountExist(accountId: $accountId)")

    val accountFindRequest = new AccountFindRequest().withId(accountId)

    def accountFindRequestExecution(): List[Account] = accountDao.find(accountFindRequest)

    tryExecuteRequest(accountFindRequestExecution).nonEmpty
  }

  /**
    * Check VM existence.
    *
    * @param vmId id of VM for check
    *
    * @return VM existence check result
    */
  def doesVirtualMachineExist(vmId: UUID): Boolean = {
    logger.trace(s"doesVirtualMachineExist(vmId: $vmId)")

    val vmFindRequest = new VmFindRequest().withId(vmId)

    def vmFindRequestExecution(): List[VirtualMachine] = vmDao.find(vmFindRequest)

    tryExecuteRequest(vmFindRequestExecution).nonEmpty
  }

  /**
    * Includes tags to specified VM.
    *
    * @param resourceId id of VM to include tag
    * @param vaultTagSet Set of vault tags to include into resource
    */
  def setVmVaultTags(resourceId: UUID, vaultTagSet: Set[VaultTag]): Unit = {
    logger.trace(s"setVmVaultTags(resourceId: $resourceId, vaultTagSet: $vaultTagSet)")
    val tagCreateRequest = new TagCreateRequest(TagCreateRequest.Settings(
      VmTagType,
      Set(resourceId),
      vaultTagSet.collect(vaultTagsToTags).toList
    ))

    tagDao.create(tagCreateRequest)
  }

  /**
    * Includes tags to specified account.
    *
    * @param resourceId id of account to include tag
    * @param vaultTagSet Set of vault tags to include into resource
    */
  def setAccountVaultTags(resourceId: UUID, vaultTagSet: Set[VaultTag]): Unit = {
    logger.trace(s"setAccountVaultTags(resourceId: $resourceId, vaultTagSet: $vaultTagSet)")
    val tagCreateRequest = new TagCreateRequest(TagCreateRequest.Settings(
      AccountTagType,
      Set(resourceId),
      vaultTagSet.collect(vaultTagsToTags).toList
    ))

    tagDao.create(tagCreateRequest)
  }

  private def tryExecuteRequest[T](request: () => T): T = {
    Try {
      request()
    } match {
      case Success(x) => x
      case Failure(e: Throwable) =>
        logger.error(s"Request execution threw an exception: $e")
        throw new CloudStackFatalException(e.toString)
    }
  }

  private def vaultTagsToTags: PartialFunction[VaultTag, Tag] = {
    case VaultTag(key, value) if key != VaultTag.Key.Other => Tag(VaultTag.Key.toString(key), value)
  }
}
