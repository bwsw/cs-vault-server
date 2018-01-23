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
import com.bwsw.cloudstack.vault.server.common.WeightedQueue
import com.bwsw.cloudstack.vault.server.util.{HttpStatus, RequestPath}
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultFatalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for tasks creation for interaction with Vault server with help of Vault library
  *
  * @param settings contains settings for interaction with Vault
  */
class VaultRestRequestExecutor(settings: VaultRestRequestExecutor.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private[vault] val endpoints: Array[String] = settings.endpoints
  protected val endpointQueue = new WeightedQueue[String](settings.endpoints.toList)

  /**
    * Creates and executes request for token creation with specified parameters
    *
    * @param tokenParameters parameters for new token
    * @return task for token creation
    * @throws VaultFatalException if response status is not expected.
    */
  def executeTokenCreateRequest(tokenParameters: String): String = {
    tryExecuteRequest(
      createRest(s"${RequestPath.vaultTokenCreate}", tokenParameters)(_).post,
      HttpStatus.OK_STATUS :: Nil,
      "create token"
    )
  }

  /**
    * Creates and executes request for revocation of token with specified id
    *
    * @param jsonTokenId id for token revocation
    * @return task for token revocation
    * @throws VaultFatalException if response status is not expected.
    */
  def executeTokenRevokeRequest(jsonTokenId: String): String = {
    tryExecuteRequest(
      createRest(s"${RequestPath.vaultTokenRevoke}", jsonTokenId)(_).post,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "revoke token"
    )
  }

  /**
    * Creates and executes request for policy creation
    *
    * @param policyName name of new Policy
    * @param policyJson json string with parameters of new Policy
    * @return task for Policy creation
    * @throws VaultFatalException if response status is not expected.
    */
  def executePolicyCreateRequest(policyName: String, policyJson: String): String = {
    tryExecuteRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", policyJson)(_).put,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "write policy"
    )
  }

  /**
    * Creates and executes request for policy deletion
    *
    * @param policyName name of Policy for deletion
    * @return task for Policy deletion
    * @throws VaultFatalException if response status is not expected.
    */
  def executePolicyDeleteRequest(policyName: String): String = {
    tryExecuteRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", "")(_).delete,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "delete policy"
    )
  }

  /**
    * Creates and executes request to retrieve lookup token
    *
    * @param jsonTokenId json string with token id
    * @return task to retrieve lookupToken
    * @throws VaultFatalException if response status is not expected.
    */
  def executeTokenLookupRequest(jsonTokenId: String): String = {
    tryExecuteRequest(
      createRest(s"${RequestPath.vaultTokenLookup}", jsonTokenId)(_).post,
      HttpStatus.OK_STATUS :: Nil,
      "get lookup token"
    )
  }

  /**
    * Creates and executes request for deletion of secret by specified path
    *
    * @param pathToSecret path to secret
    * @return task for deletion of secret
    * @throws VaultFatalException if response status is not expected.
    */
  def executeDeleteSecretRequest(pathToSecret: String): String = {
    tryExecuteRequest(
      createRest(s"$pathToSecret", "")(_).delete,
      HttpStatus.OK_STATUS_WITH_EMPTY_BODY :: Nil,
      "delete secret"
    )
  }

  /**
    * Creates and executes request to retrieve secret by specified path
    *
    * @param pathToRootSecret path to root secret
    * @return task to retrieve json string of sub-paths of secrets
    * @throws VaultFatalException if response status is not expected.
    */
  def executeGetSubSecretPathsRequest(pathToRootSecret: String): String = {
    tryExecuteRequest(
      createRest(s"$pathToRootSecret?list=true", "")(_).get,
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
  protected def createRest(path: String, data: String)(endpoint: String): Rest = {
    val endpoint = "test"
    new Rest()
      .url(s"$endpoint$path")
      .header("X-Vault-Token", settings.rootToken)
      .body(data.getBytes("UTF-8"))
  }

  /**
    * Handles request execution
    */
  protected def tryExecuteRequest(request: (String) => () => RestResponse,
                                  expectedResponseStatuses: List[Int],
                                  requestDescription: String): String = {
    logger.trace(s"tryExecuteRequest(request: $request, expectedResponseStatuses: $expectedResponseStatuses, " +
      s"requestDescription: $requestDescription)")
    val currentEndpoint = endpointQueue.getElement
    Try {
      request(currentEndpoint)()
    } match {
      case Success(response) => response
        if (!expectedResponseStatuses.contains(response.getStatus)) {
          throw new VaultFatalException(s"Response status: ${response.getStatus} from Vault server is not expected")
        }
        new String(response.getBody)

      case Failure(e: RestException) if e.getCause.isInstanceOf[java.net.ConnectException] =>
        logger.warn(s"Vault server is unavailable by endpoint: $currentEndpoint, " +
          s"retry request execution after ${settings.retryDelay} seconds")
        endpointQueue.moveElementToEnd(currentEndpoint)
        Thread.sleep(settings.retryDelay)
        tryExecuteRequest(request, expectedResponseStatuses, requestDescription)

      case Failure(e: Throwable) =>
        logger.error(s"Request to Vault server does not complete successfully, exception occurred: $e")
        throw new VaultFatalException(e.toString)
    }
  }
}

object VaultRestRequestExecutor {
  case class Settings(endpoints: Array[String], rootToken: String, retryDelay: Int)
}
