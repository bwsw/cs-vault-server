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
package com.bwsw.cloudstack.vault.server.util.kafka

import com.bwsw.kafka.reader.Consumer
import com.bwsw.kafka.reader.entities.{TopicPartitionInfo, TopicPartitionInfoList}
import org.apache.kafka.common.TopicPartition

import scala.collection.JavaConverters._

class TestConsumer[K,V](brokers: String, groupId: String) extends Consumer[K,V](Consumer.Settings(brokers, groupId)) {
  def assignToEnd(topic: String): Unit = {
    val partitions = consumer.partitionsFor(topic).asScala.map(x => new TopicPartition(topic, x.partition())).asJavaCollection
    consumer.assign(partitions)
    consumer.endOffsets(partitions).asScala.foreach {
      case (partition, offset) => consumer.seek(partition, offset)
    }
  }

  def commitToEnd(topic: String): Unit = {
    val partitions = consumer.partitionsFor(topic).asScala.map(x => new TopicPartition(topic, x.partition())).asJavaCollection
    val topicPartitionsInfo = consumer.endOffsets(partitions).asScala.map {
      case (partition, offset) => TopicPartitionInfo(topic, partition.partition(), offset)
    }
    commit(TopicPartitionInfoList(topicPartitionsInfo.toList))
  }
}
