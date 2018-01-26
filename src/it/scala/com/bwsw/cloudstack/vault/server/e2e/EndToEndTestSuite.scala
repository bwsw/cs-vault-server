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

import com.bettercloud.vault.json.Json
import com.bwsw.cloudstack.entities.requests.account.AccountCreateRequest
import com.bwsw.cloudstack.entities.requests.account.AccountCreateRequest.RootAdmin
import com.bwsw.cloudstack.entities.requests.tag.TagFindRequest
import com.bwsw.cloudstack.entities.requests.tag.types.{TagType, VmTagType}
import com.bwsw.cloudstack.entities.requests.vm.VmCreateRequest
import com.bwsw.cloudstack.entities.responses.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.EventManager
import com.bwsw.cloudstack.vault.server.util.{IntegrationTestsSettings, RequestPath}
import com.bwsw.cloudstack.vault.server.util.cloudstack.TestEntities
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.VmCreateTestRequest
import com.bwsw.cloudstack.vault.server.util.cloudstack.responses.VmCreateResponse
import com.bwsw.cloudstack.vault.server.util.e2e.entities.TokenTuple
import com.bwsw.cloudstack.vault.server.util.vault.{LookupToken, TokenData}
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.kafka.reader.Consumer
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EndToEndTestSuite extends FlatSpec with TestEntities with BeforeAndAfterAll {
  val expectedHostTag = Tag(
    VaultTagKey.toString(VaultTagKey.VaultHosts),
    IntegrationTestsSettings.vaultEndpoints.map(endpoint => s"$endpoint${RequestPath.vaultRoot}").mkString(",")
  )

  val consumer = new Consumer[String,String](Consumer.Settings(
    IntegrationTestsSettings.kafkaEndpoints,
    IntegrationTestsSettings.kafkaGroupId
  ))

  val zooKeeperService = new ZooKeeperService(ZooKeeperService.Settings(
    IntegrationTestsSettings.zooKeeperEndpoints,
    IntegrationTestsSettings.zooKeeperRetryDelay
  ))

  val controllerSettings = CloudStackVaultController.Settings(
    IntegrationTestsSettings.vmSecretPath,
    IntegrationTestsSettings.accountSecretPath,
    IntegrationTestsSettings.zooKeeperRootNode
  )

  val controller = new CloudStackVaultController(vaultService, cloudStackService, zooKeeperService, controllerSettings)

  val eventManagerSettings = EventManager.Settings(
    IntegrationTestsSettings.kafkaTopics.toList,
    IntegrationTestsSettings.kafkaEventCount
  )

  val eventManager = new EventManager(
    consumer,
    mapper,
    controller,
    eventManagerSettings
  )

  Future(eventManager.execute())

  it should "handle vm creation/deletion" in {
    val accountId = UUID.randomUUID()
    val accountName = accountId.toString
    accountDao.create(getAccountCreateRequest.withId(accountId).withName(accountName).withDomain(retrievedAdminDomainId))

    val vmCreateTestRequest = new VmCreateTestRequest(VmCreateRequest.Settings(
      retrievedServiceOfferingId, retrievedTemplateId, retrievedZoneId
    )).withDomainAccount(accountName, retrievedAdminDomainId).asInstanceOf[VmCreateTestRequest]

    val vmId = mapper.deserialize[VmCreateResponse](executor.executeRequest(vmCreateTestRequest.request)).vmId.id

    Thread.sleep(10000)

    //check tags existing
    val expectedPrefixTag = Tag(
      VaultTagKey.toString(VaultTagKey.VaultPrefix),
      Paths.get(IntegrationTestsSettings.vmSecretPath, vmId.toString).toString
    )

    val tokenTuple = retrieveTokenTagsIfThereAre(expectedPrefixTag, vmId, VmTagType)

    //check policies and tokens
    val expectedReadTokenPolicyNameList = List(s"acl_${accountId}_${vmId}_ro*")
    val expectedWriteTokenPolicyNameList = List(s"acl_${accountId}_${vmId}_rw*")

    checkTokenPoliciesNames(tokenTuple.readToken, expectedReadTokenPolicyNameList)
    checkTokenPoliciesNames(tokenTuple.writeToken, expectedWriteTokenPolicyNameList)

    //check zooKeeper token's nodes existence
    val readTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString, VaultTagKey.VaultRO.toString.toLowerCase()
    ).toString
    val writeTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString, VaultTagKey.VaultRW.toString.toLowerCase()
    ).toString

    assert(zooKeeperService.getNodeData(readTokenPath).contains(tokenTuple.readToken))
    assert(zooKeeperService.getNodeData(writeTokenPath).contains(tokenTuple.writeToken))
  }

  private def getAccountCreateRequest: AccountCreateRequest = {
    val userName = UUID.randomUUID()
    new AccountCreateRequest(AccountCreateRequest.Settings(
      RootAdmin,
      "test@example.com",
      "firstname",
      "lastName",
      "password",
      s"user $userName"
    ))
  }

  private def retrieveTokenTagsIfThereAre(expectedPrefixTag: Tag, entityId: UUID, tagType: TagType): TokenTuple = {
    val tags = tagDao.find(new TagFindRequest().withResource(entityId).withResourceType(tagType))

    val roTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRO
    }

    val rwTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRW
    }

    assert(Set(expectedHostTag, expectedPrefixTag).subsetOf(tags))
    assert(roTokenTagOpt.nonEmpty)
    assert(rwTokenTagOpt.nonEmpty)

    TokenTuple(roTokenTagOpt.get.value, rwTokenTagOpt.get.value)
  }

  private def checkTokenPoliciesNames(tokenId: String, expectedPolicyNameList: List[String]): Unit = {
    val jsonTokenId = Json.`object`().add("token", tokenId.toString).toString

    val lookupResponseString = vaultRestRequestExecutor.executeTokenLookupRequest(jsonTokenId)

    val actualPolicyNameList = mapper.deserialize[TokenData](lookupResponseString).data.policies

    assert(actualPolicyNameList == expectedPolicyNameList, s"token: $tokenId has unexpected policy")
  }

  override def afterAll(): Unit = {
    eventManager.close()
    zooKeeperService.close
    consumer.close()
  }
}
