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
package com.bwsw.cloudstack.vault.server.e2e

import com.bwsw.cloudstack.vault.server.{EventManager, IntegrationTestsComponents}
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.util.IntegrationTestsSettings
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.kafka.reader.Consumer

import scala.util.Try

class TestComponents extends IntegrationTestsComponents {
  val consumer = new Consumer[String,String](Consumer.Settings(
    IntegrationTestsSettings.kafkaEndpoints,
    IntegrationTestsSettings.kafkaGroupId
  ))

  val zooKeeperService = new ZooKeeperService(ZooKeeperService.Settings(
    IntegrationTestsSettings.zooKeeperEndpoints,
    IntegrationTestsSettings.zooKeeperRetryDelay
  ))

  val controllerSettings = CloudStackVaultController.Settings(
    IntegrationTestsSettings.vmSecretPath,
    IntegrationTestsSettings.accountSecretPath,
    IntegrationTestsSettings.zooKeeperRootNode
  )

  val controller = new CloudStackVaultController(vaultService, cloudStackService, zooKeeperService, controllerSettings)

  val eventManagerSettings = EventManager.Settings(
    IntegrationTestsSettings.kafkaTopics.toList,
    IntegrationTestsSettings.kafkaEventCount
  )

  val eventManager = new EventManager(
    consumer,
    mapper,
    controller,
    eventManagerSettings
  )

  def close(): Unit = {
    List(eventManager.shutdown _, zooKeeperService.close _, consumer.close _).foreach(func => {
      Try(func())
    })
  }
}
