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
import com.bwsw.cloudstack.entities.requests.tag.types.TagType
import com.bwsw.cloudstack.entities.responses.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.util.cloudstack.components.CloudStackTestsComponents
import com.bwsw.cloudstack.vault.server.util.e2e.entities.TokenTuple
import com.bwsw.cloudstack.vault.server.util.kafka.TestConsumer
import com.bwsw.cloudstack.vault.server.util.vault.{Constants, TokenData}
import com.bwsw.cloudstack.vault.server.util.{IntegrationTestsSettings, RequestPath}
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestExecutor

import scala.util.{Failure, Success, Try}

trait Checks extends CloudStackTestsComponents {
  val expectedHostTag = Tag(
    VaultTagKey.toString(VaultTagKey.VaultHosts),
    IntegrationTestsSettings.vaultEndpoints.map(endpoint => s"$endpoint${RequestPath.vaultRoot}").mkString(",")
  )

  def checkAbsentVaultPolicy(policyName: String): Unit = {
    val responseLookupToken = new Rest()
      .url(s"${IntegrationTestsSettings.vaultEndpoints.head}${Constants.RequestPaths.vaultPolicy}/$policyName")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .get()

    assert(responseLookupToken.getStatus == Constants.Statuses.policyNotFound)
  }

  def checkAbsentVaultToken(tokenId: String): Unit = {
    val responseLookupToken = new Rest()
      .url(s"${IntegrationTestsSettings.vaultEndpoints.head}${Constants.RequestPaths.tokenLookup}/$tokenId")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .get()

    assert(responseLookupToken.getStatus == Constants.Statuses.tokenNotFound)
  }

  def vaultPermissionTest(entityPath: String, entityId: UUID, tokenTuple: TokenTuple): Unit = {
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
    import com.bettercloud.vault.{Vault, VaultConfig}
    val config = new VaultConfig().address(IntegrationTestsSettings.vaultEndpoints.head).token(tokenTuple.readToken).build()
    val vault = new Vault(config)
    val actualValue = vault.logical()
      .read(Paths.get(entityPath, entityId.toString).toString)
      .getData.get(secretKey)

    assert(actualValue == expectedValue)
  }

  def checkVaultSecretNonExistence(secretPath: String, entityId: UUID): Unit = {
    val entityEndpoint = s"${IntegrationTestsSettings.vaultEndpoints.head}" +
      s"${Paths.get(Constants.RequestPaths.vaultRoot, secretPath, entityId.toString).toString}"

    val responseGetSecret = new Rest()
      .url(entityEndpoint)
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .get()

    assert(responseGetSecret.getStatus == Constants.Statuses.secretNotFound)
  }

  def getAccountCreateRequest: AccountCreateRequest = {
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

  def retrieveTokenTagsIfThereAre(expectedPrefixTag: Tag,
                                  entityId: UUID,
                                  tagType: TagType,
                                  maxRetryCount: Int,
                                  retryDelay: Int): TokenTuple = {
    val findRequest = new TagFindRequest().withResource(entityId).withResourceType(tagType)

    var tags = Set.empty[Tag]
    var retryCount = 0

    while(retryCount < maxRetryCount && tags.isEmpty) {
      tags = tagDao.find(findRequest)
      retryCount = retryCount + 1
      Thread.sleep(retryDelay)
    }

    val roTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRO
    }

    val rwTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRW
    }

    val expectedEnvironmentTags = Set(expectedHostTag, expectedPrefixTag)

    assert(expectedEnvironmentTags.subsetOf(tags), s"tags:$tags of entity: $entityId are not containing expectedEnvironmentTags: $expectedEnvironmentTags")
    assert(roTokenTagOpt.nonEmpty)
    assert(rwTokenTagOpt.nonEmpty)

    TokenTuple(roTokenTagOpt.get.value, rwTokenTagOpt.get.value)
  }

  def checkTokenPolicies(tokenId: String, expectedPolicies: List[String], vaultRestRequestExecutor: VaultRestRequestExecutor): Unit = {
    val jsonTokenId = Json.`object`().add("token", tokenId).toString

    val lookupResponseString = vaultRestRequestExecutor.executeTokenLookupRequest(jsonTokenId)

    val actualPolicyNameList = mapper.deserialize[TokenData](lookupResponseString).data.policies

    assert(actualPolicyNameList == expectedPolicies, s"token: $tokenId has unexpected policy")
  }

  def commitToEndForGroup(groupId: String): Unit = {
    val testCousumer = new TestConsumer(IntegrationTestsSettings.kafkaEndpoints, groupId)
    Try {
      IntegrationTestsSettings.kafkaTopics.foreach(testCousumer.commitToEnd)
    } match {
      case Success(x) =>
        testCousumer.close()
      case Failure(e: Throwable) =>
        testCousumer.close()
        throw e
    }
  }
}
