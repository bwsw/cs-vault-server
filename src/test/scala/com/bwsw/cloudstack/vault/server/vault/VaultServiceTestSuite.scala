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

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig._
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultFatalException
import org.scalatest.FlatSpec

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
  "createToken" should "not swallow VaultFatalException thrown by VaultRestRequestCreator" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountReadPolicy(entityId, accountSecretPath)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createTokenCreateRequest(tokenParameters: String): () => String  = {
        throw new VaultFatalException("test exception")
      }

      override def createPolicyCreateRequest(policyName: String, policyJson: String): () => String = {
        assert(policyName == policy.name, "policyName is wrong")
        assert(policyJson == policy.jsonString, "policyJson is wrong")
        () => ""
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultFatalException] {
      vaultService.createToken(policy :: Nil)
    }
  }

  "revokeToken" should "not swallow VaultFatalException thrown by VaultRestRequestCreator" in {
    val entityId = UUID.randomUUID()
    val tokenJson = getTokenJson(entityId.toString)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createTokenLookupRequest(tokenJsonString: String): () => String  = {
        assert(tokenJson == tokenJsonString, "tokenJsonString is wrong")
        throw new VaultFatalException("test exception")
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultFatalException] {
      vaultService.revokeToken(entityId)
    }
  }

  "deleteSecretRecursive" should "not swallow VaultFatalException thrown by VaultRestRequestCreator" in {
    val path = "/test/path"
    val responseWithEmptySubTree = "{\"errors\":[]}"

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createDeleteSecretRequest(pathToSecret: String): () => String = {
        assert(pathToSecret == path, "vaultRest is wrong")
        throw new VaultFatalException("test exception")
      }

      override def createGetSubSecretPathsRequest(pathToRootSecret: String): () => String = {
        () => responseWithEmptySubTree
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultFatalException] {
      vaultService.deleteSecretsRecursively(path)
    }
  }

  "deletePolicy" should "not swallow VaultFatalException thrown by VaultRestRequestCreator" in {
    val entityId = UUID.randomUUID()
    val policy = Policy.createAccountWritePolicy(entityId, accountSecretPath)

    val vaultRest = new VaultRestRequestCreator(vaultRestRequestCreatorSettings) {
      override def createPolicyDeleteRequest(policyName: String): () => String = {
        assert(policyName == policy.name, "policyName is wrong")
        throw new VaultFatalException("test exception")
      }
    }

    val vaultService = new VaultService(vaultRest, vaultServiceSettings)

    assertThrows[VaultFatalException] {
      vaultService.deletePolicy(policy.name)
    }
  }
}
