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
package com.bwsw.cloudstack.vault.server.common

import com.bwsw.cloudstack.vault.server.Components
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService

object ConfigLoader {

  def loadConfig(): Components.Settings = {
    //zookeeper
    val zooKeeperRetryDelay: Int = ApplicationConfig.getRequiredInt(ConfigLiterals.zooKeeperRetryDelay)
    val zooKeeperUrl: String = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperUrl)
    //vault
    val vaultTokenPeriod: Int = ApplicationConfig.getRequiredInt(ConfigLiterals.tokenPeriod)
    val vaultRetryDelay: Int = ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    val vaultUrl: String = ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)
    val vaultRootToken: String = ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken)
    //cloudstack
    val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
    val cloudStackUrlList: Array[String] = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackApiUrlList).split("[,\\s]+")
    val cloudStackSecretKey: String = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackSecretKey)
    val cloudStackApiKey: String = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackApiKey)
    //cloudStackVaultController
    val vmSecretPath: String = ApplicationConfig.getRequiredString(ConfigLiterals.vmsVaultBasicPath)
    val accountSecretPath: String = ApplicationConfig.getRequiredString(ConfigLiterals.accountsVaultBasicPath)
    val zooKeeperRootNode: String = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperRootNode)

    Components.Settings(
      CloudStackService.Settings(cloudStackRetryDelay),
      CloudStackTaskCreator.Settings(cloudStackUrlList, cloudStackSecretKey, cloudStackApiKey),
      VaultService.Settings(vaultTokenPeriod, vaultRetryDelay),
      VaultRestRequestCreator.Settings(vaultUrl, vaultRootToken),
      ZooKeeperService.Settings(zooKeeperUrl, zooKeeperRetryDelay),
      CloudStackVaultController.Settings(vmSecretPath, accountSecretPath, zooKeeperRootNode)
    )
  }
}
