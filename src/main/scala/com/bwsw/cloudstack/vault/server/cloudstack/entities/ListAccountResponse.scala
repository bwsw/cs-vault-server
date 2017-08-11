package com.bwsw.cloudstack.vault.server.cloudstack.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created by medvedev_vv on 10.08.17.
  */
case class ListAccountResponse(@JsonProperty("listaccountsresponse") accountResponse: AccountResponse)

case class AccountResponse(count: Int, @JsonProperty("account") accounts: List[Account])

case class Account(id: UUID, @JsonProperty("user") users: List[User])
