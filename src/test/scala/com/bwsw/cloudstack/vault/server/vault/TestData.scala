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
package com.bwsw.cloudstack.vault.server.vault

import java.util.UUID

import com.bwsw.cloudstack.entities.common.WeightedQueue
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.entities.Policy

import scala.util.Random

trait TestData {
  val token = UUID.randomUUID()
  val accountId = UUID.randomUUID()
  val vmId = UUID.randomUUID()
  val pathTokenCreate = s"${RequestPath.vaultTokenCreate}"

  def getTokenJsonResponse(tokenId: String): String = "{\"auth\":{\"client_token\":\"" + s"$tokenId" + "\"}}"

  def getLookupTokenJsonResponse(policyName: String): String = "{\"data\":{\"policies\":[\"" + s"$policyName" + "\"]}}"

  def getTokenJson(token: String): String = "{\"token\":\"" + s"$token" + "\"}"

  def getTokenInitParametersJson(noDefaultPolicy: Boolean, policyName: String, period: Int): String = "{\"no_default_policy\":" + s"$noDefaultPolicy" + ",\"policies\":[\"" + s"$policyName" + "\"],\"period\":" + s"$period}"

  def getPolicyJson(policy: Policy): String = "{\"rules\":\"path \\\"" + s"${policy.path}" + "\\\" {\\\"policy\\\"=\\\"" + s"${Policy.ACL.toString(policy.acl)}" + "\\\"}\"}"

  def getSubSecretPathsJson(firstSecretPaths: String, secondSecretPaths:String): String = "{\"data\":{\"keys\": [\"" + s"$firstSecretPaths" + "\", \"" + s"$secondSecretPaths" + "\"]}}"

  def getEndpointQueue(endpoints: List[String]): WeightedQueue[String] = new WeightedQueue[String](endpoints) {
    override val r = new Random {
      override def nextInt(n: Int): Int = 0
    }
  }
}
