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
import com.bwsw.cloudstack.entities.common.JsonMapper
import com.bwsw.cloudstack.entities.dao.{AccountDao, TagDao, VirtualMachineDao}
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService

import scala.concurrent.ExecutionContext.Implicits.global

object Components {
  case class Settings(executorSettings: Executor.Settings,
                      keyAuthenticationClientCreatorSettings: KeyAuthenticationClientCreator.Settings,
                      vaultServiceSettings: VaultService.Settings,
                      vaultRestRequestCreatorSettings: VaultRestRequestCreator.Settings,
                      zooKeeperServiceSettings: ZooKeeperService.Settings,
                      cloudStackVaultControllerSettings: CloudStackVaultController.Settings)
}

class Components(settings: Components.Settings) {
  //CloudStack authenticator
  lazy val clientCreator = new KeyAuthenticationClientCreator(settings.keyAuthenticationClientCreatorSettings)

  //CloudStack request executor
  lazy val executor = new Executor(settings.executorSettings, clientCreator)

  //Json String deserializer
  lazy val mapper = new JsonMapper(ignoreUnknownProperties = true)

  //dao
  lazy val vmDao = new VirtualMachineDao(executor, mapper)
  lazy val accountDao = new AccountDao(executor, mapper)
  lazy val tagDao = new TagDao(executor, mapper)

  //services
  lazy val cloudStackService = new CloudStackService(accountDao, tagDao, vmDao)
  lazy val vaultService = new VaultService(
    new VaultRestRequestCreator(settings.vaultRestRequestCreatorSettings),
    settings.vaultServiceSettings
  )
  lazy val zooKeeperService = new ZooKeeperService(
    settings.zooKeeperServiceSettings
  )

  //controllers
  lazy val cloudStackVaultController = new CloudStackVaultController(
    vaultService,
    cloudStackService,
    zooKeeperService,
    settings.cloudStackVaultControllerSettings
  )

  //handlers
  lazy val cloudStackEventHandler = new CloudStackEventHandler(cloudStackVaultController)


  def close(): Unit = {
    zooKeeperService.close()
  }

}
