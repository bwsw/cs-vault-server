package com.bwsw.csvault.kafka

import java.util.{Collections, Properties}

import com.bwsw.csvault.common.JsonSerializer
import com.bwsw.csvault.common.traits.EventHandler
import com.bwsw.csvault.cs.entities.CloudStackEvent
import com.bwsw.csvault.cs.util.CloudStackEventHandler
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
/**
  * Created by medvedev_vv on 31.07.17.
  */
class ConsumerThread(val brokers: String,
                     val partition: TopicPartition,
                     val eventHandler: EventHandler) extends Runnable {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val props: Properties = createConsumerConfig(brokers)
  private val consumer = new KafkaConsumer[String, String](props)
  private val jsonSerializer = new JsonSerializer(true)
  private val timeout = 10000

  def createConsumerConfig(brokers: String): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "10000")
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props
  }

  def run() {
    consumer.assign(Collections.singletonList(partition))
    logger.info(s"Assign to the partition: ${partition.partition()}")
    while (true) {
      logger.debug(s"Waiting for records that consumed from kafka for $timeout milliseconds\n")
      val records = consumer.poll(timeout)
      records.asScala.foreach { x =>
        eventHandler.handleEvent(x.value())
      }
    }
  }

}
