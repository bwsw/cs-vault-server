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
class ZooKeeperService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val host = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperUrl)
  private val retryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.zooKeeperRetryDelay)

  def createNode(path: String, data: String): Unit = {
    logger.debug(s"createNode with path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createConnectionTask(host), retryDelay)

    Try {
      zooKeeper.create(path, data.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
    } match {
      case Success(x) => x
      case Failure(e: NodeExistsException) =>                                           //it need because after check a stat in isExistNode method, node could be created by another thread
        logger.warn("Node could not been created, bacause already it have been exist")
        throw e
      case Failure(e: Throwable) =>
        TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createNodeCreationTask(zooKeeper, path, data), retryDelay)
    }
    logger.debug(s"Node was successfully created by path: $path")
    TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
  }

  def getData(path: String): String = {
    logger.debug(s"getData from path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createConnectionTask(host), retryDelay)
    val data = Try {
      new String(zooKeeper.getData(path, false, null), "UTF-8")
    } match {
      case Success(stringData) =>
        TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
        stringData
      case Failure(e: NoNodeException) =>                                               //it need because after check a stat in isExistNode method, node could be deleted by another thread
        logger.warn("Data could not been got, bacause node is have not existed")
        throw e
      case Failure(e: Throwable) =>
        TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createGetDataTask(zooKeeper, path), retryDelay)
    }
    logger.debug(s"Node data was successfully got by path: $path")
    TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
    data
  }

  def deleteNode(path: String): Unit = {
    logger.debug(s"deleteNode with path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createConnectionTask(host), retryDelay)
    TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createNodeDeletionTask(zooKeeper, path), retryDelay)
    TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
  }

  def isExistNode(path: String): Boolean = {
    logger.debug(s"isExistNode by path: $path")
    val zooKeeper: ZooKeeper = TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createConnectionTask(host), retryDelay)
    val isExist = TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createCheckExistNodeTask(zooKeeper, path), retryDelay)
    logger.debug(s"Node is exists: $isExist")
    TaskRunner.tryRunUntilSuccess(ZooKeeperTaskCreator.createCloseConnectionTask(zooKeeper), retryDelay)
    isExist
  }

}
