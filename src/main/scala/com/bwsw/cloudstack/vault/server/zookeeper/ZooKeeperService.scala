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
package com.bwsw.cloudstack.vault.server.zookeeper

import com.bwsw.cloudstack.vault.server.util.TaskRunner
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.slf4j.LoggerFactory

class ZooKeeperService(zooKeeperTaskCreator: ZooKeeperTaskCreator,
                       settings: ZooKeeperService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val retryDelay = settings.retryDelay

  /**
    * Creates zNode in ZooKeeper server.
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    * @param data String with data of zNode
    * @throws ZooKeeperCriticalException if zNode already does exist.
    */
  def createNodeWithData(path: String, data: String): Unit = {
    logger.debug(s"createNode with path: $path")

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createNodeCreationTask(path, data), retryDelay)
  }

  /**
    * Gets data from zNode in ZooKeeper server.
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path to zNode data
    *
    * @return String with data stored in the zNode
    * @throws ZooKeeperCriticalException if zNode already does not exist.
    */
  def getData(path: String): String = {
    logger.debug(s"getData from path: $path")

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createGetDataTask(path), retryDelay)
  }

  /**
    * Deletes data from zNode in ZooKeeper server.
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    */
  def deleteNode(path: String): Unit = {
    logger.debug(s"deleteNode with path: $path")

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createNodeDeletionTask(path), retryDelay)
  }

  /**
    * Checks zNode existing
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    */
  def isExistNode(path: String): Boolean = {
    logger.debug(s"isExistNode by path: $path")

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createCheckExistNodeTask(path), retryDelay)
  }
}

object ZooKeeperService {
  case class Settings(retryDelay: Int)
}
