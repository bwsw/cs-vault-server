package com.bwsw.cloudstack.vault.server.zookeeper.util

import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.apache.zookeeper.KeeperException.{ConnectionLossException, NoNodeException, NodeExistsException}
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 21.08.17.
  */
class ZooKeeperTaskCreator(settings: ZooKeeperTaskCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createConnectionTask(): ZooKeeper = {
    logger.debug(s"createConnectionTask with host: ${settings.host}")
    val zooKeeper = new ZooKeeper(settings.host, 5000, null)
    logger.debug(s"Сonnection was been successfully create to host: ${settings.host}")
    zooKeeper
  }

  def createCloseConnectionTask(zooKeeper: ZooKeeper)(): Unit = {
    logger.debug("createCloseConnectionTask")
    if (zooKeeper != null) {
      zooKeeper.close()
      logger.debug("The connection was been close")
    } else {
      logger.debug("The connection was been null")
    }
  }

  def createGetDataTask(zooKeeper: ZooKeeper, path: String)(): String = {
    logger.debug(s"createGetDataTask from path: $path")
    Try {
      new String(zooKeeper.getData(path, false, null), "UTF-8")
    } match {
      case Success(x) =>
        logger.debug(s"Node data was successfully got by path: $path")
        x
      case Failure(e: ConnectionLossException) =>
        logger.warn("ZooKeeper server is unavailable")
        throw e
      case Failure(e: Throwable) =>
        throw new ZooKeeperCriticalException(e)
    }
  }

  def createNodeCreationTask(zooKeeper: ZooKeeper, path: String, data: String)(): Unit = {
    logger.debug(s"createNodeCreationTask with path: $path")
    Try {
      zooKeeper.create(path, data.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
    } match {
      case Success(x) =>
        logger.debug(s"Node was successfully created by path: $path")
        x
      case Failure(e: ConnectionLossException) =>
        logger.warn("ZooKeeper server is unavailable")
        throw e
      case Failure(e: Throwable) =>
        throw new ZooKeeperCriticalException(e)
    }
  }

  def createCheckExistNodeTask(zooKeeper: ZooKeeper, path: String)(): Boolean = {
    logger.debug(s"createCheckExistNodeTask from path: $path")
    val isExist = zooKeeper.exists(path, false) != null
    logger.debug(s"Node is exists: $isExist")
    isExist
  }

  def createNodeDeletionTask(zooKeeper: ZooKeeper, path: String)(): Unit = {
    logger.debug(s"createNodeDeletionTask by path: $path")
    val stat: Stat = zooKeeper.exists(path, true)
    if (stat != null) {
      zooKeeper.delete(path, stat.getVersion)
      logger.debug(s"Node was successfully deleted by: $path")
    } else {
      logger.warn(s"Node does not exist by path: $path")
    }
  }
}

object ZooKeeperTaskCreator {
  case class Settings(host: String)
}
