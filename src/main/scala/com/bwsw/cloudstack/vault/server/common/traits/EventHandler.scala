package com.bwsw.cloudstack.vault.server.common.traits

import scala.concurrent.Future

/**
  * Created by medvedev_vv on 02.08.17.
  */
trait EventHandler[T] {
  def handleEventsFromRecords(records: List[String]): List[(Future[Unit], T)]
  def restartEvent(event: T): (Future[Unit], T)
  def isNonFatalException(exception: Throwable): Boolean
}
