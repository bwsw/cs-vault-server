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

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}

object CloudStackEvent {

  class ActionDeserializer extends JsonDeserializer[Action] {
    def deserialize(parser: JsonParser, context: DeserializationContext): Action = {
      val value = parser.getValueAsString
      Option(value).map[CloudStackEvent.Action](Action.fromString) match {
        case Some(x) => x
        case None => throw new RuntimeJsonMappingException(s"$value is not valid CloudStackEvent.Action")
      }
    }
  }

  class StatusDeserializer extends JsonDeserializer[Status] {
    def deserialize(parser: JsonParser, context: DeserializationContext): Status = {
      val value = parser.getValueAsString
      Option(value).map[CloudStackEvent.Status](Status.fromString) match {
        case Some(x) => x
        case None => throw new RuntimeJsonMappingException(s"$value is not valid CloudStackEvent.Status")
      }
    }
  }

  @JsonDeserialize(using = classOf[ActionDeserializer])
  sealed trait Action extends Product with Serializable {

    def oneOf(xs: Action*): Boolean = xs.contains(this)

    def oneOf(xs: Set[Action]): Boolean = xs.contains(this)

  }

  object Action {
    case object VMCreate      extends Action
    case object UserCreate    extends Action
    case object UserDelete    extends Action
    case object AccountCreate extends Action
    case object VMDelete      extends Action
    case object AccountDelete extends Action
    case object Other         extends Action

    def fromString(eventSting: String): Action = eventSting match {
      case "VM.CREATE"      => Action.VMCreate
      case "VM.DESTROY"     => Action.VMDelete
      case "USER.CREATE"    => Action.UserCreate
      case "USER.DELETE"    => Action.UserDelete
      case "ACCOUNT.CREATE" => Action.AccountCreate
      case "ACCOUNT.DELETE" => Action.AccountDelete
      case _                => Action.Other
    }

    def toString(x: Action): String = x match {
      case Action.VMCreate       => "VM.CREATE"
      case Action.VMDelete       => "VM.DESTROY"
      case Action.UserCreate     => "USER.CREATE"
      case Action.UserDelete     => "USER.DELETE"
      case Action.AccountCreate  => "ACCOUNT.CREATE"
      case Action.AccountDelete  => "ACCOUNT.DELETE"
      case Action.Other          => ""
    }
  }

  @JsonDeserialize(using = classOf[StatusDeserializer])
  sealed trait Status extends Product with Serializable {

    def oneOf(xs: Status*): Boolean = xs.contains(this)

    def oneOf(xs: Set[Status]): Boolean = xs.contains(this)

  }

  object Status {
    case object Completed      extends Status
    case object Other          extends Status

    def fromString(statusSting: String): Status = statusSting match {
      case "Completed"      => Status.Completed
      case _                => Status.Other
    }

    def toString(x: Status): String = x match {
      case Status.Completed     => "Completed"
      case Status.Other         => ""
    }
  }

}

final case class CloudStackEvent(status: Option[CloudStackEvent.Status],
                                 @JsonProperty("event") action: Option[CloudStackEvent.Action],
                                 entityuuid: Option[UUID])
