package com.bwsw.cloudstack.vault.server.cloudstack.util

import br.com.autonomiccs.apacheCloudStack.client.{ApacheCloudStackClient, ApacheCloudStackRequest}
import br.com.autonomiccs.apacheCloudStack.exceptions.ApacheCloudStackClientRuntimeException
import com.bwsw.cloudstack.vault.server.MockConfig.cloudStackTaskCreatorSettings
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.TestData
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackCriticalException
import org.scalatest.{FlatSpec, PrivateMethodTester}

/**
  * Created by medvedev_vv on 31.08.17.
  */
class CloudStackTaskCreatorTestSuite extends FlatSpec with TestData with BaseTestSuite with PrivateMethodTester {

  //Positive tests
  "createGetTagTask" should "create task which returns response with user tags" in {
    val key = Tag.Key.VaultRW
    val value = "value1"
    val expectedRequest = Request.getUserTagsRequest(userId)
    val expectedResponse = Response.getTagResponseJson(key, value)

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val tagResponse = cloudStackTaskCreator.createGetTagTask(Tag.Type.User, userId)()
    assert(tagResponse == expectedResponse)
  }

  "createGetTagTask" should "create task which returns response with VM tags" in {
    val key = Tag.Key.VaultRW
    val value = "value2"
    val expectedRequest = Request.getVmTagsRequest(vmId)
    val expectedResponse = Response.getTagResponseJson(key, value)

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val tagResponse = cloudStackTaskCreator.createGetTagTask(Tag.Type.UserVM, vmId)()
    assert(tagResponse == expectedResponse)
  }

  "createGetEntityTask" should "create task which returns response with accounts by id" in {
    val expectedRequest = Request.getAccountRequest(accountId)
    val expectedResponse = Response.getAccountResponseJson(accountId.toString, userId.toString)
    val command = Command.ListAccounts

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val accountResponse = cloudStackTaskCreator.createGetEntityTask(
      accountId.toString,
      cloudStackTaskCreator.idParameter,
      command
    )()

    assert(accountResponse == expectedResponse)
  }

  "createGetEntityTask" should "create task which returns response with VM by id" in {
    val accountName = "admin"
    val expectedRequest = Request.getVmRequest(vmId)
    val expectedResponse = Response.getVmResponseJson(vmId.toString, accountName)
    val command = Command.ListVirtualMachines

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val vmResponse = cloudStackTaskCreator.createGetEntityTask(
      vmId.toString,
      cloudStackTaskCreator.idParameter,
      command
    )()

    assert(vmResponse == expectedResponse)
  }

  "createGetEntityTask" should "create task which returns response with users by id" in {
    val expectedRequest = Request.getUserRequest(userId)
    val expectedResponse = Response.getUserResponseJson(userId.toString, accountId.toString)
    val command = Command.ListUsers

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val userResponse = cloudStackTaskCreator.createGetEntityTask(
      userId.toString,
      cloudStackTaskCreator.idParameter,
      command
    )()

    assert(userResponse == expectedResponse)
  }

  "createGetEntityTask" should "create task which returns response with accounts by name" in {
    val accountName = "admin"
    val expectedRequest = Request.getAccountRequestByName(accountName)
    val expectedResponse = Response.getAccountResponseJson(accountId.toString, userId.toString)
    val command = Command.ListAccounts

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val vmResponse = cloudStackTaskCreator.createGetEntityTask(
      accountName,
      cloudStackTaskCreator.nameParameter,
      command
    )()

    assert(vmResponse == expectedResponse)
  }

  "createSetResourceTagTask" should "create task which sets VM tags" in {
    val tagsTuple: Tuple3[Tag, Tag, Tag] = (
      Tag(Tag.Key.VaultRO, "value1"),
      Tag(Tag.Key.VaultRW, "value2"),
      Tag(Tag.Key.VaultRO, "value3")
    )
    val expectedRequest = Request.getSetTagsRequest(vmId, Tag.Type.UserVM, tagsTuple)

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, "")

    val createTagResponse = cloudStackTaskCreator.createSetResourceTagTask(
      vmId,
      Tag.Type.UserVM,
      tagsTuple._1 :: tagsTuple._2 :: tagsTuple._3 :: Nil
    )()

    assert(createTagResponse.isInstanceOf[Unit])
  }

  "createSetResourceTagTask" should "create task which sets User tags" in {
    val tagsTuple: Tuple3[Tag, Tag, Tag] = (
      Tag(Tag.Key.VaultRO, "value1"),
      Tag(Tag.Key.VaultRW, "value2"),
      Tag(Tag.Key.VaultRO, "value3")
    )
    val expectedRequest = Request.getSetTagsRequest(userId, Tag.Type.User, tagsTuple)

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, "")

    val createTagResponse = cloudStackTaskCreator.createSetResourceTagTask(
      userId,
      Tag.Type.User,
      tagsTuple._1 :: tagsTuple._2 :: tagsTuple._3 :: Nil
    )()

    assert(createTagResponse.isInstanceOf[Unit])
  }

  "createRequest" should "create request after three count of throw ApacheCloudStackClientRuntimeException" in {
    var checkedPath = List.empty[String]

    val response = "testResponse"
    val urlFirstClient = "http://127.0.0.1:8080/client/api/1"
    val urlSecondClient = "http://127.0.0.1:8080/client/api/2"
    val urlThirdClient = "http://127.0.0.1:8080/client/api/3"
    val expectedPathList = List(urlFirstClient, urlSecondClient, urlThirdClient, urlFirstClient)

    var isSecondExecution = false

    val createRequest = PrivateMethod[String]('createRequest)

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] = List (
        new ApacheCloudStackClient(urlFirstClient, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            checkedPath = checkedPath ::: urlFirstClient :: Nil
            if (isSecondExecution) {
              response
            } else {
              isSecondExecution = true
              throw new ApacheCloudStackClientRuntimeException
            }
          }
        },
        new ApacheCloudStackClient(urlSecondClient, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            checkedPath = checkedPath ::: urlSecondClient :: Nil
            throw new ApacheCloudStackClientRuntimeException
          }
        },
        new ApacheCloudStackClient(urlThirdClient, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            checkedPath = checkedPath ::: urlThirdClient :: Nil
            throw new ApacheCloudStackClientRuntimeException
          }
        }
      )

      override def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String =
        super.createRequest(request, requestDescription)
    }

    def requestTask(): String = cloudStackTaskCreator invokePrivate createRequest(Request.getVmRequest(vmId), "request description")

    assertThrows[ApacheCloudStackClientRuntimeException]{
      requestTask()
    }
    assertThrows[ApacheCloudStackClientRuntimeException]{
      requestTask()
    }
    assertThrows[ApacheCloudStackClientRuntimeException]{
      requestTask()
    }
    assert(requestTask() == response)

    assert(checkedPath == expectedPathList)
  }

  "createRequest" should "wrap non-ApacheCloudStackClientRuntimeException into CloudStackCriticalException" in {
    var checkedPath = List.empty[String]

    val response = "testResponse"
    val urlFirstClient = "http://127.0.0.1:8080/client/api/1"
    val urlSecondClient = "http://127.0.0.1:8080/client/api/2"
    val urlThirdClient = "http://127.0.0.1:8080/client/api/3"
    val expectedPathList = List(urlFirstClient, urlSecondClient, urlThirdClient, urlFirstClient)

    var isSecondExecution = false

    val createRequest = PrivateMethod[String]('createRequest)

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] = List (
        new ApacheCloudStackClient(urlFirstClient, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            checkedPath = checkedPath ::: urlFirstClient :: Nil
            if (isSecondExecution) {
              throw new Exception
            } else {
              isSecondExecution = true
              throw new ApacheCloudStackClientRuntimeException
            }
          }
        },
        new ApacheCloudStackClient(urlSecondClient, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            checkedPath = checkedPath ::: urlSecondClient :: Nil
            throw new ApacheCloudStackClientRuntimeException
          }
        },
        new ApacheCloudStackClient(urlThirdClient, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            checkedPath = checkedPath ::: urlThirdClient :: Nil
            throw new ApacheCloudStackClientRuntimeException
          }
        }
      )

      override def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String =
        super.createRequest(request, requestDescription)
    }

    def requestTask(): String = cloudStackTaskCreator invokePrivate createRequest(Request.getVmRequest(vmId), "request description")

    assertThrows[ApacheCloudStackClientRuntimeException]{
      requestTask()
    }
    assertThrows[ApacheCloudStackClientRuntimeException]{
      requestTask()
    }
    assertThrows[ApacheCloudStackClientRuntimeException]{
      requestTask()
    }
    assertThrows[CloudStackCriticalException]{
      requestTask()
    }

    assert(checkedPath == expectedPathList)
  }

  //Negative tests
  "createGetTagTask" should "if ApacheCloudStackClient throw ApacheCloudStackClientRuntimeException, exception does not catch" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] =
        cloudStackTaskCreatorSettings.urlList.map { x =>
          new ApacheCloudStackClient(x, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              throw new ApacheCloudStackClientRuntimeException
            }
          }
        }.toList
    }

    assertThrows[ApacheCloudStackClientRuntimeException] {
      cloudStackTaskCreator.createGetTagTask(Tag.Type.User, userId)()
    }
  }

  "createGetTagTask" should "if ApacheCloudStackClient throw not same with ApacheCloudStackClientRuntimeException, " +
    "the exception will wrapped to CloudStackCriticalException" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] =
        cloudStackTaskCreatorSettings.urlList.map { x =>
          new ApacheCloudStackClient(x, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              throw new Exception("test exception")
            }
          }
        }.toList
    }

    assertThrows[CloudStackCriticalException] {
      cloudStackTaskCreator.createGetTagTask(Tag.Type.User, userId)()
    }
  }

  "createGetEntityTask" should "if ApacheCloudStackClient throw ApacheCloudStackClientRuntimeException, exception does not catch" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] =
        cloudStackTaskCreatorSettings.urlList.map { x =>
          new ApacheCloudStackClient(x, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              throw new ApacheCloudStackClientRuntimeException
            }
          }
        }.toList
    }

    assertThrows[ApacheCloudStackClientRuntimeException] {
      cloudStackTaskCreator.createGetEntityTask(
        accountId.toString,
        cloudStackTaskCreator.idParameter,
        Command.ListAccounts
      )()
    }
  }

  "createGetEntityTask" should "if ApacheCloudStackClient throw not same with ApacheCloudStackClientRuntimeException, " +
  "the exception will wrapped to CloudStackCriticalException" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] =
        cloudStackTaskCreatorSettings.urlList.map { x =>
          new ApacheCloudStackClient(x, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              throw new Exception("test exception")
            }
          }
        }.toList
    }

    assertThrows[CloudStackCriticalException] {
      cloudStackTaskCreator.createGetEntityTask(
        accountId.toString,
        cloudStackTaskCreator.idParameter,
        Command.ListAccounts
      )()
    }
  }

  "createSetResourceTagTask" should  "if ApacheCloudStackClient throw ApacheCloudStackClientRuntimeException, exception does not catch" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] =
        cloudStackTaskCreatorSettings.urlList.map { x =>
          new ApacheCloudStackClient(x, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              throw new ApacheCloudStackClientRuntimeException
            }
          }
        }.toList
    }

    assertThrows[ApacheCloudStackClientRuntimeException] {
      cloudStackTaskCreator.createSetResourceTagTask(
        userId,
        Tag.Type.User,
        List(Tag(Tag.Key.VaultRO, "value"))
      )()
    }
  }

  "createSetResourceTagTask" should "if ApacheCloudStackClient throw not same with ApacheCloudStackClientRuntimeException, " +
    "the exception will wrapped to CloudStackCriticalException" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val apacheCloudStackClientList: List[ApacheCloudStackClient] =
        cloudStackTaskCreatorSettings.urlList.map { x =>
          new ApacheCloudStackClient(x, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              throw new Exception("test exception")
            }
          }
        }.toList
    }

    assertThrows[CloudStackCriticalException] {
      cloudStackTaskCreator.createSetResourceTagTask(
        userId,
        Tag.Type.User,
        List(Tag(Tag.Key.VaultRO, "value"))
      )()
    }
  }
}