package com.bwsw.cloudstack.vault.server.cloudstack.util

import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.common.traits.EventHandler
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import CloudStackEvent.Action._
import br.com.autonomiccs.apacheCloudStack.exceptions.ApacheCloudStackClientRequestRuntimeException
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackEntityDoesNotExistException
import com.bwsw.cloudstack.vault.server.util.exception.CriticalException

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackEventHandler(controller: CloudStackVaultController)
                            (implicit executionContext: ExecutionContext) extends EventHandler[CloudStackEvent] {
  private val jsonSerializer = new JsonSerializer(true)
  private val logger = LoggerFactory.getLogger(this.getClass)

  @Override
  def handleEventsFromRecords(records: List[String]): Set[(Future[Unit], CloudStackEvent)] = {
    logger.debug(s"handleEventsFromRecords: $records")
    records.map(jsonSerializer.deserialize[CloudStackEvent]).toSet.collect(handleEvent)
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
