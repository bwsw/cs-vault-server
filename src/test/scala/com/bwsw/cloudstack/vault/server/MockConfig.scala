package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService

/**
  * Created by medvedev_vv on 31.08.17.
  */
object MockConfig {
  val zooKeeperServiceSettings = ZooKeeperService.Settings(endpoints = "127.0.0.1:2181", retryDelay = 10000)

  val cloudStackServiceSettings = CloudStackService.Settings(retryDelay = 10000)
  val cloudStackTaskCreatorSettings = CloudStackTaskCreator.Settings(Array("http://127.0.0.1:8080/client/api"), "secretKey", "apiKey")

  val vaultServiceSettings = VaultService.Settings(tokenPeriod = 10000, retryDelay =  10000)
  val vaultRestRequestCreatorSettings = VaultRestRequestCreator.Settings("http://127.0.0.1:8200", "rootToken")

  val cloudStackVaultControllerSettings = CloudStackVaultController.Settings("secret/cs/vms/", "secret/cs/account/", "/cs_vault_server")
}
