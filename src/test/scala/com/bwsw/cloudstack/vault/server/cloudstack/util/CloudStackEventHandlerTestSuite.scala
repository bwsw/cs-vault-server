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
import java.util.concurrent.atomic.AtomicBoolean

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.TestData
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.mocks.MockMessageQueue
import com.bwsw.cloudstack.vault.server.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import com.bwsw.cloudstack.vault.server.util.exception.{CriticalException, FatalException}
import com.bwsw.kafka.reader.entities.InputEnvelope
import com.fasterxml.jackson.core.{JsonFactory, JsonParseException}
import com.bwsw.cloudstack.vault.server.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global

class CloudStackEventHandlerTestSuite extends FlatSpec with TestData with BaseTestSuite {

  "handle" should "handle valid records" in {
    val dummyFlag = new AtomicBoolean(true)
    val notExpectedId = UUID.randomUUID()

    var actualCreationAccountId: UUID = notExpectedId
    var actualCreationVmId: UUID = notExpectedId
    var actualDeletionAccountId: UUID = notExpectedId
    var actualDeletionVmId: UUID = notExpectedId

    val expectedCreationAccountId = accountId
    val expectedCreationVmId = vmId
    val expectedDeletionAccountId = UUID.randomUUID()
    val expectedDeletionVmId = UUID.randomUUID()

    val records: List[String] = List(
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$expectedDeletionAccountId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"VM.DESTROY\",\"entityuuid\":\"" + s"$expectedDeletionVmId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
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

      override def handleVmCreate(vmId: UUID): Unit = {
        assert(expectedCreationVmId == vmId)
        actualCreationVmId = vmId
      }

      override def handleVmDelete(vmId: UUID): Unit = {
        assert(expectedDeletionVmId == vmId)
        actualDeletionVmId = vmId
      }
    }

    val mockMessageQueue = getMockMessageQueue(records)

    val cloudStackEventHandler = new CloudStackEventHandler[String](
      mockMessageQueue,
      messageCount = 10,
      controller
    )

    cloudStackEventHandler.handle(dummyFlag)

    assert(actualCreationAccountId == expectedCreationAccountId)
    assert(actualCreationVmId == expectedCreationVmId)
    assert(actualDeletionAccountId == actualDeletionAccountId)
    assert(actualDeletionVmId == actualDeletionVmId)
  }

  "handle" should "handle invalid records" in {
    val dummyFlag = new AtomicBoolean(true)

    val records: List[String] = List(
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"USER.DELETE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"User Id: 13\",\"status\":\"Completed\",\"event\":\"USER.DELETE\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"ACCOUNT.CREATE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"User Id: 13\",\"status\":\"Completed\",\"event\":\"ACCOUNT.CREATE\"}",
      "notvalidJson123"
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ){
    }

    val mockMessageQueue = getMockMessageQueue(records)

    val cloudStackEventHandler = new CloudStackEventHandler[String](
      mockMessageQueue,
      messageCount = 10,
      controller
    )

    assert(cloudStackEventHandler.handle(dummyFlag).size == records.size)
  }

  "handle" should "change the input flag to 'false' and return an empty outputEnvelopes list " +
    "if the process of parsing a json record to entity threw a non-JsonParseException" in {
    val dummyFlag = new AtomicBoolean(true)

    val accountDeletionRecord = "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\"," +
      "\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\"," +
      "\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$accountId" + "\"," +
      "\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\"," +
      "\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
    val records: List[String] = List(accountDeletionRecord)

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    )

    val mockMessageQueue = getMockMessageQueue(records)

    val cloudStackEventHandler = new CloudStackEventHandler[String](
      mockMessageQueue,
      messageCount = 10,
      controller
    ){
      override val jsonSerializer: JsonSerializer = new JsonSerializer(true){
        override def deserialize[T: Manifest](value: String): T = {
          assert(value == accountDeletionRecord)
          throw new Exception
        }
      }
    }

    assert(cloudStackEventHandler.handle(dummyFlag).isEmpty)
    assert(!dummyFlag.get())
  }

  "handle" should "process an event such as unknown if JsonSerializer threw JsonParseException" in {
    val dummyFlag = new AtomicBoolean(true)

    val accountDeletionRecord = "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\"," +
      "\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\"," +
      "\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$accountId" + "\"," +
      "\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\"," +
      "\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
    val records: List[String] = List(accountDeletionRecord)

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    )

    val mockMessageQueue = getMockMessageQueue(records)

    val cloudStackEventHandler = new CloudStackEventHandler[String](
      mockMessageQueue,
      messageCount = 10,
      controller
    ){
      override val jsonSerializer: JsonSerializer = new JsonSerializer(true){
        override def deserialize[T: Manifest](value: String): T = {
          assert(value == accountDeletionRecord)
          val jsonFactory = new JsonFactory()
          throw new JsonParseException(jsonFactory.createParser(value), "")
        }
      }
    }

    assert(cloudStackEventHandler.handle(dummyFlag).size == records.size)
  }

  "handle" should "change the input flag to 'false' and return an empty OutputEnvelopes list " +
    "if the controller threw an exception" in {
    val dummyFlag = new AtomicBoolean(true)
    val expectedAccountId = accountId

    val accountDeletionRecords: List[String] = List(
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\"," +
        "\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\"," +
        "\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$expectedAccountId" + "\"," +
        "\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\"," +
        "\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ){
      override def handleAccountDelete(id: UUID): Unit = {
        assert(expectedAccountId == id)
        throw new Exception
      }
    }

    val mockMessageQueue = getMockMessageQueue(accountDeletionRecords)

    val cloudStackEventHandler = new CloudStackEventHandler[String](
      mockMessageQueue,
      messageCount = 10,
      controller
    )

    assert(cloudStackEventHandler.handle(dummyFlag).isEmpty)
    assert(!dummyFlag.get())
  }

  "handle" should "swallow CriticalException" in {
    val dummyFlag = new AtomicBoolean(true)
    val expectedAccountId = accountId

    val accountDeletionRecords: List[String] = List(
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\"," +
        "\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\"," +
        "\"entityuuid\":\"" + s"$expectedAccountId" + "\",\"entity\":\"com.cloud.user.Account\"," +
        "\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\"," +
        "\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ){
      override def handleAccountDelete(id: UUID): Unit = {
        assert(expectedAccountId == id)
        throw new CriticalException("")
      }
    }

    val mockMessageQueue = getMockMessageQueue(accountDeletionRecords)

    val cloudStackEventHandler = new CloudStackEventHandler[String](
      mockMessageQueue,
      messageCount = 10,
      controller
    )

    assert(cloudStackEventHandler.handle(dummyFlag).size == accountDeletionRecords.size)
  }

  "handle" should "restart event handling if FatalException was thrown" in {
    val dummyFlag = new AtomicBoolean(true)
    val expectedAccountId = accountId
    var isFirstRun = true

    val accountDeletionRecords: List[String] = List(
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\"," +
        "\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\"," +
        "\"entityuuid\":\"" + s"$expectedAccountId" + "\",\"entity\":\"com.cloud.user.Account\"," +
        "\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\"," +
        "\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}"
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService,
      settings.cloudStackVaultControllerSettings
    ){
      override def handleAccountDelete(id: UUID): Unit = {
        assert(expectedAccountId == id)
        if (isFirstRun) {
          isFirstRun = false
          throw new FatalException("")
        }
      }
    }

    val mockMessageQueue = getMockMessageQueue(accountDeletionRecords)

    val cloudStackEventHandler = new CloudStackEventHandler[String](
      mockMessageQueue,
      messageCount = 10,
      controller
    )

    assert(cloudStackEventHandler.handle(dummyFlag).size == accountDeletionRecords.size)
    assert(!isFirstRun)
  }

  private def getMockMessageQueue(records: List[String]): MockMessageQueue[String, String] = {
    new MockMessageQueue[String, String](
      records.map { x =>
        InputEnvelope(topic = "topic", partition = 0, offset = 0, x)
      }
    )
  }
}
