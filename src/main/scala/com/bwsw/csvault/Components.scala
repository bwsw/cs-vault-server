package com.bwsw.csvault

import com.bwsw.csvault.controllers.CSVaultController
import com.bwsw.csvault.cs.CloudStackService
import com.bwsw.csvault.vault.VaultService

/**
  * Created by medvedev_vv on 02.08.17.
  */
class Components {
  //services
  lazy val cloudStackService = new CloudStackService
  lazy val vaultService = new VaultService

  //controllers
  lazy val csVaultController = new CSVaultController(vaultService, cloudStackService)

}
