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
package com.bwsw.cloudstack.vault.server.e2e

import java.nio.file.Paths
import java.util.UUID

import com.bettercloud.vault.rest.Rest
import com.bwsw.cloudstack.entities.requests.vm.VmCreateRequest
import com.bwsw.cloudstack.vault.server.util.IntegrationTestsSettings
import com.bwsw.cloudstack.vault.server.util.cloudstack.TestEntities
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.{AccountDeleteRequest, VmCreateTestRequest, VmDeleteRequest}
import com.bwsw.cloudstack.vault.server.util.cloudstack.responses.VmCreateResponse
import com.bwsw.cloudstack.vault.server.util.vault.Constants
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NonExistenceEntityEventHandlingTestSuite extends FlatSpec with Checks with BeforeAndAfterAll with TestEntities {
  private val accountId = UUID.randomUUID()
  private val accountName = accountId.toString
  accountDao.create(getAccountCreateRequest.withId(accountId).withName(accountName).withDomain(retrievedAdminDomainId))

  private val vmCreateTestRequest = new VmCreateTestRequest(VmCreateRequest.Settings(
    retrievedServiceOfferingId, retrievedTemplateId, retrievedZoneId
  )).withDomainAccount(accountName, retrievedAdminDomainId).asInstanceOf[VmCreateTestRequest]

  private val vmId = mapper.deserialize[VmCreateResponse](executor.executeRequest(vmCreateTestRequest.request)).vmId.id

  private val deleteVmRequest = new VmDeleteRequest(vmId)

  executor.executeRequest(deleteVmRequest.request)

  private val accountDeleteRequest = new AccountDeleteRequest(accountId)

  executor.executeRequest(accountDeleteRequest.request)

  private val entityEndpoint = s"${IntegrationTestsSettings.vaultEndpoints.head}" +
    s"${Paths.get(Constants.RequestPaths.vaultRoot, IntegrationTestsSettings.vmSecretPath, vmId.toString).toString}"

  private val writeVmSecretRequest = new Rest()
    .url(entityEndpoint)
    .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
    .body("{\"key\": \"value\"}".getBytes("UTF-8"))

  assert(writeVmSecretRequest.post().getStatus == Constants.Statuses.okWithEmptyBody)

  val components = new TestComponents

  Future(components.eventManager.execute())

  "cs-vault-server" should "handle vm creation if entity does not exist, and handle vm deletion if token for the vm " +
    "are not created in vault" in {
    //check Vault policy non-existence
    val expectedReadTokenPolicyName = s"acl_${accountId}_${vmId}_ro*"
    val expectedWriteTokenPolicyName = s"acl_${accountId}_${vmId}_rw*"

    checkAbsenceVaultPolicy(expectedReadTokenPolicyName)
    checkAbsenceVaultPolicy(expectedWriteTokenPolicyName)

    //check ZooKeeper VM node non-existence
    assert(!components.zooKeeperService.doesNodeExist(
      Paths.get(
        IntegrationTestsSettings.zooKeeperRootNode,
        "vms",
        vmId.toString
      ).toString
    ))

    //check Vault secret non-existence
    checkVaultSecretNonExistence(IntegrationTestsSettings.vmSecretPath, vmId)
  }

  "cs-vault-server" should "handle account creation if entity does not exist, and handle account deletion if token for the vm " +
    "are not created in vault" in {
    //check Vault policy non-existence
    val expectedReadTokenPolicyName = s"acl_${accountId}_ro*"
    val expectedWriteTokenPolicyName = s"acl_${accountId}_rw*"

    checkAbsenceVaultPolicy(expectedReadTokenPolicyName)
    checkAbsenceVaultPolicy(expectedWriteTokenPolicyName)

    //check ZooKeeper VM node non-existence
    assert(!components.zooKeeperService.doesNodeExist(
      Paths.get(
        IntegrationTestsSettings.zooKeeperRootNode,
        "accounts",
        accountId.toString
      ).toString
    ))

    //check Vault secret non-existence
    checkVaultSecretNonExistence(IntegrationTestsSettings.accountSecretPath, accountId)
  }

  override def afterAll(): Unit = {
    components.close()
  }
}
