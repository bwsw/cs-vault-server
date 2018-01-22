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
import com.bwsw.cloudstack.entities.requests.tag.types.{AccountTagType, TagType, VmTagType}
import com.bwsw.cloudstack.entities.requests.tag.{TagCreateRequest, TagFindRequest}
import com.bwsw.cloudstack.entities.requests.vm.VmFindRequest
import com.bwsw.cloudstack.entities.responses._
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import com.bwsw.cloudstack.vault.server.mocks.dao.{MockAccountDao, MockTagDao, MockVirtualMachineDao}
import com.bwsw.cloudstack.vault.server.mocks.requests.{MockAccountFindRequest, MockTagCreateRequest, MockTagFindRequest, MockVmFindRequest}
import org.scalatest.FlatSpec

class CloudStackServiceTestSuite extends FlatSpec with TestData {
  val key1 = VaultTagKey.prefix + "vault.rw"
  val vaultKey1 = VaultTagKey.fromString(key1)
  val value1 = "value1"

  val key2 = VaultTagKey.prefix + "vault.ro"
  val vaultKey2 = VaultTagKey.fromString(key2)
  val value2 = "value2"

  //Positive tests
  "getVaultAccountTags" should "return account vault tags by id" in {
    val expectedAccountId = accountId
    val expectedRequest = new MockTagFindRequest(Request.getAccountTagsRequest(expectedAccountId))

    getVaultTagsTest(expectedRequest)
  }

  "getVaultVmTags" should "return VM vault tags by id" in {
    val expectedVmId = vmId
    val expectedRequest = new MockTagFindRequest(Request.getVmTagsRequest(vmId))

    getVaultTagsTest(expectedRequest)
  }

  "getVmOwnerAccount" should "return account id by VM id" in {
    val accountName = "admin"
    val expectedVmId = vmId
    val expectedAccountId = accountId
    val expectedDomainId = domainId
    val expectedVmFindRequest = new MockVmFindRequest(Request.getVmRequest(vmId))
    val expectedAccountFindRequest = new MockAccountFindRequest(
      Request.getAccountRequestByName(accountName, expectedDomainId.toString)
    )

    val vmDao = new MockVirtualMachineDao {
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
        assert(expectedVmFindRequest.requestIsEqualTo(request))
        List(VirtualMachine(expectedVmId, accountName, expectedDomainId))
      }
    }

    val accountDao = new MockAccountDao {
      override def find(request: AccountFindRequest)(implicit m: Manifest[AccountResponse]): List[Account] = {
        assert(expectedAccountFindRequest.requestIsEqualTo(request))
        List(Account(expectedAccountId, List.empty[User]))
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
    val expectedVmFindRequest = new MockVmFindRequest(Request.getVmRequest(vmId))

    val vmDao = new MockVirtualMachineDao {
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
        assert(expectedVmFindRequest.requestIsEqualTo(request))
        List(VirtualMachine(expectedVmId, "accountName", domainId))
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
    val expectedVmFindRequest = new MockVmFindRequest(Request.getVmRequest(vmId))

    val vmDao = new MockVirtualMachineDao {
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
        assert(expectedVmFindRequest.requestIsEqualTo(request))
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

    val expectedAccountFindRequest = new MockAccountFindRequest(Request.getAccountRequest(expectedAccountId))

    val accountDao = new MockAccountDao {
      override def find(request: AccountFindRequest)(implicit m: Manifest[AccountResponse]): List[Account] = {
        assert(expectedAccountFindRequest.requestIsEqualTo(request))
        List(Account(expectedAccountId, List.empty[User]))
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

    val expectedAccountFindRequest = new MockAccountFindRequest(Request.getAccountRequest(expectedAccountId))

    val accountDao = new MockAccountDao {
      override def find(request: AccountFindRequest)(implicit m: Manifest[AccountResponse]): List[Account] = {
        assert(expectedAccountFindRequest.requestIsEqualTo(request))
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
  "getVaultAccountTags" should "throw CloudStackFatalException if accountDao throws an exception" in {
    val accountDao = new MockAccountDao {
      override def find(request: AccountFindRequest)(implicit m: Manifest[AccountResponse]): List[Account] = {
        throw new Exception
      }
    }

    val cloudStackService = new CloudStackService(
      accountDao,
      new MockTagDao,
      new MockVirtualMachineDao
    )

    assertThrows[CloudStackFatalException] {
      cloudStackService.getAccountTags(accountId)
    }
  }

  "getVaultVmTags" should "throw CloudStackFatalException if vmDao throws an exception" in {
    val vmDao = new MockVirtualMachineDao {
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
        throw new Exception
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      new MockTagDao,
      vmDao
    )

    assertThrows[CloudStackFatalException] {
      cloudStackService.getVmTags(vmId)
    }
  }

  "getVmOwnerAccount" should "throw CloudStackFatalException if accountDao throws an exception" in {
    val accountName = "admin"

    val vmDao = new MockVirtualMachineDao {
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
        List(VirtualMachine(vmId, accountName, domainId))
      }
    }

    val accountDao = new MockAccountDao {
      override def find(request: AccountFindRequest)(implicit m: Manifest[AccountResponse]): List[Account] = {
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
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
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
      override def find(request: AccountFindRequest)(implicit m: Manifest[AccountResponse]): List[Account] = {
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
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
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
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
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
      override def find(request: VmFindRequest)(implicit m: Manifest[VirtualMachinesResponse]): List[VirtualMachine] = {
        List(VirtualMachine(vmId, accountName, domainId))
      }
    }

    val accountDao = new MockAccountDao {
      override def find(request: AccountFindRequest)(implicit m: Manifest[AccountResponse]): List[Account] = {
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

  private def getVaultTagsTest(expectedRequest: MockTagFindRequest) = {
    val tagDao = new MockTagDao {
      override def find(request: TagFindRequest)(implicit m: Manifest[TagResponse]): Set[Tag] = {
        assert(expectedRequest.requestIsEqualTo(request))
        Set(Tag(key1, value1))
      }
    }

    val cloudStackService = new CloudStackService(
      new MockAccountDao,
      tagDao,
      new MockVirtualMachineDao
    )

    val tags = cloudStackService.getAccountTags(accountId)
    assert(Set(Tag(VaultTagKey.toString(vaultKey1),value1)) == tags)
  }

  private def getCloudStackServiceForSetVaultTagsTest(isRun: AtomicBoolean, expectedResourceId: UUID, tagType: TagType) = {
    val expectedVmTagsCreateRequest = new MockTagCreateRequest(
      Request.getSetTagsRequest(
        expectedResourceId,
        tagType,
        (Tag(VaultTagKey.toString(vaultKey1), key1), Tag(VaultTagKey.toString(vaultKey2), key2))
      ),
      TagCreateRequest.Settings(tagType, Set(expectedResourceId), List(Tag(key1,value1), Tag(key2, value2)))
    )

    val tagDao = new MockTagDao {
      override def create(request: TagCreateRequest): Unit = {
        isRun.set(true)
        assert(expectedVmTagsCreateRequest.requestEqualsTo(request))
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
