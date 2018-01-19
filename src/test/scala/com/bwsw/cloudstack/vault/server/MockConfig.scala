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
package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.KeyAuthenticationClientCreator
import com.bwsw.cloudstack.entities.Executor
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService

object MockConfig {
  val executorSettings = Executor.Settings(endpoints = Array("127.0.0.1:8888"), retryDelay = 10000)
  val clientCreatorSettings = KeyAuthenticationClientCreator.Settings("secretKey", "apiKey")

  val zooKeeperServiceSettings = ZooKeeperService.Settings(endpoints = "127.0.0.1:2181", retryDelay = 10000)

  val vaultServiceSettings = VaultService.Settings(tokenPeriod = 10000, retryDelay =  10000)
  val vaultRestRequestCreatorSettings = VaultRestRequestCreator.Settings("http://127.0.0.1:8200", "rootToken")

  val cloudStackVaultControllerSettings = CloudStackVaultController.Settings("secret/cs/vms/", "secret/cs/account/", "/cs_vault_server")
}
