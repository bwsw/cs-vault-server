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
package com.bwsw.cloudstack.vault.server.cloudstack.entities

sealed trait Command extends Product with Serializable

object Command {
  case object ListVirtualMachines     extends Command
  case object ListUsers               extends Command
  case object ListAccounts            extends Command
  case object ListTags                extends Command
  case object CreateTags              extends Command

  def fromString: PartialFunction[String, Command] = {
    case "listVirtualMachines"           => Command.ListVirtualMachines
    case "listUsers"                     => Command.ListUsers
    case "listAccounts"                  => Command.ListAccounts
    case "listTags"                      => Command.ListTags
    case "createTags"                    => Command.CreateTags
  }

  def toString(x: Command): String = x match {
    case Command.ListVirtualMachines     => "listVirtualMachines"
    case Command.ListUsers               => "listUsers"
    case Command.ListAccounts            => "listAccounts"
    case Command.ListTags                => "listTags"
    case Command.CreateTags              => "createTags"
  }
}
