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

import java.util.concurrent.CountDownLatch
import java.util.{Collections, Properties}

import com.bwsw.cloudstack.vault.server.common.InterruptableCountDawnLatch
import com.bwsw.cloudstack.vault.server.common.traits.EventHandler
import com.bwsw.cloudstack.vault.server.util.exception.{CriticalException, FatalException}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Class is responsible for getting events from Kafka topic
  */
class Consumer[T](val brokers: String,
                  val topic: String,
                  val groupId: String,
                  val pollTimeout: Int,
                  val eventHandler: EventHandler[T])
                 (implicit executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val props: Properties = createConsumerConfig(brokers)
  protected val consumer: org.apache.kafka.clients.consumer.Consumer[String, String] = new KafkaConsumer[String, String](props)

  def createConsumerConfig(brokers: String): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10000")
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props
  }

  def shutdown(): Unit = {
    consumer.close()
  }

  def subscribe(): Unit = {
    logger.info(s"Subscribe to the topic: $topic")
    consumer.subscribe(Collections.singletonList(topic))
  }

  def process() {
    logger.debug(s"Waiting for records that consumed from kafka for $pollTimeout milliseconds\n")
    val records = consumer.poll(pollTimeout)

    val futureEvents: Set[(Future[Unit], T)] = eventHandler.handleEventsFromRecords(records.asScala.map(_.value()).toList)
    val eventLatch: InterruptableCountDawnLatch = new InterruptableCountDawnLatch(new CountDownLatch(futureEvents.size))

    futureEvents.foreach { x =>
      checkEvent(x)
    }

    eventLatch.await()

    consumer.commitSync()

    def checkEvent(eventForCheck: (Future[Unit], T)): Unit = {
      eventForCheck match {
        case (future, event) =>
          future.onComplete {
            case Success(x) =>
              logger.info(s"The event: $event was successful")
              eventLatch.succeed()
            case Failure(e: FatalException) =>
              logger.warn("An exception: \"" + s"${e.getMessage}" +
                "\" occurred during the event: \"" + s"$event" + "\" processing is fatal, " +
                "the event will be restarted after 2 seconds")
              Thread.sleep(2000)
              val restartedEvent = eventHandler.restartEvent(event)
              checkEvent(restartedEvent)
            case Failure(e: CriticalException) =>
              logger.warn("An exception: \"" + s"${e.getMessage}" +
                "\" occurred during the event: \"" + s"$event" + "\" processing is not fatal and will be ignored")
              eventLatch.succeed()
            case Failure(e: Throwable) =>
              logger.error(s"Unhandled exception was thrown: $e")
              eventLatch.abort()
          }
      }
    }
  }
}
