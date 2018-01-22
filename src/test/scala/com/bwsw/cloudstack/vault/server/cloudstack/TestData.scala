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

import br.com.autonomiccs.apacheCloudStack.client.ApacheCloudStackRequest
import com.bwsw.cloudstack.entities.common.WeightedQueue
import com.bwsw.cloudstack.entities.requests.tag.types.TagType
import com.bwsw.cloudstack.entities.responses.Tag

import scala.util.Random

trait TestData {
  val accountId: UUID = UUID.randomUUID()
  val vmId: UUID = UUID.randomUUID()
  val domainId: UUID = UUID.randomUUID()

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

    def getSetTagsRequest(resourceId: UUID, resourceType: TagType, tagTuple: (Tag, Tag)): ApacheCloudStackRequest = {
      val request = new ApacheCloudStackRequest("createTags")
      request.addParameter("response", "json")
      request.addParameter("resourcetype", resourceType.name)
      request.addParameter("resourceids", resourceId)
      request.addParameter(s"tags[0].key", tagTuple._1.key)
      request.addParameter(s"tags[0].value", tagTuple._1.value)
      request.addParameter(s"tags[1].key", tagTuple._2.key)
      request.addParameter(s"tags[1].value", tagTuple._2.value)
    }
  }

  def getEndpointQueue(endpoints: List[String]): WeightedQueue[String] = new WeightedQueue[String](endpoints) {
    override val r = new Random {
      override def nextInt(n: Int): Int = 0
    }
  }

}
