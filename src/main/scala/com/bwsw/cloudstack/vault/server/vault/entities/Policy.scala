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
package com.bwsw.cloudstack.vault.server.vault.entities

import java.util.UUID

import com.bettercloud.vault.json.Json
import com.bwsw.cloudstack.vault.server.util.{DataPath, RequestPath}

object Policy {

  sealed trait ACL extends Product with Serializable

  object ACL {
    case object Read       extends ACL
    case object Write      extends ACL

    def fromString(acl: String): ACL = acl match {
      case "read"      => ACL.Read
      case "write"     => ACL.Write
    }

    def toString(x: ACL): String = x match {
      case ACL.Read       => "read"
      case ACL.Write      => "write"
    }
  }

  def createVmReadPolicy(account: UUID, vm: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_${vm}_ro*",
      path = s"${DataPath.vmSecret}$vm*",
      acl = Policy.ACL.Read
    )
  }

  def createVmWritePolicy(account: UUID, vm: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_${vm}_rw*",
      path = s"${DataPath.vmSecret}$vm*",
      acl = Policy.ACL.Write
    )
  }

  def createAccountReadPolicy(account: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_ro*",
      path = s"${DataPath.accountSecret}$account*",
      acl = Policy.ACL.Read
    )
  }

  def createAccountWritePolicy(account: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_rw*",
      path = s"${DataPath.accountSecret}$account*",
      acl = Policy.ACL.Write
    )
  }
}

final case class Policy(name: String, path: String, acl: Policy.ACL) {
  def jsonString: String = {
    Json.`object`().add(
      "rules", "path \"" + path + "\" {\"policy\"=\"" + Policy.ACL.toString(acl) + "\"}"
    ).toString()
  }
}
