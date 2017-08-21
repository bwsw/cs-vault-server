package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService

/**
  * Created by medvedev_vv on 02.08.17.
  */
class Components {
  //services
  val cloudStackService = new CloudStackService
  val vaultService = new VaultService

  //controllers
  val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService)

}
