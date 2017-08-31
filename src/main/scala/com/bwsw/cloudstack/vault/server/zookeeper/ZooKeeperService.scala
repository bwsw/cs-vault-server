package com.bwsw.cloudstack.vault.server.zookeeper

import com.bwsw.cloudstack.vault.server.util.TaskRunner
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 21.08.17.
  */
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
