package com.bwsw.cloudstack.vault.server.cloudstack.util

import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.common.traits.EventHandler
import com.bwsw.cloudstack.vault.server.controllers.CSVaultController
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackEventHandler(controller: CSVaultController) extends EventHandler {
  private val jsonSerializer = new JsonSerializer()
  private val logger = LoggerFactory.getLogger(this.getClass)

  def handleEvent(value: String): Unit = {
    import CloudStackEvent.Action._
    val event = jsonSerializer.deserialize[CloudStackEvent](value)
    event.action match {
      case VMCreate => logger.debug("VMCreate")
      case VMDelete => logger.debug("VMDelete")
      case AccountCreate => logger.debug("AccountCreate")
      case AccountDelete => logger.debug("AccountDelete")
      case UserCreate => logger.debug("UserCreate")
    }
  }
}