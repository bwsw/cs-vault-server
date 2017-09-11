package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.common.{ConfigLoader, LeaderLatch}
import com.bwsw.cloudstack.vault.server.kafka.ConsumerManager
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, RequestPath}
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 28.07.17.
  */
object Launcher extends StrictLogging {
  private var leaderLatch: Option[LeaderLatch] = None

  def main(args: Array[String]): Unit = {
    Try {
      start()
    } match {
      case Success(_) =>
        logger.info(s"Application started")
      case Failure(e) =>
        logger.error(s"Application did not start, exception was thrown: $e")
        leaderLatch.foreach(_.close())
    }

  }

  protected def start(): Unit = {
    leaderLatch = Option(createLeaderLatch(
      ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperUrl)
    ))

    val components = new Components(ConfigLoader.loadConfig())
    val consumerManager = new ConsumerManager(
      ApplicationConfig.getRequiredString(ConfigLiterals.kafkaTopic),
      ApplicationConfig.getRequiredString(ConfigLiterals.kafkaServerList),
      components
    )
    consumerManager.execute()

    leaderLatch.foreach(_.close())
  }

  protected def createLeaderLatch(zookeeperServer: String, nodeId: String = ""): LeaderLatch = {
    logger.debug(s"createLeaderLatch(zookeeperServer: $zookeeperServer)")
    val leader = new LeaderLatch(zookeeperServer, RequestPath.masterLatchNode, nodeId)
    leader.start()
    leader.acquireLeadership(1000)

    leader
  }
}
