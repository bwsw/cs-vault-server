package com.bwsw.csvault

import com.bwsw.csvault.common.JsonSerializer
import com.bwsw.csvault.cs.entities.CloudStackEvent
import com.bwsw.csvault.kafka.ConsumerThreadManager
import com.bwsw.csvault.util.ApplicationConfig._
import com.bwsw.csvault.util.ConfigLiterals
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.scalalogging.StrictLogging


/**
  * Created by medvedev_vv on 28.07.17.
  */
object Launcher extends StrictLogging{

  def main(args: Array[String]): Unit = {
    val consumerManager = new ConsumerThreadManager(
      getRequiredString(ConfigLiterals.kafkaTopic),
      getRequiredString(ConfigLiterals.kafkaServerList)
    )
    consumerManager.execute()
  }

}
