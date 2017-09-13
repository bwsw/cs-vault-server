package com.bwsw.cloudstack.vault.server.common

import java.util.UUID

import org.apache.curator.CuratorZookeeperClient
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.test.TestingServer
import org.apache.zookeeper.ZooKeeper
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Outcome}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LeaderLatchTestSuite extends FlatSpec with Matchers with BeforeAndAfterAll {

  val server = new TestingServer(true)
  val connectString = server.getConnectString
  val client = new CuratorZookeeperClient(connectString, 5000, 5000, null, new RetryOneTime(1000))
  client.start()
  client.blockUntilConnectedOrTimedOut()
  val zooKeeper = client.getZooKeeper

  val timeLimitPerTest = 4.second
  val halfTimeLimitPerTest = timeLimitPerTest.toMillis / 2
  val updatingTimeout = timeLimitPerTest.toMillis / 10

  override def withFixture(test: NoArgTest): Outcome = {
    // TimeLimitedTest does not work in some cases (e.g. infinite loop)
    Await.result(Future(super.withFixture(test)), timeLimitPerTest)
  }


  "LeaderLatch" should "create master node on ZK server" in {
    val masterNode = newMasterNode
    val leaderLatchId = UUID.randomUUID().toString

    zooKeeper.exists(masterNode, false) shouldBe null

    new LeaderLatch(connectString, masterNode, leaderLatchId)

    zooKeeper.exists(masterNode, false) should not be null
  }

  it should "not be a leader if it hasn't been started" in {
    val masterNode = newMasterNode
    val leaderLatchId = UUID.randomUUID().toString
    val leaderLatch = new LeaderLatch(connectString, masterNode, leaderLatchId)

    leaderLatch.hasLeadership() shouldBe false
  }

  it should "acquire leadership if there is only one participant of the master node" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchId = UUID.randomUUID().toString

    val leaderLatch = new LeaderLatch(connectString, masterNode, leaderLatchId)
    NodeInfo(masterNode, zooKeeper).children shouldBe empty

    leaderLatch.start()
    leaderLatch.acquireLeadership(delay)

    val nodeInfo = NodeInfo(masterNode, zooKeeper)
    nodeInfo.children.size shouldBe 1
    nodeInfo.children.head.data shouldBe leaderLatchId
    leaderLatch.hasLeadership() shouldBe true
  }

  it should "not acquire leadership if it hasn't been started" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchId = UUID.randomUUID().toString

    val leaderLatch = new LeaderLatch(connectString, masterNode, leaderLatchId)
    val future = Future(leaderLatch.acquireLeadership(delay))

    Thread.sleep(halfTimeLimitPerTest)

    future.isCompleted shouldBe false
  }

  it should "return its ID if it acquires leadership" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchId = UUID.randomUUID().toString
    val leaderLatch = new LeaderLatch(connectString, masterNode, leaderLatchId)

    leaderLatch.start()
    leaderLatch.acquireLeadership(delay)

    leaderLatch.getLeaderInfo() shouldBe leaderLatchId
  }

  it should "delete its node from a server after closing" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchId = UUID.randomUUID().toString
    val leaderLatch = new LeaderLatch(connectString, masterNode, leaderLatchId)

    leaderLatch.start()
    leaderLatch.acquireLeadership(delay)
    leaderLatch.close()

    Thread.sleep(updatingTimeout)
    val nodeInfo = NodeInfo(masterNode, zooKeeper)

    nodeInfo.children.size shouldBe 0
  }

  it should "not acquire leadership until there is another leader" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchCount = 3
    val leaderLatchIds = Seq.fill(leaderLatchCount)(UUID.randomUUID().toString)
    val leaderLatches = leaderLatchIds.map(id => id -> new LeaderLatch(connectString, masterNode, id)).toMap
    leaderLatches.values.head.start()
    leaderLatches.values.head.acquireLeadership(delay)

    leaderLatches.values.tail.foreach(_.start())

    val leaderId = leaderLatches.values.head.getLeaderInfo()
    val notLeader = (leaderLatches - leaderId).values.head
    val future = Future(notLeader.acquireLeadership(delay))
    Thread.sleep(halfTimeLimitPerTest)

    future.isCompleted shouldBe false
  }


  "LeaderLatch instances" should "return leader ID if it hasn't been started" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchCount = 3
    val leaderLatchIds = Seq.fill(leaderLatchCount)(UUID.randomUUID().toString)

    val leaderLatches = leaderLatchIds.map(id => new LeaderLatch(connectString, masterNode, id))
    leaderLatches.head.start()
    leaderLatches.head.acquireLeadership(delay)

    leaderLatches.foreach { leaderLatch =>
      leaderLatch.getLeaderInfo() shouldBe leaderLatchIds.head
    }
  }

  it should "return the same leader ID" in {
    val masterNode = newMasterNode
    val leaderLatchCount = 3
    val leaderLatchIds = Seq.fill(leaderLatchCount)(UUID.randomUUID().toString)

    val leaderLatches = leaderLatchIds.map(id => new LeaderLatch(connectString, masterNode, id))
    leaderLatches.foreach(_.start())

    Thread.sleep(updatingTimeout)

    val leaderIdSet = leaderLatches.map(_.getLeaderInfo()).toSet
    leaderIdSet.size shouldBe 1
    leaderIdSet.foreach { leaderId =>
      leaderLatchIds should contain(leaderId)
    }
  }

  it should "elect a new leader if current leader has been stopped" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchCount = 3
    val leaderLatchIds = Seq.fill(leaderLatchCount)(UUID.randomUUID().toString)
    val leaderLatches = leaderLatchIds.map(id => id -> new LeaderLatch(connectString, masterNode, id)).toMap
    leaderLatches.values.head.start()
    leaderLatches.values.head.acquireLeadership(delay)

    leaderLatches.values.tail.foreach(_.start())

    val oldLeaderId = leaderLatches.values.head.getLeaderInfo()
    leaderLatches(oldLeaderId).close()

    Thread.sleep(updatingTimeout)

    val leaderLatchesWithoutOldLeader = leaderLatches - oldLeaderId
    val newLeaderId = leaderLatchesWithoutOldLeader.values.head.getLeaderInfo()

    newLeaderId should not be oldLeaderId
    leaderLatchIds should contain(newLeaderId)
    leaderLatchesWithoutOldLeader.values.foreach { leaderLatch =>
      leaderLatch.getLeaderInfo() shouldBe newLeaderId
    }
  }

  override def afterAll(): Unit = {
    client.close()
    server.close()
  }


  case class NodeInfo(node: String, data: String, children: Seq[NodeInfo])

  object NodeInfo {
    def apply(node: String, zooKeeper: ZooKeeper): NodeInfo = {
      new NodeInfo(
        node = node,
        data = new String(zooKeeper.getData(node, null, null)),
        children = zooKeeper.getChildren(node, null).asScala.map(c => apply(s"$node/$c", zooKeeper)))
    }
  }

  def newMasterNode: String =
    "/leader-latch/test/" + UUID.randomUUID().toString
}