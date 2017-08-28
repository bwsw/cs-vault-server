package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator

/**
  * Created by medvedev_vv on 02.08.17.
  */
class Components {

  //services
  val cloudStackService = new CloudStackService(new ApacheCloudStackTaskCreator)
  val vaultService = new VaultService(new VaultRestRequestCreator)

  //controllers
  val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService)

}
