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

import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryForever
import org.apache.zookeeper.{CreateMode, ZooDefs}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for launch task which are created by ZooKeeperTaskCreator.
  *
  * @param settings contains the settings for interaction with ZooKeeper
  */
class ZooKeeperService(settings: ZooKeeperService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val retryPolicy = new RetryForever(settings.retryDelay)
  protected val curatorClient: CuratorFramework = CuratorFrameworkFactory.newClient(settings.endpoints, retryPolicy)
  initCuratorClient()

  /**
    * Creates zNode in ZooKeeper server.
    * Will be waiting if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    * @param data String with data of zNode
    * @throws ZooKeeperCriticalException if zNode already does exist.
    */
  def createNodeWithData(path: String, data: String): Unit = {
    logger.debug(s"createNode with path: $path")

    Try {
      curatorClient
        .create()
        .creatingParentsIfNeeded()
        .withMode(CreateMode.PERSISTENT)
        .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
        .forPath(path, data.getBytes("UTF-8"))
    } match {
      case Success(x) =>
      case Failure(e: Throwable) =>
        throw new ZooKeeperCriticalException(e)
    }

  }

  /**
    * Gets data from zNode in ZooKeeper server.
    * Will be waiting if zookeeper server is unavailable.
    *
    * @param path String with path to zNode data
    *
    * @return Data as string stored in zNode if zNode exist
    *         None if zNode does not exist
    */
  def getNodeData(path: String): Option[String] = {
    logger.debug(s"getNodeData from path: $path")
    if (curatorClient.checkExists().forPath(path) == null) {
      None
    } else {
      Some(new String(curatorClient.getData.forPath(path), "UTF-8"))
    }
  }

  /**
    * Launches task for deletion data from zNode in ZooKeeper server.
    * Will be waiting if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    * @throws ZooKeeperCriticalException if node does not exist
    */
  def deleteNode(path: String): Unit = {
    logger.debug(s"deleteNode with path: $path")

    Try {
      curatorClient.delete().deletingChildrenIfNeeded().forPath(path)
    } match {
      case Success(x) =>
      case Failure(e: Throwable) =>
        throw new ZooKeeperCriticalException(e)
    }

  }

  /**
    * Launches task for check zNode existing
    * Will be waiting if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    *
    * @return boolean flag on the existence zNode
    */
  def doesNodeExist(path: String): Boolean = {
    logger.debug(s"doesNodeExist by path: $path")

    curatorClient.checkExists().forPath(path) != null
  }

  def close(): Unit = {
    curatorClient.close()
  }

  protected def initCuratorClient(): Unit = {
    curatorClient.start()
  }
}

object ZooKeeperService {
  case class Settings(endpoints: String, retryDelay: Int)
}
