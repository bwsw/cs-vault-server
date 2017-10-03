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
import java.util.regex.Pattern

import com.bettercloud.vault.json.Json
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util._
import com.bwsw.cloudstack.vault.server.vault.entities._
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultCriticalException
import org.slf4j.LoggerFactory

/**
  * Class is responsible for interaction with Vault server with help of VaultRestRequestCreator
  *
  * @param vaultRest allows for creating task for interaction with Vault
  * @param settings contains the settings for interaction with Vault
  */
class VaultService(vaultRest: VaultRestRequestCreator,
                   settings: VaultService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsonSerializer = new JsonSerializer(true)

  /**
    * Creates token with specified policy
    *
    * @param policies policies for token
    *
    * @return UUID of token
    * @throws VaultCriticalException if response status is not expected.
    */
  def createToken(policies: List[Policy]): UUID = {
    logger.debug(s"createToken with policies: $policies")
    policies.foreach(writePolicy)

    val tokenParameters = Token.TokenInitParameters(
      noDefaultPolicy = true,
      policies.map(_.name),
      settings.tokenPeriod
    )

    def executeRequest = vaultRest.createTokenCreateRequest(jsonSerializer.serialize(tokenParameters))

    val responseString = TaskRunner.tryRunUntilSuccess[String](
      executeRequest,
      settings.vaultRetryDelay
    )

    val token = jsonSerializer.deserialize[Token](responseString)
    logger.debug("Token was created")
    token.tokenId.id
  }

  /**
    * Revokes token from vault server.
    *
    * @param tokenId UUID of token for revoke
    *
    * @return List of names of revoked token policies
    * @throws VaultCriticalException if response status is not expected.
    */
  def revokeToken(tokenId: UUID): List[String] = {
    logger.debug(s"revokeToken")
    val jsonTokenId = Json.`object`().add("token", tokenId.toString).toString

    def executeLookupRequest = vaultRest.createTokenLookupRequest(jsonTokenId)

    val lookupResponseString = TaskRunner.tryRunUntilSuccess[String](
      executeLookupRequest,
      settings.vaultRetryDelay
    )

    val lookupToken = jsonSerializer.deserialize[LookupToken](lookupResponseString)

    def executeRevokeRequest = vaultRest.createTokenRevokeRequest(jsonTokenId)

    val revokeResponseString = TaskRunner.tryRunUntilSuccess[String](
      executeRevokeRequest,
      settings.vaultRetryDelay
    )
    logger.debug(s"Token was revoked")

    lookupToken.tokenData.policies.filter { x =>
      x != "default" && x != "root"
    }
  }

  /**
    * Deletes secret from vault server by specified path.
    *
    * @param pathToRootSecret path to root secret for deletion, tree of sub-secret will be deleted too
    *
    * @throws VaultCriticalException if response status is not expected.
    */
  def deleteSecretsRecursively(pathToRootSecret: String): Unit = {
    logger.debug(s"deleteSecretsRecursively: $pathToRootSecret")
    val stringPattern = Pattern.compile(".+/")
    var pathsForDeletion = List(pathToRootSecret)

    def loop(pathToSecret: String, pathsWithSubPaths: List[String]): Unit = {
      if (pathsWithSubPaths.nonEmpty) {
        getPathListPair(s"$pathToSecret/${pathsWithSubPaths.head.dropRight(1)}") match {
          case (newPathsWithSubPaths, pathsWithData) =>
            pathsForDeletion = pathsForDeletion ::: pathsWithData.map { path =>
              s"$pathToSecret/${pathsWithSubPaths.head}$path"
            }
            if (newPathsWithSubPaths.nonEmpty) {
              loop(s"$pathToSecret/${pathsWithSubPaths.head.substring(0, pathsWithSubPaths.head.length - 1)}", newPathsWithSubPaths)
            }
          case _ => List.empty[String]
        }
        loop(pathToSecret, pathsWithSubPaths.tail)
      }
    }

    def getPathListPair(pathToSecret: String): (List[String], List[String]) = {
      jsonSerializer.deserialize[SecretResponse](TaskRunner.tryRunUntilSuccess[String](
        vaultRest.createGetSubSecretPathsRequest(pathToSecret),
        settings.vaultRetryDelay
      )).secretList.getOrElse(SecretList(List.empty[String])).secrets.partition { x =>
        stringPattern.matcher(x).matches()
      }
    }

    val subPathsOfRootPath = {
      getPathListPair(pathToRootSecret) match {
        case (pathsWithSubPaths, pathsWithData) =>
          pathsForDeletion = pathsForDeletion ::: pathsWithData.map { path =>
            s"$pathToRootSecret/$path"
          }
          pathsWithSubPaths
        case _ => List.empty[String]
      }
    }
    loop(pathToRootSecret, subPathsOfRootPath)
    pathsForDeletion.reverse.foreach { x =>
      TaskRunner.tryRunUntilSuccess[String](vaultRest.createDeleteSecretRequest(x), settings.vaultRetryDelay)
      logger.debug(s"data from path: $x was deleted")
    }
  }

  /**
    * deletes policy in Vault server
    *
    * @param policyName policyNeme for deletion
    *
    * @throws VaultCriticalException if response status is not expected.
    */
  def deletePolicy(policyName: String): Unit = {
    logger.debug(s"deletePolicy: $policyName")

    def executeRequest = vaultRest.createPolicyDeleteRequest(policyName)

    TaskRunner.tryRunUntilSuccess[String](
      executeRequest,
      settings.vaultRetryDelay
    )

    logger.debug(s"policy with name: $policyName was deleted")
  }

  /**
    * Creates policy in Vault server
    *
    * @param policy policy for creating
    *
    * @throws VaultCriticalException if response status is not expected.
    */
  private def writePolicy(policy: Policy) = {
    logger.debug(s"writePolicy: $policy")

    def executeRequest = vaultRest.createPolicyCreateRequest(policy.name, policy.jsonString)

    TaskRunner.tryRunUntilSuccess[String](
      executeRequest,
      settings.vaultRetryDelay
    )
    logger.debug(s"policy was writed: $policy")
  }
}

object VaultService {
  case class Settings(tokenPeriod: Int, vaultRetryDelay: Int)
}
