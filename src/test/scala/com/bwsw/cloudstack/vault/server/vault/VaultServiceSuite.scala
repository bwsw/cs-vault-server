package com.bwsw.cloudstack.vault.server.vault

import java.util.UUID

import com.bwsw.cloudstack.vault.server.BaseSuite
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 28.08.17.
  */
class VaultServiceSuite extends FlatSpec with TestData with BaseSuite {

  "createToken" should "return new token UUID" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountReadPolicy(entityId)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createTokenCreateRequest(tokenParameters: String): () => String  = {
        () => getTokenJsonResponse(token.toString)
      }

      override def createPolicyCreateRequest(policyName: String, policyJson: String): () => String = {
        assert(policyName == policy.name, "policyName is wrong")
        assert(policyJson == policy.jsonString, "policyJson is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultSettings)

    val expectedToken = vaultService.createToken(policy :: Nil)
    assert(expectedToken == token)
  }

  "revokeToken" should "revoke token and return path to secret data" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId)
    val tokenJson = getTokenJson(entityId.toString)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createTokenLookupRequest(tokenJsonString: String): () => String  = {
        assert(tokenJson == tokenJsonString, "tokenJsonString is wrong")
        () => getLookupTokenJsonResponse(policy.name, policy.path)
      }

      override def createTokenRevokeRequest(tokenJsonString: String): () => String = {
        assert(tokenJson == tokenJsonString, "tokenJsonString is wrong")
        () => ""
      }

      override def createPolicyDeleteRequest(policyName: String): () => String = {
        assert(policyName == policy.name, "policyName is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultSettings)

    val expectedPath = vaultService.revokeToken(entityId)
    assert(expectedPath == policy.path)
  }

  "deleteSecret" should "delete secret in vault by path" in {
    val path = "test/path"
    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createDeleteSecretRequest(pathToSecret: String): () => String = {
        assert(pathToSecret == path, "vaultRest is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultSettings)
    assert(vaultService.deleteSecret(path).isInstanceOf[Unit])
  }
}
