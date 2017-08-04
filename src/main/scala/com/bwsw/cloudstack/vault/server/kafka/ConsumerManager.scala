package com.bwsw.cloudstack.vault.server.kafka

import com.bwsw.cloudstack.vault.server.Components
import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by medvedev_vv on 01.08.17.
  */
class ConsumerManager(topic: String, brokers: String, components: Components) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val timeout = 10000
  private val groupId = "group01"

  def execute(): Unit = {
    val consumer = new Consumer[CloudStackEvent](brokers, topic, groupId, timeout, new CloudStackEventHandler(components.csVaultController))
    try {
      while(true) {
        consumer.start()
      }
    } catch {
      case e: InterruptedException => consumer.shutdown()
    }
  }
}

