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
package com.bwsw.cloudstack.vault.server.util

import com.bwsw.cloudstack.vault.server.util.exception.CriticalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object TaskRunner {
  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Re-runs task while not it will be successfully executed or critical exception will be thrown
    *
    * @param task task for execution
    * @param retryDelay delay between task restarts
    *
    * @return object with type parametrized T
    */
  def tryRunUntilSuccess[T](task: () => T,
                            retryDelay: Int): T = {
    Try {
      task()
    } match {
      case Success(x) => x
      case Failure(e: CriticalException) =>
        logger.error(s"The critical exception: ${e.exception.getMessage} was thrown")
        throw e
      case Failure(e) =>
        logger.warn(s"The task execute with an exception: ${e.getMessage}, restart function after $retryDelay seconds")
        Thread.sleep(retryDelay)
        tryRunUntilSuccess[T](task, retryDelay)
    }
  }
}
