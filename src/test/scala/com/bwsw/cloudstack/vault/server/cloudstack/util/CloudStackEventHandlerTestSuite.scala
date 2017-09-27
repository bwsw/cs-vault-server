package com.bwsw.cloudstack.vault.server.cloudstack.util

import java.util.UUID

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.TestData
import com.bwsw.cloudstack.vault.server.controllers.CloudStackVaultController
import com.bwsw.cloudstack.vault.server.common.mocks.services.{MockCloudStackService, MockVaultService, MockZooKeeperService}
import org.scalatest.FlatSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by medvedev_vv on 05.09.17.
  */
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
      new MockZooKeeperService
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
      case (future, event) => future
    }

    val singleFuture = Future.sequence(handleEventFutures)

    singleFuture.onComplete(x => assert(x.isSuccess))

    assert(actualCreationAccountId == expectedCreationAccountId)
    assert(actualCreationUserId == expectedCreationUserId)
    assert(actualCreationVmId == expectedCreationVmId)
    assert(actualDeletionAccountId == actualDeletionAccountId)
    assert(actualDeletionVmId == actualDeletionVmId)
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
      new MockZooKeeperService
    ){
    }

    val cloudStackEventHandler = new CloudStackEventHandler(controller)

    assert(cloudStackEventHandler.handleEventsFromRecords(records).isEmpty)
  }

}
