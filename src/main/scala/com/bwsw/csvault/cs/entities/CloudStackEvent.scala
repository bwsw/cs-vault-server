package com.bwsw.csvault.cs.entities

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}

/**
  * Created by medvedev_vv on 01.08.17.
  */
object CloudStackEvent {

  class LocalDateTimeSerializer extends JsonSerializer[LocalDateTime] {
    def serialize(value: LocalDateTime, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
      gen.writeString(value.toString.toUpperCase)
    }
  }

  class LocalDateTimeDeserializer extends JsonDeserializer[LocalDateTime] {
    def deserialize(parser: JsonParser, context: DeserializationContext): LocalDateTime = {
      val value = parser.getValueAsString
      Option(value).map[LocalDateTime](x => LocalDateTime.parse(x, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss +SSSS") )) match {
        case Some(x) => x
        case None => throw new RuntimeJsonMappingException(s"$value is not valid LocalDateTime")
      }
    }
  }

  class ActionSerializer extends JsonSerializer[Action] {
    def serialize(value: Action, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
      gen.writeString(value.toString.toUpperCase)
    }
  }

  class ActionDeserializer extends JsonDeserializer[Action] {
    def deserialize(parser: JsonParser, context: DeserializationContext): Action = {
      val value = parser.getValueAsString
      Option(value).map[CloudStackEvent.Action](Action.fromString) match {
        case Some(x) => x
        case None => throw new RuntimeJsonMappingException(s"$value is not valid CloudStackEvent.Action")
      }
    }
  }

  @JsonSerialize(using = classOf[ActionSerializer])
  @JsonDeserialize(using = classOf[ActionDeserializer])
  sealed trait Action extends Product with Serializable {

    def oneOf(xs: Action*): Boolean = xs.contains(this)

    def oneOf(xs: Set[Action]): Boolean = xs.contains(this)

  }

  object Action {
    case object VMCreate      extends Action
    case object UserCreate    extends Action
    case object AccountCreate extends Action
    case object VMDelete      extends Action
    case object AccountDelete extends Action

    val fromString: PartialFunction[String, Action] = {
      case "VM.CREATE"      => Action.VMCreate
      case "VM.DELETE"      => Action.VMDelete
      case "USER.CREATE"    => Action.UserCreate
      case "ACCOUNT.CREATE" => Action.AccountCreate
      case "ACCOUNT.DELETE" => Action.AccountDelete
    }

    def ActionToString(x: Action): String = x match {
      case Action.VMCreate       => "VM.CREATE"
      case Action.VMDelete       => "VM.DELETE"
      case Action.UserCreate     => "USER.CREATE"
      case Action.AccountCreate  => "ACCOUNT.CREATE"
      case Action.AccountDelete  => "ACCOUNT.DELETE"
    }
  }

}

final case class CloudStackEvent(@JsonProperty("Role")  role: UUID,
                                 @JsonProperty("Account")  accountCS: UUID,
                                 @JsonSerialize(using = classOf[CloudStackEvent.LocalDateTimeSerializer])
                                 @JsonDeserialize(using = classOf[CloudStackEvent.LocalDateTimeDeserializer])
                                 eventDateTime: LocalDateTime,
                                 entityuuid: UUID,
                                 description: String,
                                 action: CloudStackEvent.Action,
                                 domain: UUID,
                                 user: UUID,
                                 account: UUID,
                                 entity: String,
                                 status: String)
