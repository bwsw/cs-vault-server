package com.bwsw.cloudstack.vault.server.common

import org.apache.curator.framework.recipes.leader
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.KeeperException
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

class LeaderLatch(zkServer: String, masterNode: String, id: String) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val curatorClient: CuratorFramework = createCuratorClient()
  createMasterNode()
  private val leaderLatch = new leader.LeaderLatch(curatorClient, masterNode, id)
  private var isStarted = false

  private def createCuratorClient(): CuratorFramework = {
    logger.debug(s"Create a curator client (connection: $zkServer).")
    val curatorClient = CuratorFrameworkFactory.newClient(zkServer, new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()
    curatorClient
  }

  private def createMasterNode(): Any = {
    logger.debug(s"Create a master node: $masterNode if it doesn't exist.")
    val doesPathExist = Option(curatorClient.checkExists().forPath(masterNode))
    if (doesPathExist.isEmpty) curatorClient.create.creatingParentsIfNeeded().forPath(masterNode)
  }

  def start(): Unit = {
    logger.info("Start a leader latch.")
    leaderLatch.start()
    isStarted = true
  }

  def acquireLeadership(delay: Long): Unit = {
    while (!hasLeadership) {
      logger.debug("Waiting until the leader latch acquires leadership.")
      Thread.sleep(delay)
    }
  }

  def getLeaderInfo(): String = {
    logger.debug("Get info of a leader.")
    var leaderInfo = getLeaderId()
    while (leaderInfo == "") {
      logger.debug("Waiting until the leader latch gets a leader info.")
      leaderInfo = getLeaderId()
      Thread.sleep(50)
    }

    leaderInfo
  }

  def hasLeadership(): Boolean = {
    leaderLatch.hasLeadership
  }

  @tailrec
  private def getLeaderId(): String = {
    logger.debug("Try to get a leader id.")
    Try(leaderLatch.getLeader.getId) match {
      case Success(leaderId) => leaderId
      case Failure(_: KeeperException) =>
        logger.debug("Waiting until the leader latch gets a leader id.")
        Thread.sleep(50)
        getLeaderId()
      case Failure(e) => throw e
    }
  }

  def close(): Unit = {
    logger.info("Close a leader latch if it's started.")
    if (isStarted) leaderLatch.close()
    curatorClient.close()
  }
}