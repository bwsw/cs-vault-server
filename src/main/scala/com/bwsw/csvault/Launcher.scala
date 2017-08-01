package com.bwsw.csvault

import com.bwsw.csvault.kafka.ConsumerThreadManager
import com.bwsw.csvault.util.ApplicationConfig._
import com.bwsw.csvault.util.ConfigLiterals
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 28.07.17.
  */
object Launcher extends StrictLogging{

  def main(args: Array[String]): Unit = {
    logger.info("Start Application")

    val consumerManager = new ConsumerThreadManager(
      getRequiredString(ConfigLiterals.kafkaTopic),
      getRequiredString(ConfigLiterals.kafkaServerList)
    )
    consumerManager.execute()
  }

}
