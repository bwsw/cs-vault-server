package com.bwsw.cloudstack.vault.server.zookeeper.util

import com.bwsw.cloudstack.vault.server.MockConfig.zooKeeperTaskCreatorSettings
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperCriticalException
import org.apache.zookeeper.KeeperException.ConnectionLossException
import org.apache.zookeeper.{CreateMode, ZooKeeper}
import org.apache.zookeeper.data.{ACL, Stat}
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 31.08.17.
  */
class ZooKeeperTaskCreatorTestSuite extends FlatSpec with BaseTestSuite {

  //Positive tests
  "createGetDataTask" should "create request for get data from node by specified path" in {
    val expectedPath = "test/path"
    val expectedData = "dataBytes"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def getData(path: String, watch: Boolean, stat: Stat): Array[Byte] = {
          assert(path == expectedPath, "path is wrong")
          expectedData.getBytes
        }
      }
    }

    val data = zooKeeperTaskCreator.createGetDataTask(expectedPath)()

    assert(expectedData == data)
  }

  "createNodeCreationTask" should "create request for node creation with data" in {
    val expectedPath = "test/path"
    val expectedData = "dataString"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def create(path: String, data: Array[Byte], acl: java.util.List[ACL], createMode: CreateMode): String = {
          assert(path == expectedPath, "path is wrong")
          expectedData
        }
      }
    }

    assert(zooKeeperTaskCreator.createNodeCreationTask(expectedPath, expectedData)().isInstanceOf[Unit])
  }

  "createCheckExistNodeTask" should "create request for a non-existent node" in {
    val expectedPath = "test/path"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def exists(path: String, watch: Boolean): Stat = {
          assert(path == expectedPath, "path is wrong")
          null
        }
      }
    }

    assert(!zooKeeperTaskCreator.createCheckExistNodeTask(expectedPath))
  }

  "createCheckExistNodeTask" should "create request for an existing node" in {
    val expectedPath = "test/path"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def exists(path: String, watch: Boolean): Stat = {
          assert(path == expectedPath, "path is wrong")
          new Stat
        }
      }
    }

    assert(zooKeeperTaskCreator.createCheckExistNodeTask(expectedPath))
  }

  "createNodeDeletionTask" should "create request for node deletion" in {
    val expectedPath = "test/path"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def exists(path: String, watch: Boolean): Stat = {
          assert(path == expectedPath, "path is wrong")
          new Stat
        }

        override def delete(path: String, version: Int): Unit = {
          assert(path == expectedPath, "path is wrong")
        }
      }
    }

    assert(zooKeeperTaskCreator.createNodeDeletionTask(expectedPath).isInstanceOf[Unit])
  }

  "createNodeDeletionTask" should "do not create request for node deletion if node is non-existent" in {
    val expectedPath = "test/path"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def exists(path: String, watch: Boolean): Stat = {
          assert(path == expectedPath, "path is wrong")
          null
        }

        override def delete(path: String, version: Int): Unit = {
          throw new Exception("Impossible exception")
        }
      }
    }

    assert(zooKeeperTaskCreator.createNodeDeletionTask(expectedPath).isInstanceOf[Unit])
  }

  //Negative tests
  "createGetDataTask" should "if ZooKeeper throw not same with ConnectionLossException, " +
    "the exception will wrapped to ZooKeeperCriticalException" in {
    val expectedPath = "test/path"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def getData(path: String, watch: Boolean, stat: Stat): Array[Byte] = {
          throw new Exception("test exception")
        }
      }
    }

    assertThrows[ZooKeeperCriticalException] {
      zooKeeperTaskCreator.createGetDataTask(expectedPath)()
    }
  }

  "createNodeCreationTask" should  "if ZooKeeper throw ConnectionLossException, exception does not catch" in {
    val expectedPath = "test/path"
    val expectedData = "dataString"

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(zooKeeperTaskCreatorSettings) {
      override def createConnection(): ZooKeeper = new ZooKeeper(zooKeeperTaskCreatorSettings.host, 5000, null) {
        override def create(path: String, data: Array[Byte], acl: java.util.List[ACL], createMode: CreateMode): String = {
          throw new ConnectionLossException
        }
      }
    }

    assertThrows[ConnectionLossException] {
      zooKeeperTaskCreator.createNodeCreationTask(expectedPath, expectedData)()
    }
  }
}


