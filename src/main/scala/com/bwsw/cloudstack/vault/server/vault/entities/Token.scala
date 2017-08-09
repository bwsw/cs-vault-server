package com.bwsw.cloudstack.vault.server.vault.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}

/**
  * Created by medvedev_vv on 09.08.17.
  */
object Token {

/**
  * @param policies names of policies from vault server
  *
  * @param period time of life for token in seconds
  */
  case class TokenParameters(@JsonProperty("client_token") id: UUID,
                             policies: List[String],
                             period: Int) //Warning! Is null after deserealization of response on token create action

  case class TokenData(policies: List[String],
                       @JsonProperty("path") path: String)

}

case class Token(@JsonProperty("auth") tokenParameters: Token.TokenParameters)

case class LookupToken(@JsonProperty("data") tokenData: Token.TokenData)
