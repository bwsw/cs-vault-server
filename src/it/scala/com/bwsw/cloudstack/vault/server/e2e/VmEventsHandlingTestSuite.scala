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

import com.bwsw.cloudstack.entities.requests.tag.types.VmTagType
import com.bwsw.cloudstack.entities.requests.vm.VmCreateRequest
import com.bwsw.cloudstack.entities.responses.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.util.IntegrationTestsSettings
import com.bwsw.cloudstack.vault.server.util.cloudstack.TestEntities
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.{VmCreateTestRequest, VmDeleteRequest}
import com.bwsw.cloudstack.vault.server.util.cloudstack.responses.VmCreateResponse
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VmEventsHandlingTestSuite extends FlatSpec with TestEntities with Checks with BeforeAndAfterAll {

  commitToEndForGroup(IntegrationTestsSettings.kafkaGroupId)

  val components = new TestComponents

  Future(components.eventManager.execute())

  "cs-vault-server" should "handle vm creation/deletion" in {
    val accountId = UUID.randomUUID()
    val accountName = accountId.toString
    accountDao.create(getAccountCreateRequest.withId(accountId).withName(accountName).withDomain(retrievedAdminDomainId))

    val vmCreateTestRequest = new VmCreateTestRequest(VmCreateRequest.Settings(
      retrievedServiceOfferingId, retrievedTemplateId, retrievedZoneId
    )).withDomainAccount(accountName, retrievedAdminDomainId).asInstanceOf[VmCreateTestRequest]

    val vmId = mapper.deserialize[VmCreateResponse](executor.executeRequest(vmCreateTestRequest.request)).vmId.id

    Thread.sleep(20000)

    //check tags existing
    val expectedPrefixTag = Tag(
      VaultTagKey.toString(VaultTagKey.VaultPrefix),
      Paths.get(IntegrationTestsSettings.vmSecretPath, vmId.toString).toString
    )

    val tokenTuple = retrieveTokenTagsIfThereAre(expectedPrefixTag, vmId, VmTagType)

    //check policies and tokens
    val expectedReadTokenPolicyName = s"acl_${accountId}_${vmId}_ro*"
    val expectedWriteTokenPolicyName = s"acl_${accountId}_${vmId}_rw*"

    checkTokenPolicies(tokenTuple.readToken, List(expectedReadTokenPolicyName))
    checkTokenPolicies(tokenTuple.writeToken, List(expectedWriteTokenPolicyName))

    //check zooKeeper token's nodes existence
    val readTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString, VaultTagKey.VaultRO.toString.toLowerCase()
    ).toString
    val writeTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString, VaultTagKey.VaultRW.toString.toLowerCase()
    ).toString

    assert(components.zooKeeperService.getNodeData(readTokenPath).contains(tokenTuple.readToken))
    assert(components.zooKeeperService.getNodeData(writeTokenPath).contains(tokenTuple.writeToken))

    //tokens permissions test
    vaultPermissionTest(IntegrationTestsSettings.vmSecretPath, vmId, tokenTuple)

    //delete VM
    val vmDeleteRequest = new VmDeleteRequest(vmId)
    executor.executeRequest(vmDeleteRequest.request)

    //wait for VM deletion handling
    Thread.sleep(10000)

    //check ZooKeeper VM node non-existence
    assert(!components.zooKeeperService.doesNodeExist(Paths.get(IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString).toString))

    //check Vault token non-existence
    checkAbsentVaultToken(tokenTuple.writeToken)
    checkAbsentVaultToken(tokenTuple.readToken)

    //check Vault policy non-existence
    checkAbsentVaultPolicy(expectedReadTokenPolicyName)
    checkAbsentVaultPolicy(expectedWriteTokenPolicyName)

    //check Vault secret non-existence
    checkVaultSecretNonExistence(IntegrationTestsSettings.vmSecretPath, vmId)
  }

  override def afterAll(): Unit = {
    components.close()
  }
}
