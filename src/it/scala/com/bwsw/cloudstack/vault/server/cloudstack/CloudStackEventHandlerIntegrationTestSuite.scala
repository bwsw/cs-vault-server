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
package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

import com.bwsw.cloudstack.entities.requests.account.AccountCreateRequest
import com.bwsw.cloudstack.entities.requests.account.AccountCreateRequest.RootAdmin
import com.bwsw.cloudstack.entities.requests.vm.VmCreateRequest
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackEventHandler
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.util.IntegrationTestsSettings
import com.bwsw.cloudstack.vault.server.util.cloudstack.TestEntities
import com.bwsw.cloudstack.vault.server.util.cloudstack.requests.{AccountDeleteRequest, VmCreateTestRequest, VmDeleteRequest}
import com.bwsw.cloudstack.vault.server.util.cloudstack.responses.VmCreateResponse
import com.bwsw.cloudstack.vault.server.util.kafka.TestConsumer
import com.bwsw.kafka.reader.MessageQueue
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global

class CloudStackEventHandlerIntegrationTestSuite extends FlatSpec with MockitoSugar with BeforeAndAfterAll with TestEntities {

  val dummyFlag = new AtomicBoolean(true)
  val consumer = new TestConsumer[String, String](
    IntegrationTestsSettings.kafkaEndpoints,
    IntegrationTestsSettings.kafkaGroupId
  )
  consumer.assignToEnd(IntegrationTestsSettings.kafkaTopics.head)

  val messageQueue = new MessageQueue[String, String](consumer)

  "CloudStackEventHandler" should "handle account creation and deletion" in {
    val accountId = UUID.randomUUID()
    var countOfCreationHandling = 0
    var countOfDeletionHandling = 0

    val controller = mock[CloudStackVaultController]

    doAnswer(_ => {
      countOfCreationHandling = countOfCreationHandling + 1
    }).when(controller).handleAccountCreate(accountId)

    doAnswer(_ => {
      countOfDeletionHandling = countOfDeletionHandling + 1
    }).when(controller).handleAccountDelete(accountId)

    val eventHandler = new CloudStackEventHandler(messageQueue, messageCount = 100, mapper, controller)

    val accountCreateRequest = new AccountCreateRequest(AccountCreateRequest.Settings(
      RootAdmin,
      "test@example.com",
      "firstname",
      "lastName",
      "password",
      s"username $accountId"
    )).withId(accountId)

    accountDao.create(accountCreateRequest)

    val accountDeleteRequest = new AccountDeleteRequest(accountId)

    executor.executeRequest(accountDeleteRequest.request)

    //waiting account creation/deletion in CloudStack Server
    var i = 0
    while(i < 5 && countOfDeletionHandling < 1) {
      Thread.sleep(1000)
      eventHandler.handle(dummyFlag)
      i = i + 1
    }

    assert(dummyFlag.get())
    assert(countOfCreationHandling == 1)
    assert(countOfDeletionHandling == 1)
  }

  "CloudStackEventHandler" should "handle VM creation and deletion" in {
    var countOfCreationHandling = 0
    var countOfDeletionHandling = 0

    val vmCreateTestRequest = new VmCreateTestRequest(VmCreateRequest.Settings(
      retrievedServiceOfferingId,
      retrievedTemplateId,
      retrievedZoneId
    ))

    val vmId = mapper.deserialize[VmCreateResponse](executor.executeRequest(vmCreateTestRequest.request)).vmId.id

    val controller = mock[CloudStackVaultController]

    doAnswer(_ => {
      countOfCreationHandling = countOfCreationHandling + 1
    }).when(controller).handleVmCreate(vmId)

    doAnswer(_ => {
      countOfDeletionHandling = countOfDeletionHandling + 1
    }).when(controller).handleVmDelete(vmId)

    val eventHandler = new CloudStackEventHandler(messageQueue, messageCount = 100, mapper, controller)

    val vmDeleteRequest = new VmDeleteRequest(vmId)

    executor.executeRequest(vmDeleteRequest.request)

    //waiting vm creation/deletion in CloudStack Server
    var i = 0
    while(i < 10 && countOfDeletionHandling < 1) {
      Thread.sleep(1000)
      eventHandler.handle(dummyFlag)
      i = i + 1
    }

    assert(dummyFlag.get())
    assert(countOfCreationHandling == 1)
    assert(countOfDeletionHandling == 1)
  }

  override def afterAll(): Unit = {
    consumer.close()
  }
}
