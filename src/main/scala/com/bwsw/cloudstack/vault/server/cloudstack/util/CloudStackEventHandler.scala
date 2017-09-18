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
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
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
  * Class is responsible for handling cloudstack event.
  *
  * @param controller allows for logic executing from event
  */
class CloudStackEventHandler(controller: CloudStackVaultController)
                            (implicit executionContext: ExecutionContext) extends EventHandler[CloudStackEvent] {
  private val jsonSerializer = new JsonSerializer(true)
  private val logger = LoggerFactory.getLogger(this.getClass)

  @Override
  def handleEventsFromRecords(records: List[String]): Set[(Future[Unit], CloudStackEvent)] = {
    logger.debug(s"handleEventsFromRecords: $records")
    records.map { record =>
      Try {
        jsonSerializer.deserialize[CloudStackEvent](record)
      } match {
        case Success(x) => x
        case Failure(e: JsonParseException) =>
          logger.warn("Can not to parse record json, the empty CloudStackEvent will be return")
          CloudStackEvent(None, None, None)
        case Failure(e: Throwable) =>
          logger.error("Exception was thrown while record was being deserialized")
          throw e
      }
    }.toSet.collect(handleEvent)
  }

  @Override
  def restartEvent(event: CloudStackEvent): (Future[Unit], CloudStackEvent) = {
    logger.debug(s"restartEvent: $event")
    handleEvent(event)
  }

  private val handleEvent = new PartialFunction[CloudStackEvent, (Future[Unit], CloudStackEvent)] {
    override def apply(event: CloudStackEvent): (Future[Unit], CloudStackEvent) = {
      event.action.get match {
        case VMCreate =>
          logger.info(s"handle VMCreate event: $event")
          (Future(controller.handleVmCreate(event.entityuuid.get)), event)
        case VMDelete =>
          logger.info(s"handle VMDelete event: $event")
          (Future(controller.handleVmDelete(event.entityuuid.get)), event)
        case AccountCreate =>
          logger.info(s"handle AccountCreate event: $event")
          (Future(controller.handleAccountCreate(event.entityuuid.get)), event)
        case AccountDelete =>
          logger.info(s"handle AccountDelete event: $event")
          (Future(controller.handleAccountDelete(event.entityuuid.get)), event)
        case UserCreate =>
          logger.info(s"handle UserCreate event: $event")
          (Future(controller.handleUserCreate(event.entityuuid.get)), event)
      }
    }

    override def isDefinedAt(event: CloudStackEvent): Boolean = {
      if (event.entityuuid.isEmpty) {
        false
      } else {
        event.action match {
          case Some(action) if action.oneOf(AccountCreate, AccountDelete, UserCreate, VMCreate, VMDelete) =>
            event.status.getOrElse(Other) == CloudStackEvent.Status.Completed                              //Event must be handle when status of event Completed
          case _ =>                                                                                        //but first event have a signature such as {"details":"...","status":"Completed","event":"..."}
            false                                                                                          //and don't have an entityuuid, so we must to check entityuuid.
        }
      }
    }
  }

  def isNonFatalException(exception: Throwable): Boolean = {
    logger.debug(s"isNonFatalException: $exception")
    exception match {
      case e: CriticalException =>
        e.exception match {
          case nonFatalException: CloudStackEntityDoesNotExistException => true
          case _ => false
        }
      case _ => false
    }
  }
}
