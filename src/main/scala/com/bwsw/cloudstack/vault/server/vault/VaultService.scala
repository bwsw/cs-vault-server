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
import com.bwsw.cloudstack.vault.server.common.{Converter, JsonSerializer}
import com.bwsw.cloudstack.vault.server.util._
import com.bwsw.cloudstack.vault.server.vault.entities._
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultFatalException
import org.slf4j.LoggerFactory

/**
  * Class is responsible for interaction with Vault server with help of VaultRestRequestCreator
  *
  * @param vaultRest provides interaction with Vault server
  *
  * @param settings contains settings for interaction with Vault
  */
class VaultService(vaultRest: VaultRestRequestCreator,
                   settings: VaultService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val jsonSerializer = new JsonSerializer(ignoreUnknownProperties = true)
  val endpoint: String = vaultRest.endpoint

  /**
    * Creates token with specified policy
    *
    * @param policies policies for token
    * @return token id
    * @throws VaultFatalException if response status is not expected.
    */
  def createToken(policies: List[Policy]): UUID = {
    logger.debug(s"createToken with policies: $policies")
    policies.foreach(writePolicy)

    val tokenParameters = Token.TokenInitParameters(
      noDefaultPolicy = true,
      policies.map(_.name),
      Converter.daysToSeconds(settings.tokenPeriod)
    )

    def executeRequest = vaultRest.createTokenCreateRequest(jsonSerializer.serialize(tokenParameters))

    val responseString = TaskRunner.tryRunUntilSuccess[String](
      executeRequest,
      settings.retryDelay
    )

    val token = jsonSerializer.deserialize[Token](responseString).tokenId.id
    logger.debug(s"Token: $token is created")
    token
  }

  /**
    * Revokes token from Vault server.
    *
    * @param tokenId token id to revoke
    * @return List of token policies names
    * @throws VaultFatalException if response status is not expected.
    */
  def revokeToken(tokenId: UUID): List[String] = {
    logger.debug(s"revokeToken")
    val jsonTokenId = Json.`object`().add("token", tokenId.toString).toString

    def executeLookupRequest = vaultRest.createTokenLookupRequest(jsonTokenId)

    val lookupResponseString = TaskRunner.tryRunUntilSuccess[String](
      executeLookupRequest,
      settings.retryDelay
    )

    val lookupToken = jsonSerializer.deserialize[LookupToken](lookupResponseString)

    def executeRevokeRequest = vaultRest.createTokenRevokeRequest(jsonTokenId)

    val revokeResponseString = TaskRunner.tryRunUntilSuccess[String](
      executeRevokeRequest,
      settings.retryDelay
    )
    logger.debug(s"Token: $tokenId is revoked")

    lookupToken.tokenData.policies.filter { x =>
      x != "default" && x != "root"
    }
  }

  /**
    * Deletes secrets from Vault server by specified path.
    *
    * @param pathToRootSecret path for deletion of root secret, tree of sub-secrets will be deleted too
    * @throws VaultFatalException if response status is not expected.
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
        settings.retryDelay
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
      TaskRunner.tryRunUntilSuccess[String](vaultRest.createDeleteSecretRequest(x), settings.retryDelay)
      logger.debug(s"Data from path: $x is deleted")
    }
  }

  /**
    * deletes policy in Vault server
    *
    * @param policyName policyName for deletion
    * @throws VaultFatalException if response status is not expected.
    */
  def deletePolicy(policyName: String): Unit = {
    logger.debug(s"deletePolicy: $policyName")

    def executeRequest = vaultRest.createPolicyDeleteRequest(policyName)

    TaskRunner.tryRunUntilSuccess[String](
      executeRequest,
      settings.retryDelay
    )

    logger.debug(s"Policy with name: $policyName is deleted")
  }

  /**
    * Creates policy in Vault server
    *
    * @param policy policy for creating
    * @throws VaultFatalException if response status is not expected.
    */
  private def writePolicy(policy: Policy) = {
    logger.debug(s"writePolicy: $policy")

    def executeRequest = vaultRest.createPolicyCreateRequest(policy.name, policy.jsonString)

    TaskRunner.tryRunUntilSuccess[String](
      executeRequest,
      settings.retryDelay
    )
    logger.debug(s"Policy: $policy is created")
  }
}

object VaultService {
  case class Settings(tokenPeriod: Int, retryDelay: Int)
}
