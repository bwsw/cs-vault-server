package com.bwsw.cloudstack.vault.server.kafka

import java.util
import java.util.UUID
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackEntityDoesNotExistException
import com.bwsw.cloudstack.vault.server.common.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.util.exception.CriticalException
import org.apache.kafka.clients.consumer.{ConsumerRecord, MockConsumer, OffsetResetStrategy}
import org.apache.kafka.common.TopicPartition
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by medvedev_vv on 15.09.17.
  */
class ConsumerTestSuite extends FlatSpec with Matchers {
  val entityId: UUID = UUID.randomUUID()
  val correctAccountDeleteEvent: String = "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$entityId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
  val expectedEvent = CloudStackEvent(Some(CloudStackEvent.Status.Completed), Some(CloudStackEvent.Action.AccountDelete), Some(entityId))
  val topic = "testTopic"

  "process" should "handle event successful" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(new MockVaultService, new MockCloudStackService, new MockZooKeeperService) {
    }
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[(Future[Unit], CloudStackEvent)] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set((Future(Unit), expectedEvent))
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.1:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }
    assert(consumer.process().isInstanceOf[Unit])

    consumer.shutdown()
  }

  "process" should "handle event successful if CriticalException which includes CloudStackEntityDoesNotExistException was thrown" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(new MockVaultService, new MockCloudStackService, new MockZooKeeperService) {
    }
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[(Future[Unit], CloudStackEvent)] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set((Future(throw new CriticalException(new CloudStackEntityDoesNotExistException("message"))), expectedEvent))
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.2:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }

    assert(consumer.process().isInstanceOf[Unit])

    consumer.shutdown()
  }

  "process" should "restart event handling if CriticalException which includes non-CloudStackEntityDoesNotExistException was thrown" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(new MockVaultService, new MockCloudStackService, new MockZooKeeperService) {
    }
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[(Future[Unit], CloudStackEvent)] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set((Future(throw new CriticalException(new Exception)), expectedEvent))
      }

      override def restartEvent(event: CloudStackEvent): (Future[Unit], CloudStackEvent) = {
        assert(event == expectedEvent, "event is wrong")
        (Future(Unit), event)
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.2:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }

    assert(consumer.process().isInstanceOf[Unit])

    consumer.shutdown()
  }

  "process" should "does not swallowed non-CriticalException" in {
    val mockConsumer = new MockConsumer[String, String](OffsetResetStrategy.EARLIEST)

    mockConsumer.assign(util.Arrays.asList(new TopicPartition(topic, 0)))
    val beginningOffsets: util.Map[TopicPartition, java.lang.Long] = new util.HashMap()
    beginningOffsets.put(new TopicPartition(topic, 0), Long2long(0L))
    mockConsumer.updateBeginningOffsets(beginningOffsets)
    mockConsumer.addRecord(new ConsumerRecord[String, String](topic, 0, 0L, "key", correctAccountDeleteEvent))

    val controller = new CloudStackVaultController(new MockVaultService, new MockCloudStackService, new MockZooKeeperService) {
    }
    val cloudStackEventHandler = new CloudStackEventHandler(controller){
      override def handleEventsFromRecords(recordValues: List[String]): Set[(Future[Unit], CloudStackEvent)] = {
        assert(recordValues == List(correctAccountDeleteEvent), "record is wrong")
        Set((Future(throw new Exception), expectedEvent))
      }
    }

    val consumer = new Consumer[CloudStackEvent]("127.0.0.2:9000", topic, "groupId", 10000, cloudStackEventHandler){
      override protected val consumer = mockConsumer
    }

    assertThrows[Exception]{
      consumer.process()
    }

    consumer.shutdown()
  }
}
