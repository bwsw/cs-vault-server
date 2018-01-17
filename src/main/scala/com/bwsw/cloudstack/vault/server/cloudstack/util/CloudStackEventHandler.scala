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
package com.bwsw.cloudstack.vault.server.cloudstack.util

import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.common.{InterruptableCountDawnLatch, JsonSerializer}
import com.bwsw.cloudstack.vault.server.util.exception.{CriticalException, FatalException}
import com.bwsw.kafka.reader.entities.OutputEnvelope
import com.bwsw.kafka.reader.{EventHandler, MessageQueue}
import com.fasterxml.jackson.core.JsonParseException

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for processing CloudStack events.
  *
  * @param controller enables logic execution from event
  */
class CloudStackEventHandler[K](messageQueue: MessageQueue[K,String], //TODO: change type "String" to "V" after use Mapper instead of JsonSerializer
                                messageCount: Int,
                                controller: CloudStackVaultController)
                               (implicit executionContext: ExecutionContext)
  extends EventHandler[K,String,Unit](messageQueue, messageCount) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val jsonSerializer = new JsonSerializer(ignoreUnknownProperties = true)

  override def handle(flag: AtomicBoolean): List[OutputEnvelope[Unit]] = {
    logger.trace(s"handle(flag: $flag")
    Try {
      handleRecordsFromQueue()
    } match {
      case Success(x) =>
        x
      case Failure(e: Throwable) =>
        flag.set(false)
        List.empty[OutputEnvelope[Unit]]
    }
  }

  private def handleRecordsFromQueue(): List[OutputEnvelope[Unit]] = {
    val inputEnvelopes = messageQueue.take(messageCount)
    val eventLatch = new InterruptableCountDawnLatch(new CountDownLatch(inputEnvelopes.size))

    val result = inputEnvelopes.map { envelope =>
      val record = envelope.data
      val event = Try {
        jsonSerializer.deserialize[CloudStackEvent](record)
      } match {
        case Success(x) =>
          logger.debug(s"The record: $record was deserialized to event: $x")
          x
        case Failure(e: JsonParseException) =>
          logger.warn("Can not parse the record: \"" + s"$record" + "\", the empty CloudStackEvent will be returned")
          new CloudStackEvent(None, None, None)
        case Failure(e: Throwable) =>
          logger.error(s"Exception $e occurred during the deserialization of the record: " + "\"" + s"$record" + "\"")
          throw e
      }

      OutputEnvelope(
        envelope.topic,
        envelope.partition,
        envelope.offset,
        handleEvent(eventLatch, event)
      )
    }
    eventLatch.await()
    result
  }


  private def handleEvent(eventLatch: InterruptableCountDawnLatch, event: CloudStackEvent): Unit = {
    import CloudStackEvent.{Action, Status}
    event match {
      case CloudStackEvent(Some(status), Some(action), Some(entityId))
        if status == Status.Completed && action == Action.AccountCreate =>
          logger.info(s"handle AccountCreateEvent(status: $status, entityId: $entityId)")
          Future(runEventHandle(eventLatch, event, entityId, controller.handleAccountCreate))

      case CloudStackEvent(Some(status), Some(action), Some(entityId))
        if status == Status.Completed && action == Action.AccountDelete =>
          logger.info(s"handle AccountDeleteEvent(status: $status, entityId: $entityId)")
          Future(runEventHandle(eventLatch, event, entityId, controller.handleAccountDelete))

      case CloudStackEvent(Some(status), Some(action), Some(entityId))
        if status == Status.Completed && action == Action.VMCreate =>
          logger.info(s"handle VirtualMachineCreateEvent(status: $status, entityId: $entityId)")
          Future(runEventHandle(eventLatch, event, entityId, controller.handleVmCreate))

      case CloudStackEvent(Some(status), Some(action), Some(entityId))
        if status == Status.Completed && action == Action.VMDelete =>
          logger.info(s"handle VirtualMachineDestroyEvent(status: $status, entityId: $entityId)")
          Future(runEventHandle(eventLatch, event, entityId, controller.handleVmDelete))

      case _ =>
        eventLatch.succeed()
    }
  }

  private def runEventHandle(leaderLatch: InterruptableCountDawnLatch,
                             event: CloudStackEvent,
                             entityId: UUID,
                             eventHandleFunc: (UUID) => Unit): Unit = {
    Try {
      eventHandleFunc(entityId)
    } match {
      case Success(x) =>
        logger.info(s"The event: $event has been processed")
        leaderLatch.succeed()
      case Failure(e: FatalException) =>
        logger.warn("An exception: \"" + s"${e.getMessage}" +
          "\" occurred during the event: \"" + s"$event" + "\" processing is fatal, " +
          "the processing of event will be restarted after 2 seconds")
        Thread.sleep(2000)
        runEventHandle(leaderLatch, event, entityId, eventHandleFunc)
      case Failure(e: CriticalException) =>
        logger.warn("An exception: \"" + s"${e.getMessage}" +
          "\" occurred during the event: \"" + s"$event" + "\" processing is not fatal, so it is ignored")
        leaderLatch.succeed()
      case Failure(e: Throwable) =>
        logger.error(s"Unhandled exception was thrown: $e")
        leaderLatch.abort()
    }
  }
}
