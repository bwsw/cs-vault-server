package com.bwsw.cloudstack.vault.server.common.mocks.services

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import org.apache.curator.framework.CuratorFramework

/**
  * Created by medvedev_vv on 04.09.17.
  */
class MockZooKeeperService extends ZooKeeperService(MockConfig.zooKeeperServiceSettings) {
  override protected val curatorClient: CuratorFramework = null

  override def getDataIfNodeExist(path: String): Option[String] = throw new NotImplementedError("getDataIfNodeExist not implemented")

  override def createNodeWithData(path: String, data: String): Unit = throw new NotImplementedError("createNodeWithData not implemented")

  override def deleteNode(path: String): Unit = throw new NotImplementedError("deleteNode not implemented")

  override def isExistNode(path: String): Boolean = throw new NotImplementedError("isExistNode not implemented")

  override def initCuratorClient(): Unit = {}
}
