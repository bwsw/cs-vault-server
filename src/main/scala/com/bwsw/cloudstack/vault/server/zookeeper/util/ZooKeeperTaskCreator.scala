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
package com.bwsw.cloudstack.vault.server.zookeeper.util

import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.apache.zookeeper.KeeperException.ConnectionLossException
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for wrapping ZooKeeper library methods.
  *
  * @param settings contains the settings for interaction with ZooKeeper
  */
class ZooKeeperTaskCreator(settings: ZooKeeperTaskCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Creates task for getting data from zNode in ZooKeeper server.
    *
    * @param path String with path of zNode
    * @throws ZooKeeperCriticalException if exception was thrown by ZooKeeper library method
    * @throws ConnectionLossException if ZooKeeper server is unavailable
    */
  def createGetDataTask(path: String)(): String = {
    logger.debug(s"createGetDataTask from path: $path")

    def request() = {
      val zooKeeper = createConnection()
      val data = new String(zooKeeper.getData(path, false, null), "UTF-8")
      logger.debug(s"Node data was successfully got by path: $path")
      closeConnection(zooKeeper)
      data
    }

    handleZookeeperRequest[String](request)
  }

  /**
    * Creates task for creating zNode in ZooKeeper server.
    *
    * @param path String with path of zNode
    * @param data String with data for zNode
    * @throws ZooKeeperCriticalException if exception was thrown by ZooKeeper library method
    * @throws ConnectionLossException if ZooKeeper server is unavailable
    */
  def createNodeCreationTask(path: String, data: String)(): Unit = {
    logger.debug(s"createNodeCreationTask with path: $path")

    def request() = {
      val zooKeeper = createConnection()
      zooKeeper.create(path, data.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
      logger.debug(s"Node was successfully created by path: $path")
      closeConnection(zooKeeper)
    }

    handleZookeeperRequest[Unit](request)
  }

  /**
    * Creates task for check existing zNode in ZooKeeper server.
    *
    * @param path String with path of zNode
    * @throws ZooKeeperCriticalException if exception was thrown by ZooKeeper library method
    * @throws ConnectionLossException if ZooKeeper server is unavailable
    */
  def createCheckExistNodeTask(path: String)(): Boolean = {
    logger.debug(s"createCheckExistNodeTask from path: $path")

    def request() = {
      val zooKeeper = createConnection()
      val isExist = zooKeeper.exists(path, false) != null
      logger.debug(s"Node is exists: $isExist")
      closeConnection(zooKeeper)
      isExist
    }

    handleZookeeperRequest[Boolean](request)
  }

  /**
    * Creates task for deletion zNode in ZooKeeper server.
    *
    * @param path String with path of zNode
    * @throws ZooKeeperCriticalException if exception was thrown by ZooKeeper library method
    * @throws ConnectionLossException if ZooKeeper server is unavailable
    */
  def createNodeDeletionTask(path: String)(): Unit = {
    logger.debug(s"createNodeDeletionTask by path: $path")

    def request() = {
      val zooKeeper = createConnection()
      val stat: Stat = zooKeeper.exists(path, true)
      if (stat != null) {
        zooKeeper.delete(path, stat.getVersion)
        logger.debug(s"Node was successfully deleted by: $path")
      } else {
        logger.warn(s"Node does not exist by path: $path")
      }
      closeConnection(zooKeeper)
    }
    handleZookeeperRequest[Unit](request)
  }

  /**
    * Creates connection with ZooKeeper server.
    */
  protected def createConnection(): ZooKeeper = {
    logger.debug(s"createConnection with host: ${settings.host}")
    new ZooKeeper(settings.host, 5000, (event: WatchedEvent) => {})
  }

  /**
    * Closes connection with ZooKeeper server.
    */
  protected def closeConnection(zooKeeper: ZooKeeper): Unit = {
    logger.debug("createCloseConnectionTask")
    if (zooKeeper != null) {
      zooKeeper.close()
      logger.debug("The connection was been close")
    } else {
      logger.debug("The connection was been null")
    }
  }

  /**
    * Handles exceptions which might be thrown while request is executing.
    */
  private def handleZookeeperRequest[T](request:() => T): T  = {
    Try {
      request()
    } match {
      case Success(x) => x
      case Failure(e: ConnectionLossException) =>
        logger.warn("ZooKeeper server is unavailable")
        throw e
      case Failure(e: Throwable) =>
        throw new ZooKeeperCriticalException(e)
    }
  }
}

object ZooKeeperTaskCreator {
  case class Settings(host: String)
}
