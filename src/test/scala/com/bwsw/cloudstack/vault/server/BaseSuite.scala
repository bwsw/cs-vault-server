package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.common.ConfigLoader

/**
  * Created by medvedev_vv on 29.08.17.
  */
trait BaseSuite {
  val settings: Components.Settings = ConfigLoader.loadConfig()
}
