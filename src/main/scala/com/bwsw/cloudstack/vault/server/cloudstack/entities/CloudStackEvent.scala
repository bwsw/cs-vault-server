package com.bwsw.cloudstack.vault.server.cloudstack.entities

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
      gen.writeString(Action.toString(value))
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

  class StatusSerializer extends JsonSerializer[Status] {
    def serialize(value: Status, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
      gen.writeString(Status.toString(value))
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

  @JsonSerialize(using = classOf[ActionSerializer])
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


  @JsonSerialize(using = classOf[StatusSerializer])
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

final case class CloudStackEvent(@JsonSerialize(using = classOf[CloudStackEvent.LocalDateTimeSerializer])
                                 @JsonDeserialize(using = classOf[CloudStackEvent.LocalDateTimeDeserializer])
                                 eventDateTime: LocalDateTime,
                                 entityuuid: UUID,
                                 @JsonProperty("event") action: CloudStackEvent.Action,
                                 status: CloudStackEvent.Status)
