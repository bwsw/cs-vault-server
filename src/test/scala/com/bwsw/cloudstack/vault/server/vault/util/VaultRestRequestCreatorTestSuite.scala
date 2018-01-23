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
package com.bwsw.cloudstack.vault.server.vault.util

import com.bettercloud.vault.json.Json
import com.bettercloud.vault.rest.{Rest, RestException, RestResponse}
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.mocks.MockConfig.vaultRestRequestExecutorSettings
import com.bwsw.cloudstack.vault.server.vault.TestData
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.vault.entities.Token.TokenInitParameters
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultFatalException
import org.scalatest.{FlatSpec, PrivateMethodTester}

class VaultRestRequestCreatorTestSuite extends FlatSpec with TestData with PrivateMethodTester with BaseTestSuite {
  val vmSecretPath = settings.cloudStackVaultControllerSettings.vmSecretPath
  // Positive tests
  "executeTokenCreateRequest" should "create request which returns token" in {
    val policyName = "policyName"
    val period = 1000
    val expectedResponseBody = getTokenJsonResponse(token.toString).getBytes("UTF-8")
    val expectedPath = "/v1/auth/token/create"
    val expectedData = getTokenInitParametersJson(noDefaultPolicy = true, policyName, period)

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            200,
            "application/json",
            expectedResponseBody
          )
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val tokenInitParameters = TokenInitParameters(noDefaultPolicy = true, List(policyName), period)
    val resultTokenResponse = vaultRestRequestCreator.executeTokenCreateRequest(mapper.serialize(tokenInitParameters))

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "executeTokenRevokeRequest" should "create request which revokes token" in {
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = "/v1/auth/token/revoke"
    val expectedData = getTokenJson(token.toString)

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val jsonTokenId = Json.`object`().add("token", token.toString).toString
    val resultTokenResponse = vaultRestRequestCreator.executeTokenRevokeRequest(jsonTokenId)

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "executeTokenLookUpRequest" should "create request which returns lookup token" in {
    val policyName = "policy"
    val pathToSecret = "path/secret"
    val expectedResponseBody = getLookupTokenJsonResponse(policyName).getBytes
    val expectedPath = "/v1/auth/token/lookup"
    val expectedData = getTokenJson(token.toString)

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            200,
            "application/json",
            expectedResponseBody
          )
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val jsonTokenId = Json.`object`().add("token", token.toString).toString
    val resultTokenResponse = vaultRestRequestCreator.executeTokenLookupRequest(jsonTokenId)

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "executePolicyCreateRequest" should "create request which creates policy" in {
    val policy = Policy.createVmWritePolicy(accountId, vmId, vmSecretPath)
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = s"/v1/sys/policy/${policy.name}"
    val expectedData = getPolicyJson(policy)

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def put(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"${vaultRestRequestExecutorSettings.endpoints}$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val resultTokenResponse = vaultRestRequestCreator.executePolicyCreateRequest(policy.name, policy.jsonString)

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "executePolicyDeleteRequest" should "create request which deletes policy" in {
    val policy = Policy.createVmReadPolicy(accountId, vmId, vmSecretPath)
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = s"/v1/sys/policy/${policy.name}"
    val expectedData = ""

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def delete(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val resultTokenResponse = vaultRestRequestCreator.executePolicyDeleteRequest(policy.name)

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "executeDeleteSecretRequest" should "create request which deletes secret" in {
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = "/v1/secret/test/path"
    val expectedData = ""

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def delete(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val resultTokenResponse = vaultRestRequestCreator.executeDeleteSecretRequest(expectedPath)

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "executeGetSubSecretPathsRequest" should "return json with sub secrets paths" in {
    val firstPath = "data"
    val secondPath = "subDataRoot"
    val expectedRequestPath = s"/$firstPath"
    val expectedResponseBody = "{\"data\":{\"keys\": [\"" + s"$firstPath" + "\", \"" + s"$secondPath" + "\"]}}"

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        assert(path == s"$expectedRequestPath?list=true", "path is wrong")
        assert(data == "", "data is wrong")
        new Rest() {
          override def get(): RestResponse = new RestResponse(
            200,
            "application/json",
            expectedResponseBody.getBytes("UTF-8")
          )
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val resultTokenResponse = vaultRestRequestCreator.executeGetSubSecretPathsRequest(s"/$firstPath")

    assert(resultTokenResponse == expectedResponseBody)
  }

  // Negative tests
  "executeTokenCreateRequest" should "throw VaultFatalException if response status is not equal to expected status" in {

    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            201,
            "application/json",
            Array.empty[Byte]
          )
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val tokenInitParameters = TokenInitParameters(noDefaultPolicy = true, List("name"), 1000)

    assertThrows[VaultFatalException] {
      vaultRestRequestCreator.executeTokenCreateRequest(mapper.serialize(tokenInitParameters))
    }
  }

  "executeTokenRevokeRequest" should "throw VaultFatalException if Rest throws an exception that is different from RestException" in {
    val vaultRestRequestCreator = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override protected def createRest(path: String, data: String)(endpoint: String): Rest = {
        new Rest() {
          override def post(): RestResponse = throw new Exception("test exception")
        }.url(s"$endpoint$path")
          .header("X-Vault-Token", vaultRestRequestExecutorSettings.rootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val jsonTokenId = Json.`object`().add("token", token.toString).toString

    assertThrows[VaultFatalException] {
      vaultRestRequestCreator.executeTokenRevokeRequest(jsonTokenId)
    }
  }

  "tryExecuteRequest" should "execute request" in {
    val expectedResponseStatus = 200
    val expectedResponse = "test"
    val endpoint = vaultRestRequestExecutorSettings.endpoints(0)

    val tryExecuteRequest = PrivateMethod[String]('tryExecuteRequest)

    val vaultRestRequestExecutor = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override val endpointQueue = getEndpointQueue(vaultRestRequestExecutorSettings.endpoints.toList)

      override def tryExecuteRequest(request: String => () => RestResponse,
                                     expectedResponseStatuses: List[Int],
                                     requestDescription: String): String = {
        super.tryExecuteRequest(request, expectedResponseStatuses, requestDescription)
      }
    }

    def response(): String = vaultRestRequestExecutor invokePrivate tryExecuteRequest(
      (endpoint: String) => () => new RestResponse(expectedResponseStatus, "application/json", expectedResponse.getBytes),
      List(expectedResponseStatus),
      "request description"
    )

    assert(response() == expectedResponse)
  }

  "tryExecuteRequest" should "restart request execution after RestException(java.net.ConnectException)" in {
    val expectedResponseStatus = 200
    val expectedResponse = "test"
    val correctEndpoint = "localhost1"
    val incorrecteEndpoint = "localhost2"
    val expectedUsedEndpoints = List(incorrecteEndpoint, correctEndpoint)

    var actualUsedEndpoints = List.empty[String]

    val tryExecuteRequest = PrivateMethod[String]('tryExecuteRequest)

    val vaultRestRequestExecutor = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override val endpointQueue = getEndpointQueue(List(incorrecteEndpoint, correctEndpoint))

      override def tryExecuteRequest(request: String => () => RestResponse,
                                     expectedResponseStatuses: List[Int],
                                     requestDescription: String): String = {
        super.tryExecuteRequest(request, expectedResponseStatuses, requestDescription)
      }
    }

    def response(): String = vaultRestRequestExecutor invokePrivate tryExecuteRequest(
      (endpoint: String) => () => {
        actualUsedEndpoints = actualUsedEndpoints ::: endpoint :: Nil
        if (endpoint == incorrecteEndpoint) {
          throw new RestException(new java.net.ConnectException())
        } else {
          new RestResponse(expectedResponseStatus, "application/json", expectedResponse.getBytes)
        }
      },
      List(expectedResponseStatus),
      "request description"
    )

    assert(response() == expectedResponse)
    assert(actualUsedEndpoints == expectedUsedEndpoints)
  }

  "tryExecuteRequest" should "throw VaultFatalException after non-RestException(java.net.ConnectException)" in {
    val expectedResponseStatus = 200
    val expectedResponse = "test"
    val endpoint = vaultRestRequestExecutorSettings.endpoints(0)

    val tryExecuteRequest = PrivateMethod[String]('tryExecuteRequest)

    val vaultRestRequestExecutor = new VaultRestRequestExecutor(vaultRestRequestExecutorSettings) {
      override val endpointQueue = getEndpointQueue(List(endpoint))

      override def tryExecuteRequest(request: String => () => RestResponse,
                                     expectedResponseStatuses: List[Int],
                                     requestDescription: String): String = {
        super.tryExecuteRequest(request, expectedResponseStatuses, requestDescription)
      }
    }

    def response(): String = vaultRestRequestExecutor invokePrivate tryExecuteRequest(
      (endpoint: String) => () => {
        new RestException("")
      },
      List(expectedResponseStatus),
      "request description"
    )

    assertThrows[VaultFatalException] {
      response()
    }
  }
}
