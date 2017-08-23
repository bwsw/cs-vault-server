package com.bwsw.cloudstack.vault.server.cloudstack.util

import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.common.traits.EventHandler
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import CloudStackEvent.Action._

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackEventHandler(controller: CloudStackVaultController)
                            (implicit executionContext: ExecutionContext) extends EventHandler[CloudStackEvent] {
  private val jsonSerializer = new JsonSerializer(true)
  private val logger = LoggerFactory.getLogger(this.getClass)

  @Override
  def handleEventsFromRecords(records: List[String]): List[(Future[Unit], CloudStackEvent)] = {
    records.map(jsonSerializer.deserialize[CloudStackEvent]).collect(handleEvent)
  }

  @Override
  def restartEvent(event: CloudStackEvent): (Future[Unit], CloudStackEvent) = {
    logger.debug(s"restartEvent: $event")
    handleEvent(event)
  }

  private val handleEvent = new PartialFunction[CloudStackEvent, (Future[Unit], CloudStackEvent)] {
    override def apply(event: CloudStackEvent): (Future[Unit], CloudStackEvent) = {
      event.action match {
        case VMCreate => (Future(controller.handleVmCreate(event.entityuuid)), event)
        case VMDelete => (Future(controller.handleVmDelete()), event)
        case AccountCreate => (Future(""), event)
        case AccountDelete => (Future(controller.handleAccountDelete()), event)
        case UserCreate => (Future(""), event)
      }
    }

    override def isDefinedAt(event: CloudStackEvent): Boolean = {
      if (event.entityuuid == null || event.eventDateTime == null) {
        false
      } else {
        event.action match {
          case AccountCreate | AccountDelete | UserCreate | VMCreate | VMDelete =>
            event.status == CloudStackEvent.Status.Completed && event.entityuuid != null  //Event must be handle when status of event Completed
          case _ =>                                                                       //but first event have a signature such as {"details":"...","status":"Completed","event":"..."}
            logger.debug("Unhandled event")                                               //and don't have an entityuuid, so we must to check entityuuid.
            false
        }
      }
    }
  }
}
