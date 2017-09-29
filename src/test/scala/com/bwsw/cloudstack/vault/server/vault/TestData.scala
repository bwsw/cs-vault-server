package com.bwsw.cloudstack.vault.server.vault

import java.util.UUID

import com.bettercloud.vault.rest.Rest
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.entities.Policy

/**
  * Created by medvedev_vv on 28.08.17.
  */
trait TestData {
  val token: UUID = UUID.randomUUID()
  val accountId: UUID = UUID.randomUUID()
  val vmId: UUID = UUID.randomUUID()
  val pathTokenCreate = s"${RequestPath.vaultTokenCreate}"

  def getTokenJsonResponse(tokenId: String): String = "{\"auth\":{\"client_token\":\"" + s"$tokenId" + "\"}}"

  def getLookupTokenJsonResponse(policyName: String): String = "{\"data\":{\"policies\":[\"" + s"$policyName" + "\"]}}"

  def getTokenJson(token: String): String = "{\"token\":\"" + s"$token" + "\"}"

  def getTokenInitParametersJson(useDefaultPolicy: Boolean, policyName: String, period: Int): String = "{\"no_default_policy\":" + s"$useDefaultPolicy" + ",\"policies\":[\"" + s"$policyName" + "\"],\"period\":" + s"$period}"

  def getPolicyJson(policy: Policy): String = "{\"rules\":\"path \\\"" + s"${policy.path}" + "\\\" {\\\"policy\\\"=\\\"" + s"${Policy.ACL.toString(policy.acl)}" + "\\\"}\"}"

}
