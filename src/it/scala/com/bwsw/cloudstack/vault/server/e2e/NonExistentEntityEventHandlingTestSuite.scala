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
import java.util.concurrent.TimeoutException

import com.bettercloud.vault.rest.Rest
import com.bwsw.cloudstack.entities.requests.vm.VmCreateRequest
import com.bwsw.cloudstack.vault.server.util.cloudstack.CloudStackTestEntities
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.{AccountDeleteRequest, VmCreateTestRequest, VmDeleteRequest}
import com.bwsw.cloudstack.vault.server.util.cloudstack.responses.VmCreateResponse
import com.bwsw.cloudstack.vault.server.util.vault.Constants
import com.bwsw.cloudstack.vault.server.util.vault.components.CommonVaultTestComponents
import com.bwsw.cloudstack.vault.server.util.{IntegrationTestsSettings, TestComponents}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NonExistentEntityEventHandlingTestSuite extends FlatSpec with Checks with BeforeAndAfterAll with CloudStackTestEntities {
  commitToEndForGroup(IntegrationTestsSettings.kafkaGroupId)

  val components = new TestComponents(new CommonVaultTestComponents)

  val accountId = UUID.randomUUID()
  val accountName = accountId.toString

  accountDao.create(getAccountCreateRequest.withId(accountId).withName(accountName).withDomain(retrievedAdminDomainId))

  val vmCreateTestRequest = new VmCreateTestRequest(VmCreateRequest.Settings(
    retrievedServiceOfferingId, retrievedTemplateId, retrievedZoneId
  )).withDomainAccount(accountName, retrievedAdminDomainId).asInstanceOf[VmCreateTestRequest]

  val vmId = mapper.deserialize[VmCreateResponse](executor.executeRequest(vmCreateTestRequest.request)).vmId.id

  val deleteVmRequest = new VmDeleteRequest(vmId)

  executor.executeRequest(deleteVmRequest.request)

  val accountDeleteRequest = new AccountDeleteRequest(accountId)

  executor.executeRequest(accountDeleteRequest.request)

  //wait for account and vm deletion in CloudStack server
  waitAccountAndVmDeletion(retryDelay = 1000, maxRetryCount = 10, accountId, vmId)

  val vmSecretEndpoint = s"${IntegrationTestsSettings.vaultEndpoints.head}" +
    s"${Paths.get(Constants.RequestPaths.vaultRoot, IntegrationTestsSettings.vmSecretPath, vmId.toString).toString}"

  val accountSecretEndpoint = s"${IntegrationTestsSettings.vaultEndpoints.head}" +
    s"${Paths.get(Constants.RequestPaths.vaultRoot, IntegrationTestsSettings.accountSecretPath, accountId.toString).toString}"

  writeEntitySecret(vmSecretEndpoint)
  writeEntitySecret(accountSecretEndpoint)

  Future(components.eventManager.execute())

  //wait entities deletion handling
  Thread.sleep(10000)

  "cs-vault-server" should "handle vm creation if entity does not exist, and handle vm deletion if token for the vm " +
    "are not created in vault" in {
    //check Vault policy non-existence
    val expectedReadTokenPolicyName = s"acl_${accountId}_${vmId}_ro*"
    val expectedWriteTokenPolicyName = s"acl_${accountId}_${vmId}_rw*"

    checkAbsentVaultPolicy(expectedReadTokenPolicyName)
    checkAbsentVaultPolicy(expectedWriteTokenPolicyName)

    //check ZooKeeper VM node non-existence
    assert(!components.zooKeeperService.doesNodeExist(
      Paths.get(
        IntegrationTestsSettings.zooKeeperRootNode,
        "vms",
        vmId.toString
      ).toString
    ))

    //check Vault secret non-existence after VM deletion handling
    checkVaultSecretNonExistence(IntegrationTestsSettings.vmSecretPath, vmId)
  }

  "cs-vault-server" should "handle account creation if entity does not exist, and handle account deletion if token for the vm " +
    "are not created in vault" in {
    //check Vault policy non-existence
    val expectedReadTokenPolicyName = s"acl_${accountId}_ro*"
    val expectedWriteTokenPolicyName = s"acl_${accountId}_rw*"

    checkAbsentVaultPolicy(expectedReadTokenPolicyName)
    checkAbsentVaultPolicy(expectedWriteTokenPolicyName)

    //check ZooKeeper VM node non-existence
    assert(!components.zooKeeperService.doesNodeExist(
      Paths.get(
        IntegrationTestsSettings.zooKeeperRootNode,
        "accounts",
        accountId.toString
      ).toString
    ))

    //check Vault secret non-existence after account deletion handling
    checkVaultSecretNonExistence(IntegrationTestsSettings.accountSecretPath, accountId)
  }

  override def afterAll(): Unit = {
    components.close()
  }

  private def writeEntitySecret(entityEndpoint: String) = {
    val writeVmSecretRequest = new Rest()
      .url(entityEndpoint)
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .body("{\"key\": \"value\"}".getBytes("UTF-8"))

    assert(writeVmSecretRequest.post().getStatus == Constants.Statuses.okWithEmptyBody)
  }

  private def waitAccountAndVmDeletion(retryDelay: Int, maxRetryCount: Int, accountId: UUID, vmId: UUID): Unit = {
    var retryCount = 0
    while(retryCount < maxRetryCount) {
      if(cloudStackService.doesAccountExist(accountId) || cloudStackService.doesVirtualMachineExist(vmId)) {
        retryCount = retryCount + 1
        if (retryCount == maxRetryCount) {
          throw new TimeoutException("entities was not deleted")
        } else {
          Thread.sleep(retryDelay)
        }
      } else {
        retryCount = maxRetryCount
      }
    }
  }
}
