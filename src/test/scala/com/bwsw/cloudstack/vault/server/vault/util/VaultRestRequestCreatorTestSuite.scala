package com.bwsw.cloudstack.vault.server.vault.util

import com.bettercloud.vault.json.Json
import com.bettercloud.vault.rest.{Rest, RestException, RestResponse}
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.MockConfig.vaultRestRequestCreatorSettings
import com.bwsw.cloudstack.vault.server.vault.TestData
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.vault.entities.Token.TokenInitParameters
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultCriticalException
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 31.08.17.
  */
class VaultRestRequestCreatorTestSuite extends FlatSpec with TestData with BaseTestSuite {

  // Positive tests
  "createTokenCreateRequest" should "create request which return token" in {
    val policyName = "policyName"
    val period = 1000
    val expectedResponseBody = getTokenJsonResponse(token.toString).getBytes("UTF-8")
    val expectedPath = "/v1/auth/token/create"
    val expectedData = getTokenInitParametersJson(false, policyName, period)

    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            200,
            "application/json",
            expectedResponseBody
          )
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val tokenInitParameters = TokenInitParameters(false, List(policyName), period)
    val resultTokenResponse = vaultRestRequestCreator.createTokenCreateRequest(jsonSerializer.serialize(tokenInitParameters))()

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "createTokenRevokeRequest" should "create request which revoke token" in {
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = "/v1/auth/token/revoke"
    val expectedData = getTokenJson(token.toString)

    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val jsonTokenId = Json.`object`().add("token", token.toString).toString
    val resultTokenResponse = vaultRestRequestCreator.createTokenRevokeRequest(jsonTokenId)()

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "createTokenRevokeRequest" should "create request which return lookup token" in {
    val policyName = "policy"
    val pathToSecret = "path/secret"
    val expectedResponseBody = getLookupTokenJsonResponse(policyName).getBytes
    val expectedPath = "/v1/auth/token/lookup"
    val expectedData = getTokenJson(token.toString)

    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            200,
            "application/json",
            expectedResponseBody
          )
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val jsonTokenId = Json.`object`().add("token", token.toString).toString
    val resultTokenResponse = vaultRestRequestCreator.createTokenLookupRequest(jsonTokenId)()

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "createPolicyCreateRequest" should "create request which creates policy" in {
    val policy = Policy.createVmWritePolicy(accountId, vmId)
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = s"/v1/sys/policy/${policy.name}"
    val expectedData = getPolicyJson(policy)

    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def put(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val resultTokenResponse = vaultRestRequestCreator.createPolicyCreateRequest(policy.name, policy.jsonString)()

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "createPolicyDeleteRequest" should "create request which deletes policy" in {
    val policy = Policy.createVmReadPolicy(accountId, vmId)
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = s"/v1/sys/policy/${policy.name}"
    val expectedData = ""

    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def delete(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val resultTokenResponse = vaultRestRequestCreator.createPolicyDeleteRequest(policy.name)()

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  "createDeleteSecretRequest" should "create request which deletes secret" in {
    val expectedResponseBody = Array.empty[Byte]
    val expectedPath = s"/v1/secret/test/path"
    val expectedData = ""

    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
        new Rest() {
          override def delete(): RestResponse = new RestResponse(
            204,
            "application/json",
            expectedResponseBody
          )
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val resultTokenResponse = vaultRestRequestCreator.createDeleteSecretRequest(expectedPath)()

    assert(resultTokenResponse == new String(expectedResponseBody, "UTF-8"))
  }

  // Negative tests
  "createTokenCreateRequest" should "if response status is not equal to expected status, " +
    "the VaultCriticalException will thrown" in {

    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        new Rest() {
          override def post(): RestResponse = new RestResponse(
            201,
            "application/json",
            Array.empty[Byte]
          )
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val tokenInitParameters = TokenInitParameters(false, List("name"), 1000)

    assertThrows[VaultCriticalException] {
      vaultRestRequestCreator.createTokenCreateRequest(jsonSerializer.serialize(tokenInitParameters))()
    }
  }

  "createTokenRevokeRequest" should "if Rest throw not same with RestException, " +
    "the exception will wrapped to VaultCriticalException" in {
    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        new Rest() {
          override def post(): RestResponse = throw new Exception("test exception")
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val jsonTokenId = Json.`object`().add("token", token.toString).toString

    assertThrows[VaultCriticalException] {
      vaultRestRequestCreator.createTokenRevokeRequest(jsonTokenId)()
    }
  }

  "createTokenRevokeRequest" should  "if Rest throw RestException, exception does not catch" in {
    val vaultRestRequestCreator = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override protected def createRest(path: String, data: String): Rest = {
        new Rest() {
          override def post(): RestResponse = throw new RestException("test exception")
        }.url(s"${vaultRestRequestCreatorSettings.vaultUrl}$path")
          .header("X-Vault-Token", vaultRestRequestCreatorSettings.vaultRootToken)
          .body(data.getBytes("UTF-8"))
      }
    }

    val jsonTokenId = Json.`object`().add("token", token.toString).toString

    assertThrows[RestException] {
      vaultRestRequestCreator.createTokenLookupRequest(jsonTokenId)()
    }
  }
}
