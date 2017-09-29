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
package com.bwsw.cloudstack.vault.server.vault.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

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
