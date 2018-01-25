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
package com.bwsw.cloudstack.vault.server.vault

import java.nio.file.Paths
import java.util.UUID

import com.bettercloud.vault.json.Json
import com.bettercloud.vault.rest.{Rest, RestResponse}
import com.bwsw.cloudstack.vault.server.IntegrationTestsComponents
import com.bwsw.cloudstack.vault.server.common.Converter
import com.bwsw.cloudstack.vault.server.util.vault.{Constants, PolicyData, SecretData, TokenData}
import com.bwsw.cloudstack.vault.server.util.{IntegrationTestsSettings, RequestPath}
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import org.scalatest.FlatSpec

class VaultServiceIntegrationTestSuite extends FlatSpec with IntegrationTestsComponents {

  "VaultService" should "create and revoke token with specified policy and period" in {
    val accountId = UUID.randomUUID()
    val expectedPolicyPath = "secret/it/test"
    val expectedTokenPeriod = Converter.daysToSeconds(IntegrationTestsSettings.vaultTokenPeriod)

    val policy = Policy.createAccountReadPolicy(accountId, expectedPolicyPath)
    vaultService.writePolicy(policy)

    val token = vaultService.createToken(List(policy))

    val jsonTokenId = Json.`object`().add("token", token.toString).toString
    val responseLookupToken = vaultRestRequestExecutor.executeTokenLookupRequest(jsonTokenId)

    val lookupToken = mapper.deserialize[TokenData](responseLookupToken).data

    assert(lookupToken.period == expectedTokenPeriod)
    assert(lookupToken.policies.toSet == Set(policy.name))

    val policiesFromRevokedToken = vaultService.revokeToken(token)

    assert(policiesFromRevokedToken.toSet == Set(policy.name))

    val responseRevokedToken = new Rest()
      .url(s"${vaultService.endpoints.head}${RequestPath.vaultTokenLookup}")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .body(jsonTokenId.getBytes("UTF-8")).post()

    assert(responseRevokedToken.getStatus == Constants.Statuses.tokenNotFound)
  }

  "VaultService" should "create secret path hierarchy and then delete it" in {
    val secretRootPath = "first"
    val secretSubRootPath = Paths.get(secretRootPath,"second").toString
    val secretValue = "value"
    val secretKey = "key"
    val secretJson =  "{\"" + secretKey + "\":\"" + secretValue + "\"}"

    createSecret(secretRootPath, secretJson)
    createSecret(secretSubRootPath, secretJson)

    val responseGetRootChildPaths = getRootSecretHierarchyList

    val actualSecretList = mapper.deserialize[SecretData](new String(responseGetRootChildPaths.getBody, "UTF-8")).data.keys
    val expectedSecretList = List(secretRootPath, s"$secretRootPath/")
    assert(expectedSecretList == actualSecretList)

    vaultService.deleteSecretsRecursively(s"${Constants.RequestPaths.secret}/$secretRootPath")

    val responseGetEmptyRootChildPaths = getRootSecretHierarchyList

    assert(responseGetEmptyRootChildPaths.getStatus == Constants.Statuses.childPathsWithSecretsNotFound)
  }

  "VaultService" should "not fail after nonexistent secret deletion" in {
    val nonexistentPath = UUID.randomUUID().toString
    assert(vaultService.deleteSecretsRecursively(s"${Constants.RequestPaths.secret}/$nonexistentPath").isInstanceOf[Unit])
  }

  "VaultService" should "write and delete policy with 'write' permissions" in {
    val accountId = UUID.randomUUID()
    val policyPath = s"secret/it/$accountId/write/"
    val expectedPolicy = Policy.createAccountWritePolicy(accountId, policyPath)
    testPolicy(expectedPolicy)
  }

  "VaultService" should "write and delete policy with 'read' permissions" in {
    val accountId = UUID.randomUUID()
    val policyPath = s"secret/it/$accountId/read/"
    val expectedPolicy = Policy.createAccountReadPolicy(accountId, policyPath)
    testPolicy(expectedPolicy)
  }

  def createSecret(path: String, secretJson: String): Unit = {
    new Rest()
      .url(s"${vaultService.endpoints.head}${Constants.RequestPaths.secret}/$path")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken)
      .body(secretJson.getBytes("UTF-8")).post()
  }

  def getRootSecretHierarchyList: RestResponse = {
    new Rest()
      .url(s"${vaultService.endpoints.head}${Constants.RequestPaths.secret}?list=true")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken).get()
  }

  def testPolicy(policy: Policy): Unit = {
    val expectedRules = "path \"" + policy.path + "\" {\"policy\"=\"" + Policy.ACL.toString(policy.acl) + "\"}"

    vaultService.writePolicy(policy)

    val responseGetCreatedPolicy = new Rest()
      .url(s"${vaultService.endpoints.head}${RequestPath.vaultPolicy}/${policy.name}")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken).get()

    val actualPolicyJson = new String(responseGetCreatedPolicy.getBody, "UTF-8")
    val actualPolicyData = mapper.deserialize[PolicyData](actualPolicyJson)

    assert(actualPolicyData.name == policy.name)
    assert(actualPolicyData.rules == expectedRules)

    vaultService.deletePolicy(policy.name)

    val responseGetDeletedPolicy = new Rest()
      .url(s"${vaultService.endpoints.head}${RequestPath.vaultPolicy}/${policy.name}")
      .header("X-Vault-Token", IntegrationTestsSettings.vaultRootToken).get

    assert(responseGetDeletedPolicy.getStatus == Constants.Statuses.policyNotFound)
  }
}
