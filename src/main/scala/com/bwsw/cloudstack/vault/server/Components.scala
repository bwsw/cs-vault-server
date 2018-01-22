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

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.kafka.reader.Consumer
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object Components {
  case class Settings(cloudStackServiceSettings: CloudStackService.Settings,
                      cloudStackTaskCreatorSettings: CloudStackTaskCreator.Settings,
                      vaultServiceSettings: VaultService.Settings,
                      vaultRestRequestCreatorSettings: VaultRestRequestCreator.Settings,
                      zooKeeperServiceSettings: ZooKeeperService.Settings,
                      cloudStackVaultControllerSettings: CloudStackVaultController.Settings,
                      consumerSettings: Consumer.Settings)
}

class Components(settings: Components.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  //services
  lazy val cloudStackService = new CloudStackService(
    new CloudStackTaskCreator(settings.cloudStackTaskCreatorSettings),
    settings.cloudStackServiceSettings
  )
  lazy val vaultService = new VaultService(
    new VaultRestRequestCreator(settings.vaultRestRequestCreatorSettings),
    settings.vaultServiceSettings
  )
  lazy val zooKeeperService = new ZooKeeperService(
    settings.zooKeeperServiceSettings
  )

  //controllers
  lazy val cloudStackVaultController = new CloudStackVaultController(
    vaultService,
    cloudStackService,
    zooKeeperService,
    settings.cloudStackVaultControllerSettings
  )

  lazy val consumer = new Consumer[String, String] (
    settings.consumerSettings
  )

  def close(): Unit = {
    close(List(zooKeeperService.close, consumer.close))
  }

  private def close(closeFunctionList: List[() => Unit]): Unit = {
    if (closeFunctionList.nonEmpty) {
      val closeFunction = closeFunctionList.head
      Try {
        closeFunction()
      } match {
        case Success(x) =>
        case Failure(e: Throwable) =>
          logger.error(s"the function: $closeFunction was executed with exception: $e")
          close(closeFunctionList.tail)
      }
    }
  }
}
