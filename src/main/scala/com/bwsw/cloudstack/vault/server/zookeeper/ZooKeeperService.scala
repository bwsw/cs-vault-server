package com.bwsw.cloudstack.vault.server.zookeeper

import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, TaskRunner}
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator
import org.apache.zookeeper.KeeperException.{NoNodeException, NodeExistsException}
import org.apache.zookeeper.{CreateMode, ZooDefs, ZooKeeper}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 21.08.17.
  */
class ZooKeeperService(zooKeeperTaskCreator: ZooKeeperTaskCreator,
                       settings: ZooKeeperService.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val retryDelay = settings.retryDelay

  /**
    * Creates zNode in ZooKeeper server.
    * Throws ZooKeeperCriticalException if zNode already does exist.
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    * @param data String with data of zNode
    */
  def createNodeWithData(path: String, data: String): Unit = {
    logger.debug(s"createNode with path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createConnectionTask, retryDelay)

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createNodeCreationTask(zooKeeper, path, data), retryDelay)

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
  }

  /**
    * Gets data from zNode in ZooKeeper server.
    * Throws ZooKeeperCriticalException if zNode already does not exist.
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path to zNode data
    *
    * @return String with data stored in the zNode
    */
  def getData(path: String): String = {
    logger.debug(s"getData from path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createConnectionTask, retryDelay)

    val data = TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createGetDataTask(zooKeeper, path), retryDelay)

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
    data
  }

  /**
    * Deletes data from zNode in ZooKeeper server.
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    */
  def deleteNode(path: String): Unit = {
    logger.debug(s"deleteNode with path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createConnectionTask, retryDelay)

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createNodeDeletionTask(zooKeeper, path), retryDelay)

    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
  }

  /**
    * Checks zNode existing
    * Will be restarted if zookeeper server is unavailable.
    *
    * @param path String with path of zNode
    */
  def isExistNode(path: String): Boolean = {
    logger.debug(s"isExistNode by path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createConnectionTask, retryDelay)

    val isExist = TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createCheckExistNodeTask(zooKeeper, path), retryDelay)
    TaskRunner.tryRunUntilSuccess(zooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
    isExist
  }

}

object ZooKeeperService {
  case class Settings(retryDelay: Int)
}
