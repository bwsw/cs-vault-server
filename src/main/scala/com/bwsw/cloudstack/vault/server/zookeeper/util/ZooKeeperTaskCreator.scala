package com.bwsw.cloudstack.vault.server.zookeeper.util

import org.apache.zookeeper.data.Stat
import org.apache.zookeeper._
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 21.08.17.
  */
object ZooKeeperTaskCreator {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createConnectionTask(host: String)(): ZooKeeper = {
    logger.debug(s"createConnectionTask with host: $host")
    val zooKeeper = new ZooKeeper(host, 5000, null)
    logger.debug(s"Сonnection was been successfully create to host: $host")
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
    new String(zooKeeper.getData(path, false, null), "UTF-8")
  }

  def createNodeCreationTask(zooKeeper: ZooKeeper, path: String, data: String)(): Unit = {
    logger.debug(s"createNodeCreationTask with path: $path")
    zooKeeper.create(path, data.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
  }

  def createCheckExistNodeTask(zooKeeper: ZooKeeper, path: String)(): Boolean = {
    logger.debug(s"createCheckExistNodeTask from path: $path")
    zooKeeper.exists(path, false) != null
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
