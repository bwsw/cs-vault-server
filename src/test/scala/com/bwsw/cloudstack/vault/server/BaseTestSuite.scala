package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.common.JsonSerializer

/**
  * Created by medvedev_vv on 29.08.17.
  */
trait BaseTestSuite {
  val jsonSerializer = new JsonSerializer(ignore = true)
  val settings = Components.Settings(MockConfig.cloudStackServiceSettings,
    MockConfig.cloudStackTaskCreatorSettings,
    MockConfig.vaultServiceSettings,
    MockConfig.vaultRestRequestCreatorSettings,
    MockConfig.zooKeeperServiceSettings,
    MockConfig.cloudStackVaultControllerSettings
  )
}
