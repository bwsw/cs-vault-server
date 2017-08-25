package com.bwsw.cloudstack.vault.server.util

import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 03.08.17.
  */
object TaskRunner {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def tryRunUntilSuccess[T](task: () => T,
                            isCriticalException: (Throwable) => Boolean,
                            retryDelay: Int): T = {
    Try {
      task()
    } match {
      case Success(x) => x
      case Failure(e) if isCriticalException(e) =>
        logger.error(s"The critical exception: $e was thrown")
        throw e
      case Failure(e) =>
        logger.warn(s"The task execute with an exception: $e, restart function after $retryDelay seconds")
        Thread.sleep(retryDelay)
        tryRunUntilSuccess[T](task, isCriticalException, retryDelay)
    }
  }
}
