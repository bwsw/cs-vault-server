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

import com.bwsw.cloudstack.entities.requests.account.AccountCreateRequest
import com.bwsw.cloudstack.entities.requests.account.AccountCreateRequest.RootAdmin
import com.bwsw.cloudstack.entities.requests.vm.VmCreateRequest
import com.bwsw.cloudstack.vault.server.util.cloudstack.TestEntities
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.VmCreateTestRequest
import com.bwsw.cloudstack.vault.server.util.cloudstack.responses.VmCreateResponse
import org.scalatest.FlatSpec

class CloudStackServiceIntegrationTestSuite extends FlatSpec with TestEntities {

  "CloudStackService" should "return false on check account existence" in {
    val accountId = UUID.randomUUID()

    assert(!cloudStackService.doesAccountExist(accountId))
  }

  "CloudStackService" should "return false on check virtual machine existence" in {
    val vmId = UUID.randomUUID()

    assert(!cloudStackService.doesVirtualMachineExist(vmId))
  }

  "CloudStackService" should "return true on check account and VM existence and then retrieve the account id by the VM id" in {
    val accountId = UUID.randomUUID()
    val accountName = accountId.toString
    val domainId = retrievedAdminDomainId

    val accountCreateRequest = new AccountCreateRequest(AccountCreateRequest.Settings(
      RootAdmin,
      "test@example.com",
      "firstname",
      "lastName",
      "password",
      s"username $accountId"
    )).withId(accountId).withName(accountName).withDomain(domainId)

    accountDao.create(accountCreateRequest)

    Thread.sleep(1000)

    assert(cloudStackService.doesAccountExist(accountId))

    val vmCreateTestRequest = new VmCreateTestRequest(VmCreateRequest.Settings(
      retrievedServiceOfferingId, retrievedTemplateId, retrievedZoneId
    )).withDomainAccount(accountName, domainId).asInstanceOf[VmCreateTestRequest]

    val vmId = mapper.deserialize[VmCreateResponse](executor.executeRequest(vmCreateTestRequest.request)).vmId.id

    Thread.sleep(3000)

    assert(cloudStackService.doesVirtualMachineExist(vmId))
    assert(cloudStackService.getVmOwnerAccount(vmId) == accountId)
  }
}
