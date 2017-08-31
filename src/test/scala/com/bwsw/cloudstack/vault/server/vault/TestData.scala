package com.bwsw.cloudstack.vault.server.vault

import java.util.UUID

import com.bwsw.cloudstack.vault.server.util.RequestPath

/**
  * Created by medvedev_vv on 28.08.17.
  */
trait TestData {
  val token: UUID = UUID.randomUUID()
  val pathTokenCreate = s"${RequestPath.vaultTokenCreate}"

  def getTokenJsonResponse(tokenId: String): String = "{\"auth\":{\"client_token\":\"" + s"$tokenId" + "\"}}"

  def getLookupTokenJsonResponse(policyName: String,
                                 pathToSecret: String): String = "{\"data\":{\"policies\":[\"" + s"$policyName" + "\"], \"path\":\"" + s"$pathToSecret" + "\"}}"

  def getTokenJson(token: String): String = "{\"token\":\"" + s"$token" + "\"}"
}