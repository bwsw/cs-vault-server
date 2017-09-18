/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.vault.server.common.{ConfigLoader, LeaderLatch}
import com.bwsw.cloudstack.vault.server.kafka.ConsumerManager
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, RequestPath}
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for launching application.
  * It creating all services needed to start application.
  * Also it provides support the Leader-Follower registration with help ZooKeeper + Curator
  *
  * @author Vladislav Medvedev
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
