package com.bwsw.cloudstack.vault.server.zookeeper

import com.bwsw.cloudstack.vault.server.MockConfig._
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import org.apache.curator.CuratorZookeeperClient
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.test.TestingServer
import org.apache.zookeeper.KeeperException.{NoNodeException, NodeExistsException}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

/**
  * Created by medvedev_vv on 29.08.17.
  */
class ZooKeeperServiceTestSuite extends FlatSpec with BaseTestSuite with BeforeAndAfterAll {
  val server = new TestingServer(true)
  val connectString = server.getConnectString
  val client: CuratorFramework = CuratorFrameworkFactory.newClient(connectString, new RetryOneTime(1000))
  client.start()

  val path = "/test/path"
  val expectedData = "expectedData"
  val zooKeeperService = new ZooKeeperService(zooKeeperServiceSettings.copy(connectString)) {
  }

  "createNodeWithData" should "create node with data" in {
    assert(zooKeeperService.createNodeWithData(path, expectedData).isInstanceOf[Unit])
    zooKeeperService.deleteNode(path)
  }

  "createNodeWithData" should "should throw NodeExistException" in {
    zooKeeperService.createNodeWithData(path, expectedData).isInstanceOf[Unit]
    assertThrows[NodeExistsException] {
      zooKeeperService.createNodeWithData(path, expectedData)
    }
    zooKeeperService.deleteNode(path)
  }

  "isExistNode" should "return true if node exist" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.doesNodeExist(path))
    zooKeeperService.deleteNode(path)
  }

  "getDataIfNodeExist" should "get data from node" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.getNodeData(path).get == expectedData)
    zooKeeperService.deleteNode(path)
  }

  "deleteNode" should "delete node" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.deleteNode(path).isInstanceOf[Unit])
  }

  "deleteNode" should "should throw NoNodeException" in {
    assertThrows[NoNodeException] {
      zooKeeperService.deleteNode(path)
    }
  }

  "getDataIfNodeExist" should "return None if node does not exist" in {
    val actualData = zooKeeperService.getNodeData(path)
    assert(actualData.isEmpty)
  }

  "isExistNode" should "return false if node does not exist" in {
    assert(!zooKeeperService.doesNodeExist(path))
  }

  override def afterAll(): Unit = {
    client.close()
    server.close()
  }
}
