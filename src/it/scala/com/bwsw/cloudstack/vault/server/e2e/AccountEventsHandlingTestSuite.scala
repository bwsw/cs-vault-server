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

import com.bwsw.cloudstack.entities.requests.tag.types.AccountTagType
import com.bwsw.cloudstack.entities.responses.tag.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.util.cloudstack.CloudStackTestEntities
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.AccountDeleteRequest
import com.bwsw.cloudstack.vault.server.util.vault.components.CommonVaultTestComponents
import com.bwsw.cloudstack.vault.server.util.{IntegrationTestsSettings, TestComponents}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountEventsHandlingTestSuite extends FlatSpec with CloudStackTestEntities with Checks with BeforeAndAfterAll {
  commitToEndForGroup(IntegrationTestsSettings.kafkaGroupId)

  val components = new TestComponents(new CommonVaultTestComponents)

  Future(components.eventManager.execute())

  "cs-vault-server" should "handle account creation/deletion" in {
    val accountId = UUID.randomUUID()
    val accountCreateRequest = getAccountCreateRequest
    accountCreateRequest.withId(accountId)
    accountDao.create(accountCreateRequest)

    Thread.sleep(20000)

    //check tags existing
    val expectedPrefixTag = Tag(
      VaultTagKey.toString(VaultTagKey.VaultPrefix),
      Paths.get(IntegrationTestsSettings.accountSecretPath, accountId.toString).toString
    )

    val tokenTuple = retrieveTokenTagsIfThereAre(expectedPrefixTag, accountId, AccountTagType, maxRetryCount = 60, retryDelay = 1000)

    //check policies and tokens
    val expectedReadTokenPolicyName = s"acl_${accountId}_ro*"
    val expectedWriteTokenPolicyName = s"acl_${accountId}_rw*"

    checkTokenPolicies(tokenTuple.readToken, List(expectedReadTokenPolicyName), components.vaultTestComponents.vaultRestRequestExecutor)
    checkTokenPolicies(tokenTuple.writeToken, List(expectedWriteTokenPolicyName), components.vaultTestComponents.vaultRestRequestExecutor)

    //check zooKeeper token's nodes existence
    val readTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "accounts", accountId.toString, VaultTagKey.VaultRO.toString.toLowerCase()
    ).toString
    val writeTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "accounts", accountId.toString, VaultTagKey.VaultRW.toString.toLowerCase()
    ).toString

    assert(components.zooKeeperService.getNodeData(readTokenPath).contains(tokenTuple.readToken))
    assert(components.zooKeeperService.getNodeData(writeTokenPath).contains(tokenTuple.writeToken))

    //tokens permissions test
    vaultPermissionTest(IntegrationTestsSettings.accountSecretPath, accountId, tokenTuple)

    //delete account
    val accountDeleteRequest = new AccountDeleteRequest(accountId)
    executor.executeRequest(accountDeleteRequest.request)

    //wait for account deletion handling
    Thread.sleep(10000)

    //check ZooKeeper account node non-existence
    assert(!components.zooKeeperService.doesNodeExist(Paths.get(IntegrationTestsSettings.zooKeeperRootNode, "accounts", accountId.toString).toString))

    //check Vault token non-existence
    checkAbsentVaultToken(tokenTuple.writeToken)
    checkAbsentVaultToken(tokenTuple.readToken)

    //check Vault policy non-existence
    checkAbsentVaultPolicy(expectedReadTokenPolicyName)
    checkAbsentVaultPolicy(expectedWriteTokenPolicyName)

    //check Vault secret non-existence
    checkVaultSecretNonExistence(IntegrationTestsSettings.accountSecretPath, accountId)
  }

  override def afterAll(): Unit = {
    components.close()
  }
}
