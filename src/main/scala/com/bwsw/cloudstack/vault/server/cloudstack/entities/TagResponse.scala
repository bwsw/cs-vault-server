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

import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind._

private[cloudstack] case class TagResponse(@JsonProperty("listtagsresponse") tagList: TagList)

private[cloudstack] case class TagList(@JsonProperty("tag") tags: Option[List[Tag]])

object Tag {
  val prefix: String = ApplicationConfig.getRequiredString(ConfigLiterals.tagNamePrefix) match {
    case "" => ""
    case x => s"$x."
  }
  def createTag(key: Tag.Key, value: String): Tag = Tag(key, value)

  class KeySerializer extends JsonSerializer[Key] {
    def serialize(value: Key, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
      gen.writeString(Key.toString(value))
    }
  }

  class KeyDeserializer extends JsonDeserializer[Key] {
    def deserialize(parser: JsonParser, context: DeserializationContext): Key = {
      val value = parser.getValueAsString.toUpperCase
      Option(value).map[Tag.Key](Key.fromString) match {
        case Some(x) => x
        case None => throw new RuntimeJsonMappingException(s"$value is not valid Tag.Key")
      }
    }
  }

  sealed trait Type extends Product with Serializable

  object Type {
    case object User      extends Type
    case object UserVM    extends Type

    def fromString: PartialFunction[String, Type] = {
      case "User"       => Type.User
      case "UserVM"     => Type.UserVM
    }

    def toString(x: Type): String = x match {
      case  Type.User       => "User"
      case  Type.UserVM     => "UserVM"
    }
  }

  @JsonSerialize(using = classOf[KeySerializer])
  @JsonDeserialize(using = classOf[KeyDeserializer])
  sealed trait Key extends Product with Serializable

  object Key {
    private val lowerCaseVaultRoString = s"${prefix}vault.ro"
    private val lowerCaseVaultRwString = s"${prefix}vault.rw"
    private val upperCaseVaultRoString = lowerCaseVaultRoString.toUpperCase
    private val upperCaseVaultRwString = lowerCaseVaultRwString.toUpperCase

    case object VaultRO      extends Key
    case object VaultRW      extends Key
    case object VaultHost    extends Key
    case object VaultPrefix  extends Key
    case object Other        extends Key

    def fromString: PartialFunction[String, Key] = {
      case x if x == s"${prefix.toUpperCase}VAULT.RO.TOKEN"       => Key.VaultRO
      case x if x == s"${prefix.toUpperCase}VAULT.RW.TOKEN"       => Key.VaultRW
      case x if x == s"${prefix.toUpperCase}VAULT.HOST"           => Key.VaultHost
      case x if x == s"${prefix.toUpperCase}VAULT.PREFIX"         => Key.VaultPrefix
      case _                                                      => Key.Other
      case `upperCaseVaultRoString`       => Key.VaultRO
      case `upperCaseVaultRwString`       => Key.VaultRW
      case _                              => Key.Other
    }

    def toString(x: Key): String = x match {
      case  Key.VaultRO       => s"${prefix}vault.ro.token"
      case  Key.VaultRW       => s"${prefix}vault.rw.token"
      case  Key.VaultPrefix   => s"${prefix}vault.prefix"
      case  Key.VaultHost     => s"${prefix}vault.host"
      case  Key.VaultRO       => lowerCaseVaultRoString
      case  Key.VaultRW       => lowerCaseVaultRwString
      case  Key.Other         => ""
    }
  }
}

case class Tag(key: Tag.Key, value: String)
