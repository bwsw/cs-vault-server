package com.bwsw.cloudstack.vault.server.cloudstack.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created by medvedev_vv on 10.08.17.
  */
private[cloudstack] case class UserResponse(@JsonProperty("listusersresponse") userList: UserList)

private[cloudstack] case class UserList(@JsonProperty("user") users: Option[List[User]])

private[cloudstack] case class User(id: UUID, accountid: UUID)
