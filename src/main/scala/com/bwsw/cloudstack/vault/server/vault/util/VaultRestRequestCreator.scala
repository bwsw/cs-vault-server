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

import com.bettercloud.vault.rest.{Rest, RestException, RestResponse}
import com.bwsw.cloudstack.vault.server.util.{HttpStatus, RequestPath}
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultFatalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for tasks creation for interaction with Vault server with help of Vault library
  *
  * @param settings contains settings for interaction with Vault
  */
class VaultRestRequestCreator(settings: VaultRestRequestCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private[vault] val endpoint: String = settings.endpoint

  /**
    * Creates request for token creation with specified parameters
    *
    * @param tokenParameters parameters for new token
    * @return task for token creation
    * @throws VaultFatalException if response status is not expected.
    */
  def createTokenCreateRequest(tokenParameters: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenCreate}", tokenParameters).post,
      HttpStatus.OK_STATUS :: Nil,
      "create token"
    )
  }

  /**
    * Creates request for revocation of token with specified id
    *
    * @param jsonTokenId id for token revocation
    * @return task for token revocation
    * @throws VaultFatalException if response status is not expected.
    */
  def createTokenRevokeRequest(jsonTokenId: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenRevoke}", jsonTokenId).post,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "revoke token"
    )
  }

  /**
    * Creates request for policy creation
    *
    * @param policyName name of new Policy
    * @param policyJson json string with parameters of new Policy
    * @return task for Policy creation
    * @throws VaultFatalException if response status is not expected.
    */
  def createPolicyCreateRequest(policyName: String, policyJson: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", policyJson).put,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "write policy"
    )
  }

  /**
    * Creates request for policy deletion
    *
    * @param policyName name of Policy for deletion
    * @return task for Policy deletion
    * @throws VaultFatalException if response status is not expected.
    */
  def createPolicyDeleteRequest(policyName: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", "").delete,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "delete policy"
    )
  }

  /**
    * Creates request to retrieve lookup token
    *
    * @param jsonTokenId json string with token id
    * @return task to retrieve lookupToken
    * @throws VaultFatalException if response status is not expected.
    */
  def createTokenLookupRequest(jsonTokenId: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenLookup}", jsonTokenId).post,
      HttpStatus.OK_STATUS :: Nil,
      "get lookup token"
    )
  }

  /**
    * Creates request for deletion of secret by specified path
    *
    * @param pathToSecret path to secret
    * @return task for deletion of secret
    * @throws VaultFatalException if response status is not expected.
    */
  def createDeleteSecretRequest(pathToSecret: String):() => String = {
    createRequest(
      createRest(s"$pathToSecret", "").delete,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "delete secret"
    )
  }

  /**
    * Creates request to retrieve secret by specified path
    *
    * @param pathToRootSecret path to root secret
    * @return task to retrieve json string of sub-paths of secrets
    * @throws VaultFatalException if response status is not expected.
    */
  def createGetSubSecretPathsRequest(pathToRootSecret: String):() => String = {
    createRequest(
      createRest(s"$pathToRootSecret?list=true", "").get,
      List(HttpStatus.OK_STATUS, HttpStatus.NOT_FOUND),
      "getSubPaths"
    )
  }

  /**
    * Creates Rest object
    *
    * @param path specified url path
    * @param data specified data for request body
    * @return Rest object
    * @throws VaultFatalException if response status is not expected.
    */
  protected def createRest(path: String, data: String): Rest = {
    new Rest()
      .url(s"${settings.endpoint}$path")
      .header("X-Vault-Token", settings.rootToken)
      .body(data.getBytes("UTF-8"))
  }

  /**
    * Handles request
    */
  private def createRequest(request: () => RestResponse,
                            expectedResponseStatuses: List[Int],
                            requestDescription: String)(): String = {
    logger.debug(s"Request is executed : $requestDescription")
    val response = Try {
      request()
    } match {
      case Success(x) => x
      case Failure(e: RestException) =>
        logger.warn("Vault server is unavailable")
        throw e
      case Failure(e: Throwable) =>
        logger.error(s"Request to vault server does not finish correctly, exception was thrown: $e")
        throw new VaultFatalException(e.toString)
    }

    if (!expectedResponseStatuses.contains(response.getStatus)) {
      throw new VaultFatalException(s"Response status: ${response.getStatus} from vault server is not expected")
    }
    new String(response.getBody)
  }
}

object VaultRestRequestCreator {
  case class Settings(endpoint: String, rootToken: String)
}
