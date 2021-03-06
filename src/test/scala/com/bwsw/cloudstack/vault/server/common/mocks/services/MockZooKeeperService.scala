/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.bwsw.cloudstack.vault.server.common.mocks.services

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import org.apache.curator.framework.CuratorFramework

class MockZooKeeperService extends ZooKeeperService(MockConfig.zooKeeperServiceSettings) {
  override protected val curatorClient: CuratorFramework = null

  override def getNodeData(path: String): Option[String] = throw new NotImplementedError("getDataIfNodeExist not implemented")

  override def createNodeWithData(path: String, data: String): Unit = throw new NotImplementedError("createNodeWithData not implemented")

  override def deleteNode(path: String): Unit = throw new NotImplementedError("deleteNode not implemented")

  override def doesNodeExist(path: String): Boolean = throw new NotImplementedError("isExistNode not implemented")

  override def initCuratorClient(): Unit = {}
}
