package com.bwsw.cloudstack.vault.server.kafka

import java.util.Properties

import com.bwsw.cloudstack.vault.server.Components
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Created by medvedev_vv on 01.08.17.
  */
class ConsumerThreadManager(topic: String, brokers: String) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val components = new Components()
  private var consumerPool: Array[ConsumerThread] = Array.empty

  def execute() {
    val partitions = getPartitionListFor(topic)
    partitions.foreach { x =>
      logger.info(s"start threadConsumer for partition: $x")
      val consumerThread = new ConsumerThread(brokers, x, new CloudStackEventHandler(components.csVaultController))
      consumerPool = consumerPool :+ consumerThread
      val thread = new Thread(consumerThread)
      thread.start()
    }
    while(!consumerPool.exists(_.closed.get())){
      Thread.sleep(10000)
    }
    consumerPool.filterNot(_.closed.get()).foreach(_.shutdown())
  }

  private def getPartitionListFor(topic: String): List[TopicPartition] = {
    logger.debug(s"getPartitionListFor: $topic")
    val prop = new Properties()
    prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer")
    prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer(prop)
    val partitionList = producer.partitionsFor(topic).asScala.toList.map { x =>
      new TopicPartition(x.topic(), x.partition())
    }
    producer.close()
    logger.debug(s"partition's ids: ${partitionList.map(_.partition())}")
    partitionList
  }

}
