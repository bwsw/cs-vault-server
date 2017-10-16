package com.bwsw.cloudstack.vault.server.zookeeper

import com.bwsw.cloudstack.vault.server.MockConfig._
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperFatalException
import org.apache.curator.test.TestingServer
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

class ZooKeeperServiceTestSuite extends FlatSpec with BaseTestSuite with BeforeAndAfterAll {
  val start = true
  val server = new TestingServer(9000, start)
  val connectString = server.getConnectString

  val path = "/test/path"
  val expectedData = "expectedData"
  val zooKeeperService = new ZooKeeperService(zooKeeperServiceSettings.copy(connectString)) {
  }

  "createNodeWithData" should "create node with data" in {
    assert(zooKeeperService.createNodeWithData(path, expectedData).isInstanceOf[Unit])
    zooKeeperService.deleteNode(path)
  }

  "createNodeWithData" should "should throw ZooKeeperCriticalException" in {
    zooKeeperService.createNodeWithData(path, expectedData).isInstanceOf[Unit]
    assertThrows[ZooKeeperFatalException] {
      zooKeeperService.createNodeWithData(path, expectedData)
    }
    zooKeeperService.deleteNode(path)
  }

  "doesNodeExist" should "return true if node exist" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.doesNodeExist(path))
    zooKeeperService.deleteNode(path)
  }

  "getNodeData" should "get data from node" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.getNodeData(path).get == expectedData)
    zooKeeperService.deleteNode(path)
  }

  "deleteNode" should "delete node" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.deleteNode(path).isInstanceOf[Unit])
  }

  "deleteNode" should "should throw ZooKeeperCriticalException" in {
    assertThrows[ZooKeeperFatalException] {
      zooKeeperService.deleteNode(path)
    }
  }

  "getNodeData" should "return None if node does not exist" in {
    val actualData = zooKeeperService.getNodeData(path)
    assert(actualData.isEmpty)
  }

  "doesNodeExist" should "return false if node does not exist" in {
    assert(!zooKeeperService.doesNodeExist(path))
  }

  override def afterAll(): Unit = {
    zooKeeperService.close()
    server.close()
  }
}
