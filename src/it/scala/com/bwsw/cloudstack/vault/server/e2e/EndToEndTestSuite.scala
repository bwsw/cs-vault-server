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
import com.bettercloud.vault.rest.Rest
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
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.{VmCreateTestRequest, VmDeleteRequest}
import com.bwsw.cloudstack.vault.server.util.cloudstack.responses.VmCreateResponse
import com.bwsw.cloudstack.vault.server.util.e2e.entities.TokenTuple
import com.bwsw.cloudstack.vault.server.util.vault._
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
    val expectedReadTokenPolicyName = s"acl_${accountId}_${vmId}_ro*"
    val expectedWriteTokenPolicyName = s"acl_${accountId}_${vmId}_rw*"

    checkTokenPoliciesNames(tokenTuple.readToken, List(expectedReadTokenPolicyName))
    checkTokenPoliciesNames(tokenTuple.writeToken, List(expectedWriteTokenPolicyName))

    //check zooKeeper token's nodes existence
    val readTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString, VaultTagKey.VaultRO.toString.toLowerCase()
    ).toString
    val writeTokenPath = Paths.get(
      IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString, VaultTagKey.VaultRW.toString.toLowerCase()
    ).toString

    assert(zooKeeperService.getNodeData(readTokenPath).contains(tokenTuple.readToken))
    assert(zooKeeperService.getNodeData(writeTokenPath).contains(tokenTuple.writeToken))

    //tokens permissions test
    vaultPermissionTest(IntegrationTestsSettings.vmSecretPath, vmId, tokenTuple)

    //delete VM
    val vmDeleteRequest = new VmDeleteRequest(vmId)
    executor.executeRequest(vmDeleteRequest.request)

    //wait VM deletion handling
    Thread.sleep(4000)

    //check ZooKeeper VM node non-existence
    assert(!zooKeeperService.doesNodeExist(Paths.get(IntegrationTestsSettings.zooKeeperRootNode, "vms", vmId.toString).toString))

    //check Vault token non-existence
    checkAbsenceVaultToken(tokenTuple.writeToken)
    checkAbsenceVaultToken(tokenTuple.readToken)

    //check Vault policy non-existence
    checkAbsenceVaultPolicy(expectedReadTokenPolicyName)
    checkAbsenceVaultPolicy(expectedWriteTokenPolicyName)

    //check Vault secret non-existence
    val responseGetSecret = new Rest()
      .url(s"${IntegrationTestsSettings.vaultEndpoints.head}/${Paths.get(IntegrationTestsSettings.vmSecretPath, vmId.toString)}")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .get()

    assert(responseGetSecret.getStatus == Constants.Statuses.secretNotFound)
  }

  private def checkAbsenceVaultPolicy(policyName: String): Unit = {
    val responseLookupToken = new Rest()
      .url(s"${IntegrationTestsSettings.vaultEndpoints.head}${Constants.RequestPaths.vaultPolicy}/$policyName")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .get()

    assert(responseLookupToken.getStatus == Constants.Statuses.policyNotFound)
  }

  private def checkAbsenceVaultToken(tokenId: String): Unit = {
    val responseLookupToken = new Rest()
      .url(s"${IntegrationTestsSettings.vaultEndpoints.head}${Constants.RequestPaths.tokenLookup}/$tokenId")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .get()

    assert(responseLookupToken.getStatus == Constants.Statuses.tokenNotFound)
  }

  private def vaultPermissionTest(entityPath: String, entityId: UUID, tokenTuple: TokenTuple): Unit = {
    val secretKey = "testKey"
    val expectedValue = UUID.randomUUID().toString
    //try write secret into unavailable path
    val unavailableEndpoint = s"${IntegrationTestsSettings.vaultEndpoints.head}" +
      s"${Paths.get(Constants.RequestPaths.vaultRoot, entityPath).toString}"

    val secretJson = "{\"" + s"$secretKey" +"\": \"" + s"$expectedValue" + "\"}"
    val requestToUnavailablePath = new Rest()
      .url(unavailableEndpoint)
      .header("X-Vault-Token", tokenTuple.writeToken)
      .body(secretJson.getBytes("UTF-8"))

    assert(requestToUnavailablePath.post().getStatus == Constants.Statuses.permissionDenied)

    //try write secret with "read" token
    val entityEndpoint = s"${IntegrationTestsSettings.vaultEndpoints.head}" +
      s"${Paths.get(Constants.RequestPaths.vaultRoot, entityPath, entityId.toString).toString}"

    val requestWithIncorrectToken = new Rest()
      .url(entityEndpoint)
      .header("X-Vault-Token", tokenTuple.readToken)
      .body(secretJson.getBytes("UTF-8"))
    assert(requestWithIncorrectToken.post().getStatus == Constants.Statuses.permissionDenied)

    //write secret with "write" token to root entity path
    val correctWriteToRootRequest = new Rest()
      .url(entityEndpoint)
      .header("X-Vault-Token", tokenTuple.writeToken)
      .body(secretJson.getBytes("UTF-8"))
    assert(correctWriteToRootRequest.post().getStatus == Constants.Statuses.okWithEmptyBody)

    //write secret with "write" token to sub-root path
    val correctWriteRequest = new Rest()
      .url(s"$entityEndpoint/test")
      .header("X-Vault-Token", tokenTuple.writeToken)
      .body(secretJson.getBytes("UTF-8"))
    assert(correctWriteRequest.post().getStatus == Constants.Statuses.okWithEmptyBody)

    //read secret with "read" token
    import com.bettercloud.vault.Vault
    import com.bettercloud.vault.VaultConfig
    val config = new VaultConfig().address(IntegrationTestsSettings.vaultEndpoints.head).token(tokenTuple.readToken).build()
    val vault = new Vault(config)
    val actualValue = vault.logical()
      .read(Paths.get(entityPath, entityId.toString).toString)
      .getData.get(secretKey)

    assert(actualValue == expectedValue)
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
    val jsonTokenId = Json.`object`().add("token", tokenId).toString

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
