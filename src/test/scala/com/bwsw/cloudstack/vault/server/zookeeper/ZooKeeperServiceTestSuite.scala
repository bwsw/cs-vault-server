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
package com.bwsw.cloudstack.vault.server.zookeeper

import com.bwsw.cloudstack.vault.server.MockConfig._
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperFatalException
import org.apache.curator.test.TestingServer
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

class ZooKeeperServiceTestSuite extends FlatSpec with BeforeAndAfterAll {
  val start = true
  val server = new TestingServer(9000, start)
  val connectString = server.getConnectString

  val path = "/test/path"
  val expectedData = "expectedData"
  val zooKeeperService = new ZooKeeperService(zooKeeperServiceSettings.copy(connectString)) {
  }

  "createNodeWithData" should "create znode with data" in {
    assert(zooKeeperService.createNodeWithData(path, expectedData).isInstanceOf[Unit])
    zooKeeperService.deleteNode(path)
  }

  "createNodeWithData" should "throw ZooKeeperCriticalException" in {
    zooKeeperService.createNodeWithData(path, expectedData).isInstanceOf[Unit]
    assertThrows[ZooKeeperFatalException] {
      zooKeeperService.createNodeWithData(path, expectedData)
    }
    zooKeeperService.deleteNode(path)
  }

  "doesNodeExist" should "return true if znode exists" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.doesNodeExist(path))
    zooKeeperService.deleteNode(path)
  }

  "getNodeData" should "get data from znode" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.getNodeData(path).get == expectedData)
    zooKeeperService.deleteNode(path)
  }

  "deleteNode" should "delete znode" in {
    zooKeeperService.createNodeWithData(path, expectedData)
    assert(zooKeeperService.deleteNode(path).isInstanceOf[Unit])
  }

  "deleteNode" should "throw ZooKeeperCriticalException" in {
    assertThrows[ZooKeeperFatalException] {
      zooKeeperService.deleteNode(path)
    }
  }

  "getNodeData" should "return None if znode does not exist" in {
    val actualData = zooKeeperService.getNodeData(path)
    assert(actualData.isEmpty)
  }

  "doesNodeExist" should "return false if znode does not exist" in {
    assert(!zooKeeperService.doesNodeExist(path))
  }

  override def afterAll(): Unit = {
    zooKeeperService.close()
    server.close()
  }
}
