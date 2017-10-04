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

import com.bettercloud.vault.VaultException
import com.bettercloud.vault.rest.{Rest, RestException, RestResponse}
import com.bwsw.cloudstack.vault.server.util.{HttpStatuses, RequestPath}
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultCriticalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for creating tasks for interaction with Vault server with help of Vault library
  *
  * @param settings contains the settings for interaction with Vault
  */
class VaultRestRequestCreator(settings: VaultRestRequestCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private[vault] val vaultUrl: String = settings.vaultUrl

  /**
    * Creates request for creating token with specified parameters
    *
    * @param tokenParameters parameters for new token
    *
    * @return task for creating token
    * @throws VaultCriticalException if response status is not expected.
    */
  def createTokenCreateRequest(tokenParameters: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenCreate}", tokenParameters).post,
      HttpStatuses.OK_STATUS,
      "create token"
    )
  }

  /**
    * Creates request for revoking token with specified id
    *
    * @param jsonTokenId id for token revoking
    *
    * @return task for revoking token
    * @throws VaultCriticalException if response status is not expected.
    */
  def createTokenRevokeRequest(jsonTokenId: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenRevoke}", jsonTokenId).post,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "revoke token"
    )
  }

  /**
    * Creates request for creating policy
    *
    * @param policyName name of new Policy
    * @param policyJson json string with parameters of new Policy
    *
    * @return task for creating Policy
    * @throws VaultCriticalException if response status is not expected.
    */
  def createPolicyCreateRequest(policyName: String, policyJson: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", policyJson).put,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "write policy"
    )
  }

  /**
    * Creates request for deletion policy
    *
    * @param policyName name of Policy for deletion
    *
    * @return task for deletion Policy
    * @throws VaultCriticalException if response status is not expected.
    */
  def createPolicyDeleteRequest(policyName: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", "").delete,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "delete policy"
    )
  }

  /**
    * Creates request for getting lookup token
    *
    * @param jsonTokenId json string with token id
    *
    * @return task for getting lookupToken
    * @throws VaultCriticalException if response status is not expected.
    */
  def createTokenLookupRequest(jsonTokenId: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenLookup}", jsonTokenId).post,
      HttpStatuses.OK_STATUS,
      "get lookup token"
    )
  }

  /**
    * Creates request for deletion secret by specified path
    *
    * @param pathToSecret path to secret
    *
    * @return task for deletion secret
    * @throws VaultCriticalException if response status is not expected.
    */
  def createDeleteSecretRequest(pathToSecret: String):() => String = {
    createRequest(
      createRest(s"$pathToSecret", "").delete,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "delete secret"
    )
  }

  /**
    * Creates request for getting secret by specified path
    *
    * @param pathToRootSecret path to root secret
    *
    * @return task for getting json string of sub-paths of secrets
    * @throws VaultCriticalException if response status is not expected.
    */
  def createGetSubSecretPathsRequest(pathToRootSecret: String):() => String = {
    createRequest(
      createRest(s"$pathToRootSecret?list=true", "").get,
      HttpStatuses.OK_STATUS,
      "getSubPaths"
    )
  }

  /**
    * Creates Rest object
    *
    * @param path specified url path
    * @param data specified data for request body
    *
    * @return Rest object
    * @throws VaultCriticalException if response status is not expected.
    */
  protected def createRest(path: String, data: String): Rest = {
    new Rest()
      .url(s"${settings.vaultUrl}$path")
      .header("X-Vault-Token", settings.vaultRootToken)
      .body(data.getBytes("UTF-8"))
  }

  /**
    * Handles request execution
    */
  private def createRequest(request: () => RestResponse,
                            expectedResponseStatus: Int,
                            requestDescription: String)(): String = {
    logger.debug(s"Request was executed for: $requestDescription")
    val response = Try {
      request()
    } match {
      case Success(x) => x
      case Failure(e: RestException) =>
        logger.warn("Vault server is unavailable")
        throw e
      case Failure(e: Throwable) =>
        logger.error(s"Request to vault server is not finish correctly, exception was thrown: $e")
        throw new VaultCriticalException(e)
    }

    if (response.getStatus != expectedResponseStatus) {
      throw new VaultCriticalException(new VaultException(s"Response status: ${response.getStatus} from vault server is not expected"))
    }
    new String(response.getBody)
  }
}

object VaultRestRequestCreator {
  case class Settings(vaultUrl: String, vaultRootToken: String)
}
