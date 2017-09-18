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
package com.bwsw.cloudstack.vault.server.kafka

import com.bwsw.cloudstack.vault.server.Components
import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class ConsumerManager(topic: String, brokers: String, components: Components) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val timeout = 10000
  private val groupId = "group01"

  def execute(): Unit = {
    val consumer = new Consumer[CloudStackEvent](brokers, topic, groupId, timeout, new CloudStackEventHandler(components.cloudStackVaultController))
    Try {
      consumer.subscribe()
      while(true) {
        consumer.process()
      }
    } match {
      case Success(x) => x
      case Failure(e: Throwable) =>
        logger.error(s"exception: ${e.getMessage} was thrown in consumer.process()")
        consumer.shutdown()
    }
  }
}
