package com.bwsw.cloudstack.vault.server.util

import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 03.08.17.
  */
object PeriodicRunner {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def runMethod[T](triggeredFunction: () => T,
                     timeBetweenRunning: Int): T = {
    Try {
      triggeredFunction()
    } match {
      case Success(x) => x
      case Failure(e) =>
        logger.warn(s"The method execute with an exception: $e, restart function after $timeBetweenRunning seconds")
        triggeredFunction()
    }
  }
}
