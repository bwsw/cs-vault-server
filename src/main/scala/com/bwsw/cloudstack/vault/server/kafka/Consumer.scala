package com.bwsw.cloudstack.vault.server.kafka

import java.util.concurrent.CountDownLatch
import java.util.{Collections, Properties}

import com.bwsw.cloudstack.vault.server.common.traits.EventHandler
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by medvedev_vv on 31.07.17.
  */
class Consumer[T](val brokers: String,
                  val topic: String,
                  val groupId: String,
                  val pollTimeout: Int,
                  val eventHandler: EventHandler[T])
                 (implicit executionContext: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val props: Properties = createConsumerConfig(brokers)
  private val consumer = new KafkaConsumer[String, String](props)

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

    val futureEvents: List[(Future[Unit], T)] = eventHandler.handleEventsFromRecords(records.asScala.map(_.value()).toList)
    val eventLatch: CountDownLatch  = new CountDownLatch(futureEvents.size)

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
              logger.debug(s"The event: $event was successful")
              eventLatch.countDown()
            case Failure(e: Throwable) =>
              logger.error(s"An exception occurred during the event processing: $e")
              val restartedEvent = eventHandler.restartEvent(event)
              checkEvent(restartedEvent)
          }
      }
    }
  }
}
