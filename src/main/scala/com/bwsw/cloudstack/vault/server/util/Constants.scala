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
package com.bwsw.cloudstack.vault.server.util

object ConfigLiterals {
  private final val applicationDomain = "app"
  private final val kafkaDomain       = s"$applicationDomain.kafka"
  private final val vaultDomain       = s"$applicationDomain.vault"
  private final val zooKeeperDomain   = s"$applicationDomain.zookeeper"
  private final val cloudStackDomain  = s"$applicationDomain.cloudStack"

  val tagNamePrefix          = s"$applicationDomain.tagNamePrefix"

  val vaultEndpoint          = s"$vaultDomain.endpoint"
  val vaultRootToken         = s"$vaultDomain.rootToken"
  val vaultRetryDelay        = s"$vaultDomain.retryDelay"
  val vaultTokenPeriod       = s"$vaultDomain.tokenPeriod"
  val vaultAccountsBasicPath = s"$vaultDomain.accountsBasicPath"
  val vaultVmsBasicPath      = s"$vaultDomain.vmsBasicPath"


  val kafkaEndpoints  = s"$kafkaDomain.endpoints"
  val kafkaTopic      = s"$kafkaDomain.topic"

  val zooKeeperEndpoints       = s"$zooKeeperDomain.endpoints"
  val zooKeeperRetryDelay      = s"$zooKeeperDomain.retryDelay"
  val zooKeeperRootNode        = s"$zooKeeperDomain.rootNode"
  val zooKeeperMasterLatchNode = s"$zooKeeperDomain.masterLatchNode"

  val cloudStackEndpoints  = s"$cloudStackDomain.endpoints"
  val cloudStackApiKey     = s"$cloudStackDomain.apiKey"
  val cloudStackSecretKey  = s"$cloudStackDomain.secretKey"
  val cloudStackRetryDelay = s"$cloudStackDomain.retryDelay"
}

object HttpStatus {
  val OK_STATUS = 200
  val OK_STATUS_WITH_EMPTY_BODY = 204
  val CLOUD_STACK_ENTITY_DOES_NOT_EXIST = 431
  val NOT_FOUND = 404
}

object RequestPath {
  val vaultRoot            = "/v1/"
  val vaultHealthCheckPath = s"${vaultRoot}sys/health"
  val vaultTokenCreate     = s"${vaultRoot}auth/token/create"
  val vaultTokenLookup     = s"${vaultRoot}auth/token/lookup"
  val vaultPolicy          = s"${vaultRoot}sys/policy"
  val vaultTokenRevoke     = s"${vaultRoot}auth/token/revoke"
}

object DataPath {
  val masterLatchNode = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperMasterLatchNode)
}
