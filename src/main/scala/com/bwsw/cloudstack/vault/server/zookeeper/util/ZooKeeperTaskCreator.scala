package com.bwsw.cloudstack.vault.server.zookeeper.util

import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.apache.zookeeper.KeeperException.ConnectionLossException
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 21.08.17.
  */
class ZooKeeperTaskCreator(settings: ZooKeeperTaskCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)

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

  protected def createConnection(): ZooKeeper = {
    logger.debug(s"createConnection with host: ${settings.host}")
    new ZooKeeper(settings.host, 5000, (event: WatchedEvent) => {})
  }

  protected def closeConnection(zooKeeper: ZooKeeper): Unit = {
    logger.debug("createCloseConnectionTask")
    if (zooKeeper != null) {
      zooKeeper.close()
      logger.debug("The connection was been close")
    } else {
      logger.debug("The connection was been null")
    }
  }

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
