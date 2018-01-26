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

import java.util.UUID

import com.bwsw.cloudstack.vault.server.IntegrationTestsComponents
import com.bwsw.cloudstack.vault.server.util.IntegrationTestsSettings
import com.bwsw.cloudstack.vault.server.zookeeper.util.exception.ZooKeeperFatalException
import org.scalatest.{BeforeAndAfterAll, Outcome, fixture}

class ZooKeeperServiceIntegrationTestSuite extends fixture.FlatSpec with IntegrationTestsComponents with BeforeAndAfterAll {
  val zooKeeperSettings = ZooKeeperService.Settings(
    IntegrationTestsSettings.zooKeeperEndpoints,
    IntegrationTestsSettings.zooKeeperRetryDelay
  )
  val zooKeeperService = new ZooKeeperService(zooKeeperSettings)

  case class FixtureParam(path: String)

  def withFixture(test: OneArgTest): Outcome = {
    val randomPath = s"/${UUID.randomUUID().toString}/1"
    val theFixture = FixtureParam(randomPath)

    withFixture(test.toNoArgTest(theFixture))
  }

  "ZookeeperService" should "return 'false' on check non-existing node, and return 'true' on check existing node" in { fixture =>

    assert(!zooKeeperService.doesNodeExist(fixture.path))

    zooKeeperService.createNodeWithData(fixture.path, "data")

    assert(zooKeeperService.doesNodeExist(fixture.path))
  }

  "ZookeeperService" should "throw ZooKeeperFatalException on creation existing node" in { fixture =>
    zooKeeperService.createNodeWithData(fixture.path, "data")

    assertThrows[ZooKeeperFatalException](zooKeeperService.createNodeWithData(fixture.path, "data"))
  }

  "ZooKeeperService" should "retrieve 'None' from non-existiong node, and retrieve data from existing node" in { fixture =>
    val expectedData = "data"

    assert(zooKeeperService.getNodeData(fixture.path).isEmpty)

    zooKeeperService.createNodeWithData(fixture.path, expectedData)
    val actualData = zooKeeperService.getNodeData(fixture.path).get

    assert(expectedData == actualData)
  }

  "ZooKeeperService" should "delete existing node" in { fixture =>
    zooKeeperService.createNodeWithData(fixture.path, "")

    assert(zooKeeperService.doesNodeExist(fixture.path))

    zooKeeperService.deleteNode(fixture.path)

    assert(!zooKeeperService.doesNodeExist(fixture.path))
  }

  "ZooKeeperService" should "throw ZooKeeperFatalException on deletion non-existing node" in { fixture =>
    assertThrows[ZooKeeperFatalException](zooKeeperService.deleteNode(fixture.path))
  }

  override def afterAll():Unit = {
    zooKeeperService.close
  }
}
