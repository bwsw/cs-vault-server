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

import java.net.NoRouteToHostException

import br.com.autonomiccs.apacheCloudStack.client.{ApacheCloudStackClient, ApacheCloudStackRequest}
import br.com.autonomiccs.apacheCloudStack.exceptions.{ApacheCloudStackClientRequestRuntimeException, ApacheCloudStackClientRuntimeException}
import com.bwsw.cloudstack.vault.server.MockConfig.cloudStackTaskCreatorSettings
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.TestData
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import org.scalatest.{FlatSpec, PrivateMethodTester}

class CloudStackTaskCreatorTestSuite extends FlatSpec with TestData with BaseTestSuite with PrivateMethodTester {

  //Positive tests
  "createGetTagTask" should "create task which returns response with account tags" in {
    val key = Tag.Key.VaultRW
    val value = "value1"
    val expectedRequest = Request.getAccountTagsRequest(accountId)
    val expectedResponse = Response.getTagResponseJson(key, value)

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val tagResponse = cloudStackTaskCreator.createGetTagsTask(Tag.Type.Account, accountId)()
    assert(tagResponse == expectedResponse)
  }

  "createGetTagTask" should "create task which returns response with VM tags" in {
    val key = Tag.Key.VaultRW
    val value = "value2"
    val expectedRequest = Request.getVmTagsRequest(vmId)
    val expectedResponse = Response.getTagResponseJson(key, value)

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val tagResponse = cloudStackTaskCreator.createGetTagsTask(Tag.Type.UserVM, vmId)()
    assert(tagResponse == expectedResponse)
  }

  "createGetEntityTask" should "create task which returns response with accounts by id" in {
    val expectedRequest = Request.getAccountRequest(accountId)
    val expectedResponse = Response.getAccountResponseJson(accountId.toString)
    val command = Command.ListAccounts

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val accountResponse = cloudStackTaskCreator.createGetEntityTask(command, Map(cloudStackTaskCreator.idParameter -> accountId.toString))()

    assert(accountResponse == expectedResponse)
  }

  "createGetEntityTask" should "create task which returns response with VM by id" in {
    val accountName = "admin"
    val expectedRequest = Request.getVmRequest(vmId)
    val expectedResponse = Response.getVmResponseJson(vmId.toString, accountName, domainId.toString)
    val command = Command.ListVirtualMachines

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val vmResponse = cloudStackTaskCreator.createGetEntityTask(command, Map(cloudStackTaskCreator.idParameter ->  vmId.toString))()

    assert(vmResponse == expectedResponse)
  }

  "createGetEntityTask" should "create task which returns response with accounts by name" in {
    val accountName = "admin"
    val expectedRequest = Request.getAccountRequestByName(accountName, domainId.toString)
    val expectedResponse = Response.getAccountResponseJson(accountId.toString)
    val command = Command.ListAccounts

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, expectedResponse)

    val vmResponse = cloudStackTaskCreator.createGetEntityTask(command, Map(
            cloudStackTaskCreator.nameParameter -> accountName,
            cloudStackTaskCreator.domainParameter -> domainId.toString
          ))()

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

    val createTagResponse = cloudStackTaskCreator.createSetResourceTagsTask(vmId, Tag.Type.UserVM, Set(tagsTuple._1, tagsTuple._2, tagsTuple._3))()

    assert(createTagResponse.isInstanceOf[Unit])
  }

  "createSetResourceTagTask" should "create task which sets Account tags" in {
    val tagsTuple: Tuple3[Tag, Tag, Tag] = (
      Tag(Tag.Key.VaultRO, "value1"),
      Tag(Tag.Key.VaultRW, "value2"),
      Tag(Tag.Key.VaultRO, "value3")
    )
    val expectedRequest = Request.getSetTagsRequest(accountId, Tag.Type.Account, tagsTuple)

    val cloudStackTaskCreator = getMockCloudStackTaskCreator(expectedRequest, "")

    val createTagResponse = cloudStackTaskCreator.createSetResourceTagsTask(accountId, Tag.Type.Account, Set(tagsTuple._1, tagsTuple._2, tagsTuple._3))()

    assert(createTagResponse.isInstanceOf[Unit])
  }

  "createRequest" should "create request" in {

    val response = "testResponse"

    val createRequest = PrivateMethod[String]('createRequest)

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            response
          }
        }
      }

      override def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String =
        super.createRequest(request, requestDescription)
    }

    def requestTask(): String = cloudStackTaskCreator invokePrivate createRequest(Request.getVmRequest(vmId), "request description")

    assert(requestTask() == response)
  }

  "createRequest" should "throw CloudStackFatalException " +
    "if non-ApacheCloudStackClientRuntimeException was thrown by CloudStack client " in {
    val createRequest = PrivateMethod[String]('createRequest)

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new Exception
          }
        }
      }

      override def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String =
        super.createRequest(request, requestDescription)
    }

    def requestTask(): String = cloudStackTaskCreator invokePrivate createRequest(Request.getVmRequest(vmId), "request description")

    assertThrows[CloudStackFatalException]{
      requestTask()
    }
  }

  "createRequest" should "throw CloudStackEntityDoesNotExistException " +
    "if ApacheCloudStackClientRequestRuntimeException with 431 status code occurred" in {
    val createRequest = PrivateMethod[String]('createRequest)

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new ApacheCloudStackClientRequestRuntimeException(431, "", "")
          }
        }
      }

      override def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String =
        super.createRequest(request, requestDescription)
    }

    def requestTask(): String = cloudStackTaskCreator invokePrivate createRequest(Request.getVmRequest(vmId), "request description")

    assertThrows[CloudStackEntityDoesNotExistException]{
      requestTask()
    }
  }

  "createRequest" should "change apacheCloudStackClient after NoRouteToHostException" in {
    var checkedPath = List.empty[String]

    val urlFirstClient = "http://127.0.0.1:8080/client/api/1"
    val urlSecondClient = "http://127.0.0.1:8080/client/api/2"
    val urlThirdClient = "http://127.0.0.1:8080/client/api/3"
    val expectedPathList = List(urlFirstClient, urlSecondClient, urlThirdClient, urlFirstClient)

    val createRequest = PrivateMethod[String]('createRequest)

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(List(urlFirstClient, urlSecondClient, urlThirdClient))

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        endpoint match {
          case `urlFirstClient` => new ApacheCloudStackClient(urlFirstClient, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              checkedPath = checkedPath ::: urlFirstClient :: Nil
              throw new ApacheCloudStackClientRuntimeException(new NoRouteToHostException)
            }
          }
          case `urlSecondClient` => new ApacheCloudStackClient(urlSecondClient, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              checkedPath = checkedPath ::: urlSecondClient :: Nil
              throw new ApacheCloudStackClientRuntimeException(new NoRouteToHostException)
            }
          }

          case `urlThirdClient` => new ApacheCloudStackClient(urlThirdClient, apacheCloudStackUser) {
            override def executeRequest(request: ApacheCloudStackRequest): String = {
              checkedPath = checkedPath ::: urlThirdClient :: Nil
              throw new ApacheCloudStackClientRuntimeException(new NoRouteToHostException)
            }
          }
        }
      }

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
    assertThrows[ApacheCloudStackClientRuntimeException]{
      requestTask()
    }

    assert(checkedPath == expectedPathList)
  }

  //Negative tests
  "createGetTagTask" should "not swallow ApacheCloudStackClientRuntimeException which includes NoRouteToHostException" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new ApacheCloudStackClientRuntimeException(new NoRouteToHostException)
          }
        }
      }
    }

    assertThrows[ApacheCloudStackClientRuntimeException] {
      cloudStackTaskCreator.createGetTagsTask(Tag.Type.Account, accountId)()
    }
  }

  "createGetTagTask" should "throw CloudStackFatalException if ApacheCloudStackClient throws an exception of type " +
    "that is different from ApacheCloudStackClientRuntimeException(NoRouteToHostException)" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new Exception("test exception")
          }
        }
      }
    }

    assertThrows[CloudStackFatalException] {
      cloudStackTaskCreator.createGetTagsTask(Tag.Type.Account, accountId)()
    }
  }

  "createGetEntityTask" should "not swallow ApacheCloudStackClientRuntimeException which includes NoRouteToHostException" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new ApacheCloudStackClientRuntimeException(new NoRouteToHostException)
          }
        }
      }
    }

    assertThrows[ApacheCloudStackClientRuntimeException] {
      cloudStackTaskCreator.createGetEntityTask(Command.ListAccounts, Map(cloudStackTaskCreator.idParameter -> accountId.toString))()
    }
  }

  "createGetEntityTask" should "throw CloudStackFatalException if ApacheCloudStackClient throws an exception of type " +
    "that is different from ApacheCloudStackClientRuntimeException(NoRouteToHostException)" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new Exception("test exception")
          }
        }
      }
    }

    assertThrows[CloudStackFatalException] {
      cloudStackTaskCreator.createGetEntityTask(Command.ListAccounts, Map(cloudStackTaskCreator.idParameter -> accountId.toString))()
    }
  }

  "createSetResourceTagTask" should "not swallow ApacheCloudStackClientRuntimeException which includes NoRouteToHostException" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new ApacheCloudStackClientRuntimeException(new NoRouteToHostException)
          }
        }
      }
    }

    assertThrows[ApacheCloudStackClientRuntimeException] {
      cloudStackTaskCreator.createSetResourceTagsTask(accountId, Tag.Type.Account, Set(Tag(Tag.Key.VaultRO, "value")))()
    }
  }

  "createSetResourceTagTask" should "throw CloudStackFatalException if ApacheCloudStackClient throws an exception of type " +
    "that is different from ApacheCloudStackClientRuntimeException(NoRouteToHostException)" in {
    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = getEndpointQueue(cloudStackTaskCreatorSettings.endpoints.toList)

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            throw new Exception("test exception")
          }
        }
      }
    }

    assertThrows[CloudStackFatalException] {
      cloudStackTaskCreator.createSetResourceTagsTask(accountId, Tag.Type.Account, Set(Tag(Tag.Key.VaultRO, "value")))()
    }
  }
}
