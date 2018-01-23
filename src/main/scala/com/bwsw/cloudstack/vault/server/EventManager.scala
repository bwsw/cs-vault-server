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

import java.util.concurrent.atomic.AtomicBoolean

import com.bwsw.cloudstack.entities.common.traits.Mapper
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.kafka.reader.entities.{TopicInfo, TopicInfoList}
import com.bwsw.kafka.reader.{CheckpointInfoProcessor, Consumer, MessageQueue}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Class is responsible for wrapping logic of working with [[https://github.com/bwsw/kafka-reader]] library
  */
class EventManager[K,V,T](consumer: Consumer[K,V],
                          mapper: Mapper[V],
                          controller: CloudStackVaultController,
                          settings: EventManager.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val dummyFlag = new AtomicBoolean(true)

  def execute(): Unit = {
    val initTopicInfoList = TopicInfoList(settings.topics.map(x => TopicInfo(topic = x)))

    val checkpointInfoProcessor = new CheckpointInfoProcessor[K,V,T](
      initTopicInfoList,
      consumer
    )

    checkpointInfoProcessor.load()

    val messageQueue = new MessageQueue[K,V](consumer)

    val eventHandler = new CloudStackEventHandler(messageQueue, settings.eventCount, mapper, controller)

    while (dummyFlag.get()) {
      eventHandler.handle(dummyFlag)
    }
  }
}

object EventManager {
  case class Settings(topics: List[String], eventCount: Int)
}
