package com.bwsw.cloudstack.vault.server.zookeeper

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator
import org.apache.zookeeper.ZooKeeper
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 29.08.17.
  */
class ZooKeeperServiceSuite extends FlatSpec with BaseTestSuite {
  val expectedPath = "test/path"
  val expectedData = "expectedData"

  "createNodeWithData" should "create node with data" in {
    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(settings.zooKeeperTaskCreatorSettings) {
      override def createNodeCreationTask(path: String, data: String)(): Unit = {
        assert(path == expectedPath, "path is wrong")
        assert(data == expectedData, "data is wrong")
      }
    }

    val zooKeeperService = new ZooKeeperService(zooKeeperTaskCreator, settings.zooKeeperServiceSettings)

    assert(zooKeeperService.createNodeWithData(expectedPath, expectedData).isInstanceOf[Unit])
  }

  "getData" should "return data from node" in {
    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(settings.zooKeeperTaskCreatorSettings) {
      override def createGetDataTask(path: String)(): String = {
        assert(expectedPath == path, "path is wrong")
        expectedData
      }
    }

    val zooKeeperService = new ZooKeeperService(zooKeeperTaskCreator, settings.zooKeeperServiceSettings)
    val actualData = zooKeeperService.getData(expectedPath)

    assert(actualData == expectedData)
  }

  "deleteNode" should "delete node" in {
    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(settings.zooKeeperTaskCreatorSettings) {
      override def createNodeDeletionTask(path: String)(): Unit = {
        assert(expectedPath == path, "path is wrong")
      }
    }

    val zooKeeperService = new ZooKeeperService(zooKeeperTaskCreator, settings.zooKeeperServiceSettings)

    assert(zooKeeperService.deleteNode(expectedPath).isInstanceOf[Unit])
  }

  "isExistNode" should "return Boolean value" in {
    val expectedIsExist = true

    val zooKeeperTaskCreator = new ZooKeeperTaskCreator(settings.zooKeeperTaskCreatorSettings) {
      override def createCheckExistNodeTask(path: String)(): Boolean = {
        assert(expectedPath == path, "path is wrong")
        expectedIsExist
      }
    }

    val zooKeeperService = new ZooKeeperService(zooKeeperTaskCreator, settings.zooKeeperServiceSettings)

    assert(zooKeeperService.isExistNode(expectedPath) == expectedIsExist)
  }
}
