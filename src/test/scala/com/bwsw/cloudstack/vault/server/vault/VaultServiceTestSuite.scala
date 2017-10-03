package com.bwsw.cloudstack.vault.server.vault

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig._
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultCriticalException
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 28.08.17.
  */
class VaultServiceTestSuite extends FlatSpec with TestData with BaseTestSuite {
  val accountSecretPath = settings.cloudStackVaultControllerSettings.accountSecretPath
  val vmSecretPath = settings.cloudStackVaultControllerSettings.vmSecretPath

  //Positive tests
  "createToken" should "return new token UUID" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountReadPolicy(entityId, accountSecretPath)

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

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    val expectedToken = vaultService.createToken(policy :: Nil)
    assert(expectedToken == token)
  }

  "revokeToken" should "revoke a token" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId, accountSecretPath)
    val tokenJson = getTokenJson(entityId.toString)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createTokenLookupRequest(tokenJsonString: String): () => String  = {
        assert(tokenJson == tokenJsonString, "tokenJsonString is wrong")
        () => getLookupTokenJsonResponse(policy.name)
      }

      override def createTokenRevokeRequest(tokenJsonString: String): () => String = {
        assert(tokenJson == tokenJsonString, "tokenJsonString is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assert(vaultService.revokeToken(entityId) == List(policy.name))
  }

  "deletePolicy" should "delete policy" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId, accountSecretPath)
    val tokenJson = getTokenJson(entityId.toString)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createPolicyDeleteRequest(policyName: String): () => String = {
        assert(policyName == policy.name, "policyName is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assert(vaultService.deletePolicy(policy.name).isInstanceOf[Unit])
  }

  "deleteSecret" should "delete secret in vault by path" in {
    val path = "test/path"
    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createDeleteSecretRequest(pathToSecret: String): () => String = {
        assert(pathToSecret == path, "vaultRest is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)
    assert(vaultService.deleteSecret(path).isInstanceOf[Unit])
  }

  //Negative tests
  "createToken" should "The VaultCriticalException thrown by VaultRestRequestCreator must not be swallowed" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountReadPolicy(entityId, accountSecretPath)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createTokenCreateRequest(tokenParameters: String): () => String  = {
        throw new VaultCriticalException(new Exception("test exception"))
      }

      override def createPolicyCreateRequest(policyName: String, policyJson: String): () => String = {
        assert(policyName == policy.name, "policyName is wrong")
        assert(policyJson == policy.jsonString, "policyJson is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultCriticalException] {
      vaultService.createToken(policy :: Nil)
    }
  }

  "revokeToken" should "The VaultCriticalException thrown by VaultRestRequestCreator must not be swallowed" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId, accountSecretPath)
    val tokenJson = getTokenJson(entityId.toString)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createTokenLookupRequest(tokenJsonString: String): () => String  = {
        assert(tokenJson == tokenJsonString, "tokenJsonString is wrong")
        throw new VaultCriticalException(new Exception("test exception"))
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultCriticalException] {
      vaultService.revokeToken(entityId)
    }
  }

  "deleteSecret" should "The VaultCriticalException thrown by VaultRestRequestCreator must not be swallowed" in {
    val path = "test/path"
    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createDeleteSecretRequest(pathToSecret: String): () => String = {
        assert(pathToSecret == path, "vaultRest is wrong")
        throw new VaultCriticalException(new Exception("test exception"))
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultCriticalException] {
      vaultService.deleteSecret(path)
    }
  }

  "deletePolicy" should "The VaultCriticalException thrown by VaultRestRequestCreator must not be swallowed" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId, accountSecretPath)
    val tokenJson = getTokenJson(entityId.toString)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createPolicyDeleteRequest(policyName: String): () => String = {
        assert(policyName == policy.name, "policyName is wrong")
        throw new VaultCriticalException(new Exception("test exception"))
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultCriticalException] {
      vaultService.deletePolicy(policy.name)
    }
  }
}
