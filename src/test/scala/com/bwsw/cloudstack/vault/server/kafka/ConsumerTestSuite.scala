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

import java.util
import java.util.UUID
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import com.bwsw.cloudstack.vault.server.common.ProcessingEventResult
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import com.bwsw.cloudstack.vault.server.common.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.util.exception.{AbortedException, CriticalException}
import org.apache.kafka.clients.consumer.{ConsumerRecord, MockConsumer, OffsetResetStrategy}
import org.apache.kafka.common.TopicPartition
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ConsumerTestSuite extends FlatSpec with Matchers with BaseTestSuite {
  val entityId: UUID = UUID.randomUUID()
  val correctAccountDeleteEvent: String = "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$entityId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
  val expectedEvent = CloudStackEvent(Some(CloudStackEvent.Status.Completed), Some(CloudStackEvent.Action.AccountDelete), Some(entityId))
  val topic = "testTopic"

  "process" should "handle event successfully" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ) {}
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[ProcessingEventResult[CloudStackEvent]] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set(ProcessingEventResult(expectedEvent, Future(Unit)))
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.1:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }
    assert(consumer.process().isInstanceOf[Unit])

    consumer.shutdown()
  }

  "process" should "handle event successfully if CriticalException which includes CloudStackEntityDoesNotExistException was thrown" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ) {}
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[ProcessingEventResult[CloudStackEvent]] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set(ProcessingEventResult(expectedEvent, Future(throw new CloudStackEntityDoesNotExistException("message"))))
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.2:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }

    assert(consumer.process().isInstanceOf[Unit])

    consumer.shutdown()
  }

  "process" should "restart event handling if CloudStackFatalException was thrown" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ) {}
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[ProcessingEventResult[CloudStackEvent]] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set(ProcessingEventResult(expectedEvent, Future(throw new CloudStackFatalException("test exception"))))
      }

      override def restartEvent(event: CloudStackEvent): ProcessingEventResult[CloudStackEvent] = {
        assert(event == expectedEvent, "event is wrong")
        ProcessingEventResult(event, Future(Unit))
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.2:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }

    assert(consumer.process().isInstanceOf[Unit])

    consumer.shutdown()
  }

  "process" should "throw AbortedException if non-CriticalException has been thrown during event processing" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ) {}
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[ProcessingEventResult[CloudStackEvent]] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set(ProcessingEventResult(expectedEvent, Future(throw new Exception)))
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.2:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }

    assertThrows[AbortedException]{
      consumer.process()
    }

    consumer.shutdown()
  }
}
