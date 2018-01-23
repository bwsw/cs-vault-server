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

import com.bwsw.cloudstack.KeyAuthenticationClientCreator
import com.bwsw.cloudstack.entities.Executor
import com.bwsw.cloudstack.entities.common.JsonMapper
import com.bwsw.cloudstack.entities.dao.{AccountDao, TagDao, VirtualMachineDao}
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestExecutor
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.kafka.reader.Consumer
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object Components {
  case class Settings(executorSettings: Executor.Settings,
                      keyAuthenticationClientCreatorSettings: KeyAuthenticationClientCreator.Settings,
                      vaultServiceSettings: VaultService.Settings,
                      vaultRestRequestCreatorSettings: VaultRestRequestExecutor.Settings,
                      zooKeeperServiceSettings: ZooKeeperService.Settings,
                      cloudStackVaultControllerSettings: CloudStackVaultController.Settings,
                      consumerSettings: Consumer.Settings)
}

class Components(settings: Components.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  //CloudStack authenticator
  lazy val clientCreator = new KeyAuthenticationClientCreator(settings.keyAuthenticationClientCreatorSettings)

  //CloudStack request executor
  lazy val executor = new Executor(settings.executorSettings, clientCreator)

  //Json String deserializer
  lazy val daoMapper = new JsonMapper(ignoreUnknownProperties = true)

  //dao
  lazy val vmDao = new VirtualMachineDao(executor, daoMapper)
  lazy val accountDao = new AccountDao(executor, daoMapper)
  lazy val tagDao = new TagDao(executor, daoMapper)

  //services
  lazy val cloudStackService = new CloudStackService(accountDao, tagDao, vmDao)
  lazy val vaultService = new VaultService(
    new VaultRestRequestExecutor(settings.vaultRestRequestCreatorSettings),
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

  //event handling
  lazy val consumer = new Consumer[String, String] (
    settings.consumerSettings
  )
  lazy val eventMapper = new JsonMapper(ignoreUnknownProperties = true)

  def close(): Unit = {
    List(zooKeeperService.close _, consumer.close _).foreach(func => {
      Try(func()) match {
        case Success(x) =>
        case Failure(e: Throwable) =>
          logger.error(s"the function: '$func' was executed with exception: $e")
      }
    })
  }
}
