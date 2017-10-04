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

  //Positive tests
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

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    val expectedToken = vaultService.createToken(policy :: Nil)
    assert(expectedToken == token)
  }

  "revokeToken" should "revoke a token" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId)
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
    val policy = Policy.createAccountWritePolicy(entityId)
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

  "deleteSecretRecursive" should "delete secret in vault by path and sub-paths" in {
    val firstRootPath = "/test!1"
    val secondRootPath = "test!!2/"
    val fullSecondRootPath = s"$firstRootPath/${secondRootPath.dropRight(1)}"
    val thirdRootPath = "test!!!3/"
    val secondPath = "test!!2"
    val thirdPath = "test!!!3"
    val fourthPath = "test!!!!4"

    val expectedGetPaths = List(
      s"$firstRootPath",
      s"$firstRootPath/$secondPath",
      s"$firstRootPath/$secondRootPath$thirdPath"
    )
    var actualGetPaths = List.empty[String]

    val expectedDeletionPaths = List(
      s"$firstRootPath/$secondRootPath$thirdRootPath$fourthPath",
      s"$firstRootPath/$secondRootPath$thirdRootPath$thirdPath",
      s"$firstRootPath/$secondRootPath$fourthPath",
      s"$firstRootPath/$secondPath",
      s"$firstRootPath"
    )
    var actualDeletionPaths = List.empty[String]

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createDeleteSecretRequest(pathToSecret: String): () => String = {
        actualDeletionPaths = actualDeletionPaths ::: pathToSecret :: Nil
        () => ""
      }

      override def createGetSubSecretPathsRequest(pathToRootSecret: String): () => String = {
        pathToRootSecret match {
          case `firstRootPath` =>
            actualGetPaths = actualGetPaths ::: pathToRootSecret :: Nil
            () => getSubSecretPathsJson(secondRootPath, secondPath)
          case `fullSecondRootPath` =>
            actualGetPaths = actualGetPaths ::: pathToRootSecret :: Nil
            () => getSubSecretPathsJson(fourthPath, thirdRootPath)
          case _ =>
            actualGetPaths = actualGetPaths ::: pathToRootSecret :: Nil
            () => getSubSecretPathsJson(thirdPath, fourthPath)
        }
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)
    assert(vaultService.deleteSecretsRecursively(firstRootPath).isInstanceOf[Unit])
    assert(actualDeletionPaths == expectedDeletionPaths)
    assert(actualGetPaths == expectedGetPaths)
  }

  "deleteSecretRecursive" should "delete secret in vault by path if sub-path does not exist" in {
    val firstPath = "test1"
    val responseWithEmptySubThree = "{\"errors\":[]}"

    val expectedGetPaths = List(s"$firstPath")
    var actualGetPaths = List.empty[String]

    val expectedDeletionPaths = List(s"$firstPath")
    var actualDeletionPaths = List.empty[String]

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createDeleteSecretRequest(pathToSecret: String): () => String = {
        actualDeletionPaths = actualDeletionPaths ::: pathToSecret :: Nil
        () => ""
      }

      override def createGetSubSecretPathsRequest(pathToRootSecret: String): () => String = {
        actualGetPaths = actualGetPaths ::: pathToRootSecret :: Nil
        () => responseWithEmptySubThree
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)
    assert(vaultService.deleteSecretsRecursively(firstPath).isInstanceOf[Unit])
    assert(actualDeletionPaths == expectedDeletionPaths)
    assert(actualGetPaths == expectedGetPaths)
  }

  //Negative tests
  "createToken" should "The VaultCriticalException thrown by VaultRestRequestCreator must not be swallowed" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountReadPolicy(entityId)

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
    val policy = Policy.createAccountWritePolicy(entityId)
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

  "deleteSecretRecursive" should "The VaultCriticalException thrown by VaultRestRequestCreator must not be swallowed" in {
    val path = "/test/path"
    val responseWithEmptySubTree = "{\"errors\":[]}"

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createDeleteSecretRequest(pathToSecret: String): () => String = {
        assert(pathToSecret == path, "vaultRest is wrong")
        throw new VaultCriticalException(new Exception("test exception"))
      }

      override def createGetSubSecretPathsRequest(pathToRootSecret: String): () => String = {
        () => responseWithEmptySubTree
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultCriticalException] {
      vaultService.deleteSecretsRecursively(path)
    }
  }

  "deletePolicy" should "The VaultCriticalException thrown by VaultRestRequestCreator must not be swallowed" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId)
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
