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

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator

object Components {
  case class Settings(cloudStackServiceSettings: CloudStackService.Settings,
                      cloudStackTaskCreatorSettings: CloudStackTaskCreator.Settings,
                      vaultServiceSettings: VaultService.Settings,
                      vaultRestRequestCreatorSettings: VaultRestRequestCreator.Settings,
                      zooKeeperServiceSettings: ZooKeeperService.Settings,
                      zooKeeperTaskCreatorSettings: ZooKeeperTaskCreator.Settings)
}

class Components(settings: Components.Settings) {
  //services
  val cloudStackService = new CloudStackService(
    new CloudStackTaskCreator(settings.cloudStackTaskCreatorSettings),
    settings.cloudStackServiceSettings
  )
  val vaultService = new VaultService(
    new VaultRestRequestCreator(settings.vaultRestRequestCreatorSettings),
    settings.vaultServiceSettings
  )
  val zooKeeperService = new ZooKeeperService(
    new ZooKeeperTaskCreator(settings.zooKeeperTaskCreatorSettings),
    settings.zooKeeperServiceSettings
  )

  //controllers
  val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService, zooKeeperService)

}
