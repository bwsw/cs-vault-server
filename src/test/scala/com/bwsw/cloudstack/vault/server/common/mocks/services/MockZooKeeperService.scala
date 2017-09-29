package com.bwsw.cloudstack.vault.server.common.mocks.services

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator

/**
  * Created by medvedev_vv on 04.09.17.
  */
class MockZooKeeperService extends ZooKeeperService(
  new ZooKeeperTaskCreator(MockConfig.zooKeeperTaskCreatorSettings),
  MockConfig.zooKeeperServiceSettings
) {
  override def getData(path: String): String = throw new NotImplementedError("getData not implemented")

  override def createNodeWithData(path: String, data: String): Unit = throw new NotImplementedError("createNodeWithData not implemented")

  override def deleteNode(path: String): Unit = throw new NotImplementedError("deleteNode not implemented")

  override def isExistNode(path: String): Boolean = throw new NotImplementedError("isExistNode not implemented")
}
