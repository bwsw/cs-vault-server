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
import com.bwsw.cloudstack.entities.requests.tag.TagCreateRequest
import com.bwsw.cloudstack.entities.requests.tag.types.{AccountTagType, VmTagType}
import com.bwsw.cloudstack.entities.requests.vm.VmFindRequest
import com.bwsw.cloudstack.entities.responses.account.Account
import com.bwsw.cloudstack.entities.responses.tag.Tag
import com.bwsw.cloudstack.entities.responses.vm.VirtualMachine
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
    * Retrieves account id for VM.
    *
    * @param vmId id of VM to retrieve account name
    *
    * @return id of account which name is indicated in VM
    * @throws CloudStackEntityDoesNotExistException if VM with specified id does not exist,
    *                                               or if account with specified name in VM does not exist.
    */
  def getVmOwnerAccount(vmId: UUID): UUID = {
    logger.trace(s"getVmOwnerAccount(vmId: $vmId)")

    val vmFindRequest = new VmFindRequest()
    vmFindRequest.withId(vmId)

    def vmFindRequestExecution: List[VirtualMachine] = vmDao.find(vmFindRequest)

    val vm = tryExecuteRequest(vmFindRequestExecution _).headOption.getOrElse(
      throw new CloudStackEntityDoesNotExistException(s"VM with id: $vmId does not exist")
    )

    val accountFindRequest = new AccountFindRequest()
    accountFindRequest.withName(vm.accountName)
    accountFindRequest.withDomain(vm.domainId)

    def accountFindRequestExecution: List[Account] = accountDao.find(accountFindRequest)

    val accountId = tryExecuteRequest(accountFindRequestExecution _).headOption.getOrElse(
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

    val accountFindRequest = new AccountFindRequest()
    accountFindRequest.withId(accountId)

    def accountFindRequestExecution: List[Account] = accountDao.find(accountFindRequest)

    tryExecuteRequest(accountFindRequestExecution _).nonEmpty
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

    val vmFindRequest = new VmFindRequest()
    vmFindRequest.withId(vmId)

    def vmFindRequestExecution: List[VirtualMachine] = vmDao.find(vmFindRequest)

    tryExecuteRequest(vmFindRequestExecution _).nonEmpty
  }

  /**
    * Includes tags to specified VM.
    *
    * @param resourceId id of VM to include tag
    * @param tagSet Set of tags to include into resource
    */
  def setVmTags(resourceId: UUID, tagSet: Set[Tag]): Unit = {
    logger.trace(s"setVmTags(resourceId: $resourceId, tagSet: $tagSet)")
    val tagCreateRequest = new TagCreateRequest(TagCreateRequest.Settings(
      VmTagType,
      Set(resourceId),
      tagSet.toList
    ))

    tagDao.create(tagCreateRequest)
  }

  /**
    * Includes tags to specified account.
    *
    * @param resourceId id of account to include tag
    * @param tagSet Set of tags to include into resource
    */
  def setAccountTags(resourceId: UUID, tagSet: Set[Tag]): Unit = {
    logger.trace(s"setAccountTags(resourceId: $resourceId, tagSet: $tagSet)")
    val tagCreateRequest = new TagCreateRequest(TagCreateRequest.Settings(
      AccountTagType,
      Set(resourceId),
      tagSet.toList
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
}
