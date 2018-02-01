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

object IntegrationTestsSettings {
  //ZooKeeper
  val zooKeeperRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.zooKeeperRetryDelay)
  val zooKeeperEndpoints = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperEndpoints)
  //Vault
  val vaultTokenPeriod = ApplicationConfig.getRequiredInt(ConfigLiterals.vaultTokenPeriod)
  val vaultRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
  val vaultRootToken = ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken)
  val vaultEndpoints = ApplicationConfig.getRequiredString(ConfigLiterals.vaultEndpoints).split("[,\\s]+")
  //CloudStack
  val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
  val cloudStackEndpoints = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackEndpoints).split("[,\\s]+")
  val cloudStackLogin = ApplicationConfig.getRequiredString(IntegrationTestsConfigLiterals.cloudStackLogin)
  val cloudStackPassword = ApplicationConfig.getRequiredString(IntegrationTestsConfigLiterals.cloudStackPassword)
  //CloudStackVaultController
  val vmSecretPath = ApplicationConfig.getRequiredString(ConfigLiterals.vaultVmsBasicPath)
  val accountSecretPath = ApplicationConfig.getRequiredString(ConfigLiterals.vaultAccountsBasicPath)
  val zooKeeperRootNode = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperRootNode)
  //Kafka
  val kafkaEndpoints = ApplicationConfig.getRequiredString(ConfigLiterals.kafkaEndpoints)
  val kafkaPollTimeout = ApplicationConfig.getRequiredInt(ConfigLiterals.kafkaPollTimeot)
  val kafkaTopics = ApplicationConfig.getRequiredString(ConfigLiterals.kafkaTopics).split("[,\\s]+")
  val kafkaEventCount = ApplicationConfig.getRequiredInt(ConfigLiterals.kafkaEventCount)
  val kafkaGroupId = ApplicationConfig.getRequiredString(ConfigLiterals.kafkaGroupId)

  object FaultTolerance {
    val vaultRootToken = ApplicationConfig.getRequiredString(IntegrationTestsConfigLiterals.FaultToleranceTest.vaultRootToken)
    val vaultVersion = ApplicationConfig.getRequiredString(IntegrationTestsConfigLiterals.FaultToleranceTest.vaultVersion)
    val vaultDockerContainerName = ApplicationConfig.getRequiredString(IntegrationTestsConfigLiterals.FaultToleranceTest.vaultDockerContainerName)
    val vaultPort = ApplicationConfig.getRequiredString(IntegrationTestsConfigLiterals.FaultToleranceTest.vaultPort)
    val vaultEndpoints = Array(s"http://localhost:$vaultPort")
  }
}
