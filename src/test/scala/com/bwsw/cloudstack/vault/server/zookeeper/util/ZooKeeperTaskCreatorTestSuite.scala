package com.bwsw.cloudstack.vault.server.zookeeper.util

import com.bwsw.cloudstack.vault.server.MockConfig.zooKeeperTaskCreatorSettings
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.apache.curator.CuratorZookeeperClient
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.test.TestingServer
import org.apache.curator.utils.ZookeeperFactory
import org.apache.zookeeper.KeeperException.ConnectionLossException
import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooKeeper}
import org.apache.zookeeper.data.{ACL, Stat}
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 31.08.17.
  */
class ZooKeeperTaskCreatorTestSuite extends FlatSpec with BaseTestSuite {
  val server = new TestingServer(true)
  val connectString = server.getConnectString
  var client: CuratorZookeeperClient = _

  //Positive tests
  "createGetDataTask" should "create request for get data from node by specified path" in {
    val expectedPath = "test/path"
    val expectedData = "dataBytes"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def getData(path: String, watch: Boolean, stat: Stat): Array[Byte] = {
        assert(path == expectedPath, "path is wrong")
        expectedData.getBytes
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    val data = zooKeeperTaskCreator.createGetDataTask(expectedPath)()
    assert(expectedData == data)
  }

  "createNodeCreationTask" should "create request for node creation with data" in {
    val expectedPath = "test/path"
    val expectedData = "dataString"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def create(path: String, data: Array[Byte], acl: java.util.List[ACL], createMode: CreateMode): String = {
        assert(path == expectedPath, "path is wrong")
        expectedData
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    assert(zooKeeperTaskCreator.createNodeCreationTask(expectedPath, expectedData)().isInstanceOf[Unit])
  }

  "createCheckExistNodeTask" should "create request for a non-existent node" in {
    val expectedPath = "test/path"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def exists(path: String, watch: Boolean): Stat = {
        assert(path == expectedPath, "path is wrong")
        null
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    assert(!zooKeeperTaskCreator.createCheckExistNodeTask(expectedPath))
  }

  "createCheckExistNodeTask" should "create request for an existing node" in {
    val expectedPath = "test/path"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def exists(path: String, watch: Boolean): Stat = {
        assert(path == expectedPath, "path is wrong")
        new Stat
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    assert(zooKeeperTaskCreator.createCheckExistNodeTask(expectedPath))
  }

  "createNodeDeletionTask" should "create request for node deletion" in {
    val expectedPath = "test/path"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def exists(path: String, watch: Boolean): Stat = {
        assert(path == expectedPath, "path is wrong")
        new Stat
      }

      override def delete(path: String, version: Int): Unit = {
        assert(path == expectedPath, "path is wrong")
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    assert(zooKeeperTaskCreator.createNodeDeletionTask(expectedPath).isInstanceOf[Unit])
  }

  "createNodeDeletionTask" should "do not create request for node deletion if node is non-existent" in {
    val expectedPath = "test/path"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def exists(path: String, watch: Boolean): Stat = {
        assert(path == expectedPath, "path is wrong")
        null
      }

      override def delete(path: String, version: Int): Unit = {
        throw new Exception("Impossible exception")
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    assert(zooKeeperTaskCreator.createNodeDeletionTask(expectedPath).isInstanceOf[Unit])
  }

  //Negative tests
  "createGetDataTask" should "if ZooKeeper throw not same with ConnectionLossException, " +
    "the exception will wrapped to ZooKeeperCriticalException" in {
    val expectedPath = "test/path"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def getData(path: String, watch: Boolean, stat: Stat): Array[Byte] = {
        throw new Exception("test exception")
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    assertThrows[ZooKeeperCriticalException] {
      zooKeeperTaskCreator.createGetDataTask(expectedPath)()
    }
    client.close()
  }

  "createNodeCreationTask" should  "if ZooKeeper throw ConnectionLossException, exception does not catch" in {
    val expectedPath = "test/path"
    val expectedData = "dataString"
    val expectedZooKeeper = new ZooKeeper(connectString, 5000, (event: WatchedEvent) => {}) {
      override def create(path: String, data: Array[Byte], acl: java.util.List[ACL], createMode: CreateMode): String = {
        throw new ConnectionLossException
      }
    }

    val zooKeeperTaskCreator = createZooKeeperTaskCreator(expectedZooKeeper)

    assertThrows[ConnectionLossException] {
      zooKeeperTaskCreator.createNodeCreationTask(expectedPath, expectedData)()
    }
    client.close()
  }

  private def createZooKeeperFactory(zooKeeper: ZooKeeper) = new ZookeeperFactory {
    override def newZooKeeper(connectString: String, sessionTimeout: Int, watcher: Watcher, canBeReadOnly: Boolean): ZooKeeper = {
      zooKeeper
    }
  }

  private def createZooKeeperTaskCreator(expectedZooKeeper: ZooKeeper) = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
    override def createConnection(): ZooKeeper = {
      val zooKeeperFactory = createZooKeeperFactory(expectedZooKeeper)
      client = new CuratorZookeeperClient(
        zooKeeperFactory,
        new FixedEnsembleProvider(connectString),
        5000,
        5000,
        (event: WatchedEvent) => {},
        new RetryOneTime(1000),
        false
      )
      client.start()
      client.blockUntilConnectedOrTimedOut()
      client.getZooKeeper
    }

    override def closeConnection(zooKeeper: ZooKeeper): Unit = {
      assert(zooKeeper == expectedZooKeeper, "zooKeeper is wrong")
      client.close()
    }
  }
}
