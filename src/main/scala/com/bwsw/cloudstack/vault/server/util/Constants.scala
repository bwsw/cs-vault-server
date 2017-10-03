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

  val tokenPeriod            = s"$applicationDomain.tokenPeriod"
  val accountsVaultBasicPath = s"$applicationDomain.accountsVaultBasicPath"
  val vmsVaultBasicPath      = s"$applicationDomain.vmsVaultBasicPath"
  val tagNamePrefix          = s"$applicationDomain.tagNamePrefix"

  val kafkaServerList = s"$kafkaDomain.serverList"
  val kafkaTopic      = s"$kafkaDomain.topic"

  val zooKeeperUrl             = s"$zooKeeperDomain.url"
  val zooKeeperRetryDelay      = s"$zooKeeperDomain.retryDelay"
  val zooKeeperRootNode        = s"$zooKeeperDomain.zooKeeperRootNode"
  val zooKeeperMasterLatchNode = s"$zooKeeperDomain.masterLatchNode"

  val vaultUrl        = s"$vaultDomain.url"
  val vaultRootToken  = s"$vaultDomain.rootToken"
  val vaultRetryDelay = s"$vaultDomain.retryDelay"

  val cloudStackApiUrlList = s"$cloudStackDomain.apiUrlList"
  val cloudStackApiKey     = s"$cloudStackDomain.apiKey"
  val cloudStackSecretKey  = s"$cloudStackDomain.secretKey"
  val cloudStackRetryDelay = s"$cloudStackDomain.retryDelay"
}

object HttpStatuses {
  val OK_STATUS = 200
  val OK_STATUS_WITH_EMPTY_BODY = 204
  val CLOUD_STACK_ENTITY_DOES_NOT_EXIST = 431
}

object RequestPath {
  val vaultHealthCheckPath = "/v1/sys/health"
  val vaultTokenCreate     = "/v1/auth/token/create"
  val vaultTokenLookup     = "/v1/auth/token/lookup"
  val vaultPolicy          = "/v1/sys/policy"
  val vaultSecretVm        = s"/v1/${DataPath.vmSecretDefaultPath}"
  val vaultSecretAccount   = s"/v1/${DataPath.accountSecretDefaultPath}"
  val vaultTokenRevoke     = "/v1/auth/token/revoke"
}

object DataPath {
  private val accountSecret           = ApplicationConfig.getRequiredString(ConfigLiterals.accountsVaultBasicPath)
  private val vmSecret                = ApplicationConfig.getRequiredString(ConfigLiterals.vmsVaultBasicPath)
  private val configZooKeeperRootNode = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperRootNode)
  private val configMasterLatchNode   = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperMasterLatchNode)
  val vmSecretDefaultPath: String       = vmSecret
  val accountSecretDefaultPath: String  = accountSecret
  val zooKeeperRootNode: String         = configZooKeeperRootNode
  val masterLatchNode: String           = configMasterLatchNode

}

object URL {
  private val configVaultUrl          = ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)
  val vaultUrl: String                = configVaultUrl
}
