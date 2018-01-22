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
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, DataPath}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for application launch.
  * It creates all services needed to start application.
  * Also it provides support the Leader-Follower registration with help ZooKeeper + Curator
  */
object Launcher {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var leaderLatch: Option[LeaderLatch] = None
  private val components = new Components(ConfigLoader.loadConfig())

  def main(args: Array[String]): Unit = {
    Try {
      start()
    } match {
      case Success(_) =>
        logger.info(s"Application stopped")
        components.close()
        leaderLatch.foreach(_.close())
      case Failure(e) =>
        logger.error(s"Application stopped, exception: '$e' was thrown")
        components.close()
        leaderLatch.foreach(_.close())
    }

  }

  protected def start(): Unit = {
    leaderLatch = Option(createLeaderLatch(
      ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperEndpoints)
    ))

    val eventManagerSettings = EventManager.Settings(
      ApplicationConfig.getRequiredString(ConfigLiterals.kafkaTopics).split("[,\\s]+").toList,
      ApplicationConfig.getRequiredInt(ConfigLiterals.kafkaEventCount)
    )

    val eventManager = new EventManager(
      components.consumer,
      components.eventMapper,
      components.cloudStackVaultController,
      eventManagerSettings
    )

    eventManager.execute()
  }

  protected def createLeaderLatch(zookeeperServer: String, nodeId: String = ""): LeaderLatch = {
    logger.trace(s"createLeaderLatch(zookeeperServer: $zookeeperServer, nodeId: $nodeId)")
    val leader = new LeaderLatch(zookeeperServer, DataPath.masterLatchNode, nodeId)
    leader.start()
    leader.acquireLeadership(1000)

    leader
  }
}
