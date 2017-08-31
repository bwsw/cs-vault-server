package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator

/**
  * Created by medvedev_vv on 02.08.17.
  */
object Components {
  case class Settings(cloudStackServiceSettings: CloudStackService.Settings,
                      cloudStackTaskCreatorSettings: ApacheCloudStackTaskCreator.Settings,
                      vaultServiceSettings: VaultService.Settings,
                      vaultRestRequestCreatorSettings: VaultRestRequestCreator.Settings,
                      zooKeeperServiceSettings: ZooKeeperService.Settings,
                      zooKeeperTaskCreatorSettings: ZooKeeperTaskCreator.Settings)
}

class Components(settings: Components.Settings) {
  //services
  val cloudStackService = new CloudStackService(
    new ApacheCloudStackTaskCreator(settings.cloudStackTaskCreatorSettings),
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
