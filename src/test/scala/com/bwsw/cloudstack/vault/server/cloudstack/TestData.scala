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
import com.bwsw.cloudstack.entities.responses.account.Account
import com.bwsw.cloudstack.entities.responses.tag.Tag
import com.bwsw.cloudstack.entities.responses.user.User
import com.bwsw.cloudstack.entities.responses.vm.VirtualMachine

import scala.util.Random

trait TestData {
  val accountId: UUID = UUID.randomUUID()
  val vmId: UUID = UUID.randomUUID()
  val domainId: UUID = UUID.randomUUID()

  def getEndpointQueue(endpoints: List[String]): WeightedQueue[String] = new WeightedQueue[String](endpoints) {
    override val r = new Random {
      override def nextInt(n: Int): Int = 0
    }
  }

  def getVirtualMachine(id: UUID, accountName: String, domainId: UUID): VirtualMachine =
    VirtualMachine(
      id = id,
      zoneId = UUID.randomUUID(),
      templateId = UUID.randomUUID(),
      serviceOfferingId = UUID.randomUUID(),
      accountName = accountName,
      domainId = domainId
    )

  def getAccount(id: UUID, name: String, domainId: UUID, users: List[User]): Account =
    Account(
      id = id,
      name = name,
      accountType = 1,
      domainId = domainId,
      networkDomain = "network",
      users = users,
      roleType = "Admin"
    )
}
