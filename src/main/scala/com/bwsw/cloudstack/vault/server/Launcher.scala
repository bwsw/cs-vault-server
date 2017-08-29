package com.bwsw.cloudstack.vault.server

import java.util.concurrent.CountDownLatch

import com.bwsw.cloudstack.vault.server.common.ConfigLoader
import com.bwsw.cloudstack.vault.server.kafka.ConsumerManager
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}
import com.typesafe.scalalogging.StrictLogging
import org.apache.zookeeper.KeeperException.ConnectionLossException
import org.apache.zookeeper.ZooKeeper

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 28.07.17.
  */
object Launcher extends StrictLogging{

  def main(args: Array[String]): Unit = {
    val components = new Components(ConfigLoader.loadConfig())
    val consumerManager = new ConsumerManager(
      ApplicationConfig.getRequiredString(ConfigLiterals.kafkaTopic),
      ApplicationConfig.getRequiredString(ConfigLiterals.kafkaServerList),
      components
    )
    consumerManager.execute()
  }

}
