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
import java.util.concurrent.atomic.AtomicBoolean

import com.bwsw.cloudstack.entities.requests.account.AccountFindRequest
import com.bwsw.cloudstack.entities.requests.tag.TagCreateRequest
import com.bwsw.cloudstack.entities.requests.tag.types.{AccountTagType, TagType, VmTagType}
import com.bwsw.cloudstack.entities.requests.vm.VmFindRequest
import com.bwsw.cloudstack.entities.responses.account.{Account, AccountFindResponse}
import com.bwsw.cloudstack.entities.responses.tag.Tag
import com.bwsw.cloudstack.entities.responses.user.User
import com.bwsw.cloudstack.entities.responses.vm.{VirtualMachine, VirtualMachineFindResponse}
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import com.bwsw.cloudstack.vault.server.mocks.dao.{MockAccountDao, MockTagDao, MockVirtualMachineDao}
import org.scalatest.FlatSpec

class CloudStackServiceTestSuite extends FlatSpec with TestData {
  val key1 = VaultTagKey.prefix + "vault.rw"
  val vaultKey1 = VaultTagKey.fromString(key1)
  val value1 = "value1"

  val key2 = VaultTagKey.prefix + "vault.ro"
  val vaultKey2 = VaultTagKey.fromString(key2)
  val value2 = "value2"

  //Positive tests
  "getVmOwnerAccount" should "return account id by VM id" in {
    val accountName = "admin"
    val expectedVmId = vmId
    val expectedAccountId = accountId
    val expectedDomainId = domainId

    val vmFindRequest = new VmFindRequest
    vmFindRequest.withId(expectedVmId)

    val accountFindRequest = new AccountFindRequest
    accountFindRequest.withName(accountName)
    accountFindRequest.withDomain(expectedDomainId)

    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        assert(request.getRequest.getCommand == vmFindRequest.getRequest.getCommand &&
          request.getRequest.getParameters == vmFindRequest.getRequest.getParameters)

        List(getVirtualMachine(expectedVmId, accountName, expectedDomainId))
      }
    }

    val accountDao = new MockAccountDao {
      override def find[R <: F](request: R)(implicit m: Manifest[AccountFindResponse]): List[Account] = {
        assert(request.getRequest.getCommand == accountFindRequest.getRequest.getCommand &&
          request.getRequest.getParameters == accountFindRequest.getRequest.getParameters)
        List(getAccount(expectedAccountId, accountName, expectedDomainId, List.empty[User]))
      }
    }

    val cloudStackService = new CloudStackService(
      accountDao,
      new MockTagDao,
      vmDao
    )

    val actualAccountId = cloudStackService.getVmOwnerAccount(vmId)
    assert(expectedAccountId == accountId)
  }

  "setVmVaultTags" should "create CloudStack request for creating new tag in VM" in {
    val isRun = new AtomicBoolean(false)
    val cloudStackService = getCloudStackServiceForSetVaultTagsTest(isRun, vmId, VmTagType)
    cloudStackService.setVmTags(vmId, Set(Tag(VaultTagKey.toString(vaultKey1), key1), Tag(VaultTagKey.toString(vaultKey2), key2)))

    assert(isRun.get())
  }

  "setAccountVaultTags" should "create CloudStack request for creating new tag in Account" in {
    val isRun = new AtomicBoolean(false)
    val cloudStackService = getCloudStackServiceForSetVaultTagsTest(isRun, accountId, AccountTagType)
    cloudStackService.setAccountTags(accountId, Set(Tag(VaultTagKey.toString(vaultKey1), key1), Tag(VaultTagKey.toString(vaultKey2), key2)))

    assert(isRun.get())
  }

  "doesVirtualMachineExist" should "return true if VirtualMachine exists" in {
    val expectedVmId = vmId
    val vmFindRequest = new VmFindRequest
    vmFindRequest.withId(expectedVmId)

    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        assert(request.getRequest.getCommand == vmFindRequest.getRequest.getCommand &&
          request.getRequest.getParameters == vmFindRequest.getRequest.getParameters)
        List(getVirtualMachine(expectedVmId, "accountName", domainId))
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      new MockTagDao,
      vmDao
    )

    assert(cloudStackService.doesVirtualMachineExist(expectedVmId))
  }

  "doesVirtualMachineExist" should "return false if VirtualMachine does not exist" in {
    val expectedVmId = vmId
    val vmFindRequest = new VmFindRequest
    vmFindRequest.withId(expectedVmId)

    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        assert(request.getRequest.getCommand == vmFindRequest.getRequest.getCommand &&
          request.getRequest.getParameters == vmFindRequest.getRequest.getParameters)
        List.empty[VirtualMachine]
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      new MockTagDao,
      vmDao
    )

    assert(!cloudStackService.doesVirtualMachineExist(expectedVmId))
  }

  "doesAccountExist" should "return true if Account exists" in {
    val expectedAccountId = accountId

    val accountFindRequest = new AccountFindRequest
    accountFindRequest.withId(expectedAccountId)

    val accountDao = new MockAccountDao {
      override def find[R <: F](request: R)(implicit m: Manifest[AccountFindResponse]): List[Account] = {
        assert(request.getRequest.getCommand == accountFindRequest.getRequest.getCommand &&
          request.getRequest.getParameters == accountFindRequest.getRequest.getParameters)
        List(getAccount(expectedAccountId, "accountName", UUID.randomUUID(), List.empty[User]))
      }
    }

    val cloudStackService = new CloudStackService(
      accountDao,
      new MockTagDao,
      new MockVirtualMachineDao
    )

    assert(cloudStackService.doesAccountExist(expectedAccountId))
  }

  "doesAccountExist" should "return false if Account does not exist" in {
    val expectedAccountId = accountId

    val accountFindRequest = new AccountFindRequest
    accountFindRequest.withId(expectedAccountId)

    val accountDao = new MockAccountDao {
      override def find[R <: F](request: R)(implicit m: Manifest[AccountFindResponse]): List[Account] = {
        assert(request.getRequest.getCommand == accountFindRequest.getRequest.getCommand &&
          request.getRequest.getParameters == accountFindRequest.getRequest.getParameters)
        List.empty[Account]
      }
    }

    val cloudStackService = new CloudStackService(
      accountDao,
      new MockTagDao,
      new MockVirtualMachineDao
    )

    assert(!cloudStackService.doesAccountExist(expectedAccountId))
  }

  //Negative tests
  "getVmOwnerAccount" should "throw CloudStackFatalException if accountDao throws an exception" in {
    val accountName = "admin"

    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        List(getVirtualMachine(vmId, accountName, domainId))
      }
    }

    val accountDao = new MockAccountDao {
      override def find[R <: F](request: R)(implicit m: Manifest[AccountFindResponse]): List[Account] = {
        throw new Exception
      }
    }

    val cloudStackService = new CloudStackService(
      accountDao,
      new MockTagDao,
      vmDao
    )

    assertThrows[CloudStackFatalException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getVmOwnerAccount" should "throw CloudStackFatalException if vmDao throws an exception" in {
    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        throw new Exception
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      new MockTagDao,
      vmDao
    )

    assertThrows[CloudStackFatalException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getAccountExist" should "throw CloudStackFatalException if accountDao throws an exception" in {
    val accountDao = new MockAccountDao {
      override def find[R <: F](request: R)(implicit m: Manifest[AccountFindResponse]): List[Account] = {
        throw new Exception
      }
    }

    val cloudStackService = new CloudStackService(
      accountDao,
      new MockTagDao,
      new MockVirtualMachineDao
    )

    assertThrows[CloudStackFatalException] {
      cloudStackService.doesAccountExist(accountId)
    }
  }

  "doesVirtualMachineExist" should "throw CloudStackFatalException if vmDao throws an exception" in {
    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        throw new Exception
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      new MockTagDao,
      vmDao
    )

    assertThrows[CloudStackFatalException] {
      cloudStackService.doesVirtualMachineExist(vmId)
    }
  }

  "getVmOwnerAccount" should "throw CloudStackEntityDoesNotExistException if VM with specified id does not exist" in {
    val accountName = "admin"

    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        List.empty[VirtualMachine]
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      new MockTagDao,
      vmDao
    )

    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getVmOwnerAccount" should "throw CloudStackEntityDoesNotExistException if account with specified name does not exist" in {
    val accountName = "admin"

    val vmDao = new MockVirtualMachineDao {
      override def find[R <: F](request: R)(implicit m: Manifest[VirtualMachineFindResponse]): List[VirtualMachine] = {
        List(getVirtualMachine(vmId, accountName, domainId))
      }
    }

    val accountDao = new MockAccountDao {
      override def find[R <: F](request: R)(implicit m: Manifest[AccountFindResponse]): List[Account] = {
        List.empty[Account]
      }
    }

    val cloudStackService = new CloudStackService(
      accountDao,
      new MockTagDao,
      vmDao
    )

    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  private def getCloudStackServiceForSetVaultTagsTest(isRun: AtomicBoolean, expectedResourceId: UUID, tagType: TagType) = {
    val tagCreateRequest = new TagCreateRequest(TagCreateRequest.Settings(
      tagType,
      Set(expectedResourceId),
      List(Tag(VaultTagKey.toString(vaultKey1), key1), Tag(VaultTagKey.toString(vaultKey2), key2))
    ))

    val tagDao = new MockTagDao {
      override def create[R <: C](request: R): Unit = {
        isRun.set(true)
        assert(request.getRequest.getCommand == tagCreateRequest.getRequest.getCommand &&
          request.getRequest.getParameters == tagCreateRequest.getRequest.getParameters)
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      tagDao,
      new MockVirtualMachineDao
    )

    cloudStackService
  }
}
