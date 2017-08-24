package com.bwsw.cloudstack.vault.server.cloudstack.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created by medvedev_vv on 10.08.17.
  */
case class UserResponse(@JsonProperty("listusersresponse") userList: UserList)

case class UserList(count: Int, @JsonProperty("user") users: List[User])

case class User(id: UUID, accountid: UUID)
