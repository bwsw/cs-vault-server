package com.bwsw.cloudstack.vault.server.common.traits

/**
  * Created by medvedev_vv on 02.08.17.
  */
trait EventHandler {
  def handleEvent(event: String): Unit
}
