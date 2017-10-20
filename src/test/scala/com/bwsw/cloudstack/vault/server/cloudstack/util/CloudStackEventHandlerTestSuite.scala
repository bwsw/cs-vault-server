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
package com.bwsw.cloudstack.vault.server.cloudstack.util

import java.util.UUID

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.TestData
import com.bwsw.cloudstack.vault.server.cloudstack.entities.CloudStackEvent
import com.bwsw.cloudstack.vault.server.common.ProcessingEventResult
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.common.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CloudStackEventHandlerTestSuite extends FlatSpec with TestData with BaseTestSuite {

  "handleEventsFromRecords" should "handle valid records" in {

    var actualCreationAccountId: UUID = null
    var actualCreationVmId: UUID = null
    var actualCreationUserId: UUID = null
    var actualDeletionAccountId: UUID = null
    var actualDeletionVmId: UUID = null

    val expectedCreationAccountId = accountId
    val expectedCreationUserId = userId
    val expectedCreationVmId = vmId
    val expectedDeletionAccountId = UUID.randomUUID()
    val expectedDeletionVmId = UUID.randomUUID()

    val records: List[String] = List(
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$expectedDeletionAccountId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"VM.DESTROY\",\"entityuuid\":\"" + s"$expectedDeletionVmId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"USER.CREATE\",\"entityuuid\":\"" + s"$expectedCreationUserId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"VM.CREATE\",\"entityuuid\":\"" + s"$expectedCreationVmId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.CREATE\",\"entityuuid\":\"" + s"$expectedCreationAccountId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ){
      override def handleAccountCreate(accountId: UUID): Unit = {
        assert(expectedCreationAccountId == accountId)
        actualCreationAccountId = accountId
      }

      override def handleAccountDelete(accountId: UUID): Unit = {
        assert(expectedDeletionAccountId == accountId)
        actualDeletionAccountId = accountId
      }

      override def handleUserCreate(userId: UUID): Unit = {
        assert(expectedCreationUserId == userId)
        actualCreationUserId = userId
      }

      override def handleVmCreate(vmId: UUID): Unit = {
        assert(expectedCreationVmId == vmId)
        actualCreationVmId = vmId
      }

      override def handleVmDelete(vmId: UUID): Unit = {
        assert(expectedDeletionVmId == vmId)
        actualDeletionVmId = vmId
      }
    }

    val cloudStackEventHandler = new CloudStackEventHandler(controller)
    val handleEventFutures = cloudStackEventHandler.handleEventsFromRecords(records).map {
      case ProcessingEventResult(event, future) => future
    }

    val singleFuture = Future.sequence(handleEventFutures)

    singleFuture.onComplete(x => assert(x.isSuccess))

    for {
      x <- singleFuture
    } yield {
      assert(actualCreationAccountId == expectedCreationAccountId)
      assert(actualCreationUserId == expectedCreationUserId)
      assert(actualCreationVmId == expectedCreationVmId)
      assert(actualDeletionAccountId == actualDeletionAccountId)
      assert(actualDeletionVmId == actualDeletionVmId)
    }
  }

  "handleEventsFromRecords" should "not handle invalid records" in {

    val records: List[String] = List(
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"USER.DELETE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"User Id: 13\",\"status\":\"Completed\",\"event\":\"USER.DELETE\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"USER.CREATE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"User Id: 13\",\"status\":\"Completed\",\"event\":\"USER.CREATE\"}",
      "notvalidJson123"
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ){
    }

    val cloudStackEventHandler = new CloudStackEventHandler(controller)

    assert(cloudStackEventHandler.handleEventsFromRecords(records).isEmpty)
  }

  "restartEvent" should "handle event" in {
    val expectedUserId = userId

    val event = CloudStackEvent(
      Some(CloudStackEvent.Status.Completed),
      Some(CloudStackEvent.Action.UserCreate),
      Some(expectedUserId)
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ){
      override def handleUserCreate(userId: UUID): Unit = {
        assert(userId == expectedUserId)
      }
    }

    val cloudStackEventHandler = new CloudStackEventHandler(controller)

    val resultFuture = cloudStackEventHandler.restartEvent(event) match {
      case ProcessingEventResult(handledEvent, future) =>
        assert(handledEvent == event, "event is wrong")
        future
    }

    resultFuture.onComplete(x => assert(x.isSuccess))
  }
}
