package com.bwsw.cloudstack.vault.server.cloudstack.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind._

/**
  * Created by medvedev_vv on 10.08.17.
  */
case class ListTagResponse(@JsonProperty("listtagsresponse") tagResponse: TagResponse)

case class TagResponse(count: Int, @JsonProperty("tag") tags: List[Tag])

object Tag {
  class KeySerializer extends JsonSerializer[Key] {
    def serialize(value: Key, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
      gen.writeString(value.toString.toUpperCase)
    }
  }

  class KeyDeserializer extends JsonDeserializer[Key] {
    def deserialize(parser: JsonParser, context: DeserializationContext): Key = {
      val value = parser.getValueAsString
      Option(value).map[Tag.Key](Key.fromString) match {
        case Some(x) => x
        case None => throw new RuntimeJsonMappingException(s"$value is not valid Tag.Key")
      }
    }
  }

  @JsonSerialize(using = classOf[KeySerializer])
  @JsonDeserialize(using = classOf[KeyDeserializer])
  sealed trait Key extends Product with Serializable

  object Key {
    case object VaultRO      extends Key
    case object VaultRW      extends Key
    case object Other        extends Key

    def fromString(tagKey: String): Key = tagKey match {
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

case class Tag(key: String, value: String)
