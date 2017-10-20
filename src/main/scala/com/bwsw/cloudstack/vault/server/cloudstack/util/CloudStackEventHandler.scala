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

import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.common.{JsonSerializer, ProcessingEventResult}
import com.bwsw.cloudstack.vault.server.common.traits.EventHandler
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import CloudStackEvent.Action._
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackEntityDoesNotExistException
import com.bwsw.cloudstack.vault.server.util.exception.CriticalException
import com.fasterxml.jackson.core.JsonParseException

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for processing CloudStack events.
  *
  * @param controller enables logic execution from event
  */
class CloudStackEventHandler(controller: CloudStackVaultController)
                            (implicit executionContext: ExecutionContext) extends EventHandler[CloudStackEvent] {
  private val jsonSerializer = new JsonSerializer(ignoreUnknownProperties = true)
  private val logger = LoggerFactory.getLogger(this.getClass)

  @Override
  def handleEventsFromRecords(records: List[String]): Set[ProcessingEventResult[CloudStackEvent]] = {
    logger.debug(s"handleEventsFromRecords: $records")
    records.map { record =>
      Try {
        jsonSerializer.deserialize[CloudStackEvent](record)
      } match {
        case Success(x) => x
        case Failure(e: JsonParseException) =>
          logger.warn("Can not parse the record: \"" + s"$record" + "\", the empty CloudStackEvent will be returned")
          CloudStackEvent(None, None, None)
        case Failure(e: Throwable) =>
          logger.error(s"Exception $e occurred during the deserialization of the record: " + "\"" + s"$record" + "\"")
          throw e
      }
    }.toSet.collect(handleEvent)
  }

  @Override
  def restartEvent(event: CloudStackEvent): ProcessingEventResult[CloudStackEvent] = {
    logger.debug(s"restartEvent: $event")
    handleEvent(event)
  }

  private val handleEvent = new PartialFunction[CloudStackEvent, ProcessingEventResult[CloudStackEvent]] {
    override def apply(event: CloudStackEvent): ProcessingEventResult[CloudStackEvent] = {
      event.action.get match {
        case VMCreate =>
          logger.info(s"handle VMCreate event: $event")
          ProcessingEventResult(event, Future(controller.handleVmCreate(event.entityuuid.get)))
        case VMDelete =>
          logger.info(s"handle VMDelete event: $event")
          ProcessingEventResult(event, Future(controller.handleVmDelete(event.entityuuid.get)))
        case AccountCreate =>
          logger.info(s"handle AccountCreate event: $event")
          ProcessingEventResult(event, Future(controller.handleAccountCreate(event.entityuuid.get)))
        case AccountDelete =>
          logger.info(s"handle AccountDelete event: $event")
          ProcessingEventResult(event, Future(controller.handleAccountDelete(event.entityuuid.get)))
        case UserCreate =>
          logger.info(s"handle UserCreate event: $event")
          ProcessingEventResult(event, Future(controller.handleUserCreate(event.entityuuid.get)))
      }
    }

    /**
      * Event is processed when status of event is Completed.
      * The first event has a signature such as {"details":"...","status":"Completed","event":"..."},
      * which does not contain an entityuuid field, so we have to check entityuuid.
      */
    override def isDefinedAt(event: CloudStackEvent): Boolean = {
      if (event.entityuuid.isEmpty) {
        false
      } else {
        event.action match {
          case Some(action) if action.oneOf(AccountCreate, AccountDelete, UserCreate, VMCreate, VMDelete) =>
            event.status.getOrElse(Other) == CloudStackEvent.Status.Completed
          case _ =>
            false
        }
      }
    }
  }
}
