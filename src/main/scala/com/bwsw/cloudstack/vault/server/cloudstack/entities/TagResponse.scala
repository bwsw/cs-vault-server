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

private[cloudstack] case class TagResponse(@JsonProperty("listtagsresponse") tagSet: TagSet)

private[cloudstack] case class TagSet(@JsonProperty("tag") tags: Option[Set[Tag]])

object Tag {
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
    case object Account   extends Type
    case object UserVM    extends Type

    def toString(x: Type): String = x match {
      case  Type.Account    => "Account"
      case  Type.UserVM     => "UserVM"
    }
  }

  @JsonSerialize(using = classOf[KeySerializer])
  @JsonDeserialize(using = classOf[KeyDeserializer])
  sealed trait Key extends Product with Serializable {

    def oneOf(xs: Key*): Boolean = xs.contains(this)

    def oneOf(xs: Set[Key]): Boolean = xs.contains(this)

  }

  object Key {
    val prefix: String = ApplicationConfig.getRequiredString(ConfigLiterals.tagNamePrefix) match {
      case "" => ""
      case x => s"$x."
    }
    private val lowerCaseVaultRoString     = s"${prefix}vault.ro"
    private val lowerCaseVaultRwString     = s"${prefix}vault.rw"
    private val lowerCaseVaultHostString   = s"${prefix}vault.host"
    private val lowerCaseVaultPrefixString = s"${prefix}vault.prefix"
    private val upperCaseVaultRoString     = lowerCaseVaultRoString.toUpperCase
    private val upperCaseVaultRwString     = lowerCaseVaultRwString.toUpperCase
    private val upperCaseVaultHostString   = lowerCaseVaultHostString.toUpperCase
    private val upperCaseVaultPrefixString = lowerCaseVaultPrefixString.toUpperCase

    case object VaultRO      extends Key
    case object VaultRW      extends Key
    case object VaultHost    extends Key
    case object VaultPrefix  extends Key
    case object Other        extends Key

    def fromString: PartialFunction[String, Key] = {
      case `upperCaseVaultRoString`       => Key.VaultRO
      case `upperCaseVaultRwString`       => Key.VaultRW
      case `upperCaseVaultHostString`     => Key.VaultHost
      case `upperCaseVaultPrefixString`   => Key.VaultPrefix
      case _                              => Key.Other
    }

    def toString(x: Key): String = x match {
      case  Key.VaultRO       => lowerCaseVaultRoString
      case  Key.VaultRW       => lowerCaseVaultRwString
      case  Key.VaultPrefix   => lowerCaseVaultPrefixString
      case  Key.VaultHost     => lowerCaseVaultHostString
      case  Key.Other         => ""
    }
  }
}

case class Tag(key: Tag.Key, value: String)
