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

import br.com.autonomiccs.apacheCloudStack.client.{ApacheCloudStackClient, ApacheCloudStackRequest}
import com.bwsw.cloudstack.vault.server.MockConfig.cloudStackTaskCreatorSettings
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.common.WeightedQueue

import scala.util.Random

trait TestData {
  val accountId: UUID = UUID.randomUUID()
  val vmId: UUID = UUID.randomUUID()
  val domainId: UUID = UUID.randomUUID()


  val listAccountsCommand = "listAccounts"
  val listVms = "listVirtualMachines"

  val vmUserResourceType = "UserVM"
  val idParameter = "id"
  val nameParameter = "name"

  object Response {
    def getTagResponseJson(key: Tag.Key, value: String): String = "{\"listtagsresponse\":{\"count\":1,\"tag\":[{\"key\":\"" + s"${Tag.Key.toString(key)}" + "\",\"value\":\"" + s"$value" + "\"}]}}"
    def getAccountResponseJson(account: String): String = "{\"listaccountsresponse\":{\"count\":1,\"account\":[{\"id\":\"" + s"$account" + "\",\"user\":[{\"id\":\"0399d562-a273-11e6-88da-6557869a736f\",\"accountid\":\"" + s"$account" + "\"}]}]}}"
    def getVmResponseJson(vm: String, accountName: String, domain: String): String = "{\"listvirtualmachinesresponse\":{\"virtualmachine\":[{\"id\":\"" + s"$vm" + "\",\"account\":\"" + s"$accountName" + "\",\"domainid\":\"" + s"$domain" + "\"}]}}"

    def getResponseWithEmptyVmList = "{\"listvirtualmachinesresponse\":{}}"
    def getResponseWithEmptyAccountList = "{\"listaccountsresponse\":{}}"
    def getResponseWithEmptyUserList = "{\"listusersresponse\":{}}"
  }

  object Request {
    def getAccountTagsRequest(accountId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listTags")
      .addParameter("response", "json")
      .addParameter("resourcetype", "Account")
      .addParameter("listAll", "true")
      .addParameter("resourceid", accountId)

    def getVmTagsRequest(vmId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listTags")
      .addParameter("response", "json")
      .addParameter("resourcetype", "UserVM")
      .addParameter("listAll", "true")
      .addParameter("resourceid", vmId)

    def getAccountRequest(accountId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listAccounts")
      .addParameter("response", "json")
      .addParameter("listAll", "true")
      .addParameter("id", accountId)

    def getAccountRequestByName(name: String, domain: String): ApacheCloudStackRequest = new ApacheCloudStackRequest("listAccounts")
      .addParameter("response", "json")
      .addParameter("listAll", "true")
      .addParameter("name", name)
      .addParameter("domainid", domain)

    def getVmRequest(vmId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listVirtualMachines")
      .addParameter("response", "json")
      .addParameter("listAll", "true")
      .addParameter("id", vmId)

    def getSetTagsRequest(resourceId: UUID, resourceType: Tag.Type, tagTuple: (Tag, Tag, Tag)): ApacheCloudStackRequest = {
      val request = new ApacheCloudStackRequest("createTags")
      request.addParameter("response", "json")
      request.addParameter("resourcetype", Tag.Type.toString(resourceType))
      request.addParameter("resourceids", resourceId)
      request.addParameter(s"tags[0].key", Tag.Key.toString(tagTuple._1.key))
      request.addParameter(s"tags[0].value", tagTuple._1.value)
      request.addParameter(s"tags[1].key", Tag.Key.toString(tagTuple._2.key))
      request.addParameter(s"tags[1].value", tagTuple._2.value)
      request.addParameter(s"tags[2].key", Tag.Key.toString(tagTuple._3.key))
      request.addParameter(s"tags[2].value", tagTuple._3.value)
    }
  }

  def getMockCloudStackTaskCreator(expectedRequest: ApacheCloudStackRequest, response: String)
  : CloudStackTaskCreator = {
    new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override val endpointQueue = new WeightedQueue[String](cloudStackTaskCreatorSettings.endpoints.toList) {
        override val r = new Random {
          override def nextInt(n: Int): Int = 0
        }
      }

      override def createClient(endpoint: String): ApacheCloudStackClient = {
        assert(endpoint == endpointQueue.getElement)
        new ApacheCloudStackClient(endpoint, apacheCloudStackUser) {
          override def executeRequest(request: ApacheCloudStackRequest): String = {
            assert(request.toString == expectedRequest.toString, "request is wrong")
            response
          }
        }
      }
    }
  }

  def getEndpointQueue(endpoints: List[String]) = new WeightedQueue[String](endpoints) {
    override val r = new Random {
      override def nextInt(n: Int): Int = 0
    }
  }

}
