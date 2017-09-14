package com.bwsw.cloudstack.vault.server.util

import com.bwsw.cloudstack.vault.server.util.exception.CriticalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 03.08.17.
  */
object TaskRunner {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def tryRunUntilSuccess[T](task: () => T,
                            retryDelay: Int): T = {
    Try {
      task()
    } match {
      case Success(x) => x
      case Failure(e: CriticalException) =>
        logger.error(s"The critical exception: ${e.exception.getStackTrace} was thrown")
        throw e
      case Failure(e) =>
        logger.warn(s"The task execute with an exception: ${e.getStackTrace}, restart function after $retryDelay seconds")
        Thread.sleep(retryDelay)
        tryRunUntilSuccess[T](task, retryDelay)
    }
  }
}
