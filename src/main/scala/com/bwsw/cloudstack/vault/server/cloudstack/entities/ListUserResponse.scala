package com.bwsw.cloudstack.vault.server.cloudstack.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created by medvedev_vv on 10.08.17.
  */
case class ListUserResponse(@JsonProperty("listusersresponse") userResponse: UserResponse)

case class UserResponse(count: Int, @JsonProperty("user") users: List[User])

case class User(id: UUID, accountId: UUID)
