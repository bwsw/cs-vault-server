package com.bwsw.cloudstack.vault.server

import br.com.autonomiccs.apacheCloudStack.client.ApacheCloudStackClient
import br.com.autonomiccs.apacheCloudStack.client.beans.ApacheCloudStackUser
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}
import com.bwsw.cloudstack.vault.server.vault.VaultService

/**
  * Created by medvedev_vv on 02.08.17.
  */
class Components {

  //services
  val cloudStackService = new CloudStackService(new ApacheCloudStackTaskCreator())
  val vaultService = new VaultService

  //controllers
  val cloudStackVaultController = new CloudStackVaultController(vaultService, cloudStackService)

}
