package com.bwsw.cloudstack.vault.server

import java.util.StringTokenizer

import com.bwsw.cloudstack.vault.server.kafka.ConsumerThreadManager
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}
import com.typesafe.scalalogging.StrictLogging

/**
  * Created by medvedev_vv on 28.07.17.
  */
object Launcher extends StrictLogging{

  def main(args: Array[String]): Unit = {
    val consumerManager = new ConsumerThreadManager(
      ApplicationConfig.getRequiredString(ConfigLiterals.kafkaTopic),
      ApplicationConfig.getRequiredString(ConfigLiterals.kafkaServerList)
    )
    consumerManager.execute()
  }

}
