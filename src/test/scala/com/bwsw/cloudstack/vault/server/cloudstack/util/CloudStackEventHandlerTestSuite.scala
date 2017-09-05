package com.bwsw.cloudstack.vault.server.cloudstack.util

import java.util.UUID
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.TestData
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.common.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by medvedev_vv on 05.09.17.
  */
class CloudStackEventHandlerTestSuite extends FlatSpec with TestData with BaseTestSuite {

  "handleEventsFromRecords" should "handle list of records" in {
    val countDawnLatch = new CountDownLatch(5)

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
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"TAG.CREATE\",\"entityuuid\":\"" + s"$expectedCreationVmId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",

      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"Account Id: 13\",\"status\":\"Completed\",\"event\":\"ACCOUNT.DELETE\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.DELETE\",\"entityuuid\":\"" + s"$expectedDeletionAccountId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",

      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"VM.DESTROY\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"Vm Id: 13\",\"status\":\"Completed\",\"event\":\"VM.DESTROY\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"VM.DESTROY\",\"entityuuid\":\"" + s"$expectedDeletionVmId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",

      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"USER.DELETE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"User Id: 13\",\"status\":\"Completed\",\"event\":\"USER.DELETE\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"USER.DELETE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",

      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"USER.CREATE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"User Id: 13\",\"status\":\"Completed\",\"event\":\"USER.CREATE\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"USER.CREATE\",\"entityuuid\":\"" + s"$expectedCreationUserId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",

      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"VM.CREATE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"Vm Id: 13\",\"status\":\"Completed\",\"event\":\"VM.CREATE\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"VM.CREATE\",\"entityuuid\":\"" + s"$expectedCreationVmId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",

      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Started\",\"description\":\"creating\",\"event\":\"ACCOUNT.CREATE\",\"entityuuid\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
      "{\"details\":\"Vm Id: 13\",\"status\":\"Completed\",\"event\":\"ACCOUNT.CREATE\"}",
      "{\"eventDateTime\":\"2017-09-05 11:44:29 +0700\",\"status\":\"Completed\",\"description\":\"deleting account. Account Id: 13\",\"event\":\"ACCOUNT.CREATE\",\"entityuuid\":\"" + s"$expectedCreationAccountId" + "\",\"entity\":\"com.cloud.user.Account\",\"Account\":\"ed427f76-f8af-4f87-abe0-26cbc4c7ff9a\",\"account\":\"0399d562-a273-11e6-88da-6557869a736f\",\"user\":\"0399e3e8-a273-11e6-88da-6557869a736f\"}",
    )

    val controller = new CloudStackVaultController(
      new MockVaultService,
      new MockCloudStackService,
      new MockZooKeeperService
    ){
      override def handleAccountCreate(accountId: UUID): Unit = {
        assert(expectedCreationAccountId == accountId)
        actualCreationAccountId = accountId
        countDawnLatch.countDown()
      }

      override def handleAccountDelete(accountId: UUID): Unit = {
        assert(expectedDeletionAccountId == accountId)
        actualDeletionAccountId = accountId
        countDawnLatch.countDown()
      }

      override def handleUserCreate(userId: UUID): Unit = {
        assert(expectedCreationUserId == userId)
        actualCreationUserId = userId
        countDawnLatch.countDown()
      }

      override def handleVmCreate(vmId: UUID): Unit = {
        assert(expectedCreationVmId == vmId)
        actualCreationVmId = vmId
        countDawnLatch.countDown()
      }

      override def handleVmDelete(vmId: UUID): Unit = {
        assert(expectedDeletionVmId == vmId)
        actualDeletionVmId = vmId
        countDawnLatch.countDown()
      }

      override def initializeZooKeeperNodes(): Unit = {}
    }

    val cloudStackEventHandler = new CloudStackEventHandler(controller)
    cloudStackEventHandler.handleEventsFromRecords(records)

    countDawnLatch.await(2000, TimeUnit.MILLISECONDS)

    assert(actualCreationAccountId == expectedCreationAccountId)
    assert(actualCreationUserId == expectedCreationUserId)
    assert(actualCreationVmId == expectedCreationVmId)
    assert(actualDeletionAccountId == actualDeletionAccountId)
    assert(actualDeletionVmId == actualDeletionVmId)
  }

}
