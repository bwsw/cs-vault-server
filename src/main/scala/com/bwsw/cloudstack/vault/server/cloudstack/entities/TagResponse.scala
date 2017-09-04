package com.bwsw.cloudstack.vault.server.cloudstack.entities

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind._

/**
  * Created by medvedev_vv on 10.08.17.
  */
private[cloudstack] case class TagResponse(@JsonProperty("listtagsresponse") tagList: TagList)

private[cloudstack] case class TagList(@JsonProperty("tag") tags: Option[List[Tag]])

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
    case object VaultRO      extends Key
    case object VaultRW      extends Key
    case object Other        extends Key

    def fromString: PartialFunction[String, Key] = {
      case "VAULT_RO"       => Key.VaultRO
      case "VAULT_RW"       => Key.VaultRW
      case _                => Key.Other
    }

    def toString(x: Key): String = x match {
      case  Key.VaultRO       => "VAULT_RO"
      case  Key.VaultRW       => "VAULT_RW"
      case  Key.Other         => ""
    }
  }
}

case class Tag(key: Tag.Key, value: String)
