package com.bwsw.cloudstack.vault.server.vault.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created by medvedev_vv on 09.08.17.
  */
private[vault] object Token {

/**
  * Use for body in token creating request
  *
  * @param policies names of policies from vault server
  *
  * @param period time of life for token in seconds
  */
  case class TokenInitParameters(policies: List[String], period: Int)

/**
  * Use for deserialize body in token create response
  *
  * @param id id of token
  */
  case class TokenId(@JsonProperty("client_token") id: UUID)

/**
  * Use for deserialize body in token lookup response
  *
  * @param policies names of policies from vault server
  *
  * @param path path to secret
  */
  case class TokenData(policies: List[String],
                       @JsonProperty("path") path: String)

}

private[vault] case class Token(@JsonProperty("auth") tokenId: Token.TokenId)

private[vault] case class LookupToken(@JsonProperty("data") tokenData: Token.TokenData)
