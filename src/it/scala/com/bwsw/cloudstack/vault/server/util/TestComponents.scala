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
package com.bwsw.cloudstack.vault.server.util

import com.bwsw.cloudstack.vault.server.EventManager
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.util.cloudstack.components.CloudStackTestsComponents
import com.bwsw.cloudstack.vault.server.util.vault.components.VaultTestComponents
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.kafka.reader.Consumer
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

class TestComponents(val vaultTestComponents: VaultTestComponents) extends CloudStackTestsComponents {
  private val logger = LoggerFactory.getLogger(this.getClass)

  lazy val consumer = new Consumer[String,String](Consumer.Settings(
    IntegrationTestsSettings.kafkaEndpoints,
    IntegrationTestsSettings.kafkaGroupId
  ))

  val zooKeeperSettings = ZooKeeperService.Settings(
    IntegrationTestsSettings.zooKeeperEndpoints,
    IntegrationTestsSettings.zooKeeperRetryDelay
  )

  lazy val zooKeeperService = new ZooKeeperService(zooKeeperSettings)

  val controllerSettings = CloudStackVaultController.Settings(
    IntegrationTestsSettings.vmSecretPath,
    IntegrationTestsSettings.accountSecretPath,
    IntegrationTestsSettings.zooKeeperRootNode
  )

  lazy val controller = new CloudStackVaultController(vaultTestComponents.vaultService, cloudStackService, zooKeeperService, controllerSettings)

  val eventManagerSettings = EventManager.Settings(
    IntegrationTestsSettings.kafkaTopics.toList,
    IntegrationTestsSettings.kafkaEventCount
  )

  lazy val eventManager = new EventManager(
    consumer,
    mapper,
    controller,
    eventManagerSettings
  )

  def close(): Unit = {
    List(eventManager.shutdown _, zooKeeperService.close _, consumer.close _).foreach(func => {
      Try(func()) match {
        case Success(x) =>
        case Failure(e: Throwable) =>
          logger.error(s"can not execute func, exception: $e was thrown")
      }
    })
  }
}
