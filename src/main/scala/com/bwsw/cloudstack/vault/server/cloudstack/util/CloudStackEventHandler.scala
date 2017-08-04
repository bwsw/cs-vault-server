package com.bwsw.cloudstack.vault.server.cloudstack.util

import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.common.traits.EventHandler
import com.bwsw.cloudstack.vault.server.controllers.CSVaultController
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import CloudStackEvent.Action._

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackEventHandler(controller: CSVaultController)
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
        case VMCreate => (Future(logger.info("Controller.VMCreate")), event)
        case VMDelete => (Future(logger.info("Controller.VMDelete")), event)
        case AccountCreate => (Future(logger.info("Controller.AccountCreate")), event)
        case AccountDelete => (Future(logger.info("Controller.AccountDelete")), event)
        case UserCreate => (Future(logger.info("Controller.UserCreate")), event)
      }
    }

    override def isDefinedAt(event: CloudStackEvent): Boolean = {
      event.action match {
        case VMCreate | VMDelete | AccountCreate | AccountDelete | UserCreate => true
        case Other =>
          logger.debug("Unknown event")
          false
      }
    }
  }
}
