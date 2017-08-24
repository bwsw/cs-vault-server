package com.bwsw.cloudstack.vault.server.vault.entities

import java.util.UUID

import com.bettercloud.vault.json.Json
import com.bwsw.cloudstack.vault.server.util.RequestPath

/**
  * Created by medvedev_vv on 08.08.17.
  */

object Policy {

  sealed trait ACL extends Product with Serializable

  object ACL {
    case object Read       extends ACL
    case object Write      extends ACL

    def fromString(acl: String): ACL = acl match {
      case "read"      => ACL.Read
      case "write"     => ACL.Write
    }

    def toString(x: ACL): String = x match {
      case ACL.Read       => "read"
      case ACL.Write      => "write"
    }
  }

  def createVmReadPolicy(account: UUID, vm: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_${vm}_ro*",
      path = s"${RequestPath.vmSecret}$vm",
      acl = Policy.ACL.Read
    )
  }

  def createVmWritePolicy(account: UUID, vm: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_${vm}_rw*",
      path = s"${RequestPath.vmSecret}$vm",
      acl = Policy.ACL.Write
    )
  }

  def createAccountReadPolicy(account: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_ro",
      path = s"${RequestPath.accountSecret}$account*",
      acl = Policy.ACL.Read
    )
  }

  def createAccountWritePolicy(account: UUID): Policy = {
    new Policy(
      name = s"acl_${account}_rw*",
      path = s"${RequestPath.accountSecret}$account",
      acl = Policy.ACL.Write
    )
  }
}

final case class Policy(name: String, path: String, acl: Policy.ACL) {
  def jsonString: String = {
    Json.`object`().add(
      "rules", "path \"" + path + "\" {\"policy\"=\"" + Policy.ACL.toString(acl) + "\"}"
    ).toString()
  }
}
