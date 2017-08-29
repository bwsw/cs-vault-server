package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator

/**
  * Created by medvedev_vv on 29.08.17.
  */
trait BaseSuite {
  val cloudStackSettings = CloudStackService.Settings(1000)
  val vaultSettings = VaultService.Settings(1000, 1000)
  val zooKeeperSettings = ZooKeeperService.Settings(1000)

  val cloudStackTaskCreatorSettings = ApacheCloudStackTaskCreator.Settings(Array.empty[String], "secretKey", "apiKey")
  val vaultRestRequestCreatorSettings = VaultRestRequestCreator.Settings("vaultUrl", "vaultRootToken")
  val zooKeeperTaskCreatorSettings = ZooKeeperTaskCreator.Settings("host")
}
