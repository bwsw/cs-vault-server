package com.bwsw.cloudstack.vault.server.vault

import java.util.UUID

import com.bettercloud.vault.json.Json
import com.bettercloud.vault.rest.RestResponse
import com.bwsw.cloudstack.vault.server.common.{Converter, JsonSerializer}
import com.bwsw.cloudstack.vault.server.util._
import com.bwsw.cloudstack.vault.server.vault.entities.{LookupToken, Policy, Token}
import com.bwsw.cloudstack.vault.server.vault.util.VaultRest
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 02.08.17.
  */
class VaultService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val vaultUrl = ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)
  private val vaultRootToken = ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken)
  private val vaultRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
  private val tokenPeriod = Converter.daysToSeconds(ApplicationConfig.getRequiredInt(ConfigLiterals.tokenPeriod))
  private val threadLocalJsonSerializer: ThreadLocal[JsonSerializer] = new ThreadLocal
  threadLocalJsonSerializer.get.setIgnoreUnknown(true)

  def createToken(policies: List[Policy])(): UUID = {
    logger.debug(s"createToken with policies: $policies")
    val jsonSerializer = threadLocalJsonSerializer.get()
    policies.foreach(writePolicy)

    val tokenParameters = Token.TokenInitParameters(
      policies.map(_.name),
      tokenPeriod
    )

    def executeRequest = VaultRest.createPostRequest(
      vaultRootToken,
      s"$vaultUrl${RequestPath.vaultTokenCreate}",
      jsonSerializer.serialize(tokenParameters),
      HttpStatuses.OK_STATUS,
      "create token"
    )

    val response = TaskRunner.tryRunUntilSuccess[RestResponse](
      executeRequest,
      vaultRetryDelay
    )

    val token = jsonSerializer.deserialize[Token](new String(response.getBody))
    logger.debug(s"Token was created")
    token.tokenId.id
  }

  def revokeToken(tokenId: UUID)(): Unit = {
    logger.debug(s"revokeToken")
    val jsonSerializer = threadLocalJsonSerializer.get()
    val jsonTokenId = Json.`object`().add("token", tokenId.toString).toString

    def executeLookupRequest = VaultRest.createPostRequest(
      vaultRootToken,
      s"$vaultUrl${RequestPath.vaultTokenLookup}",
      jsonTokenId,
      HttpStatuses.OK_STATUS,
      "get lookup token"
    )

    val lookupResponse = TaskRunner.tryRunUntilSuccess[RestResponse](
      executeLookupRequest,
      vaultRetryDelay
    )

    val lookupToken = jsonSerializer.deserialize[LookupToken](new String(lookupResponse.getBody))

    def executeRevokeRequest = VaultRest.createPostRequest(
      vaultRootToken,
      s"$vaultUrl${RequestPath.vaultTokenRevoke}",
      jsonTokenId,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "revoke token"
    )

    val revokeResponse = TaskRunner.tryRunUntilSuccess[RestResponse](
      executeRevokeRequest,
      vaultRetryDelay
    )
    logger.debug(s"Token was revoked")

    lookupToken.tokenData.policies.filter { x =>
      x != "default" && x != "root"
    }.foreach(deletePolicy)

    deleteSecret(lookupToken.tokenData.path)
  }

  private def deleteSecret(pathToSecret: String): Unit = {
    logger.debug(s"deleteSecret: $pathToSecret")
    def executeRequest = VaultRest.createDeleteRequest(
      vaultRootToken,
      s"$vaultUrl${RequestPath.vaultSecret}/$pathToSecret",
      "",
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "delete secret"
    )

    TaskRunner.tryRunUntilSuccess[RestResponse](
      executeRequest,
      vaultRetryDelay
    )

    logger.debug(s"data from path: $pathToSecret was deleted")
  }

  private def writePolicy(policy: Policy) = {
    logger.debug(s"writePolicy: $policy")

    def executeRequest = VaultRest.createPutRequest(
        vaultRootToken,
        s"$vaultUrl${RequestPath.vaultPolicy}/${policy.name}",
        policy.jsonString,
        HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
        "write policy"
      )

    TaskRunner.tryRunUntilSuccess[RestResponse](
      executeRequest,
      vaultRetryDelay
    )
    logger.debug(s"policy was writed: $policy")
  }

  private def deletePolicy(policyName: String) = {
    logger.debug(s"deletePolicy: $policyName")

    def executeRequest = VaultRest.createDeleteRequest(
      vaultRootToken,
      s"$vaultUrl${RequestPath.vaultPolicy}/$policyName",
      "",
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "delete policy"
    )

    TaskRunner.tryRunUntilSuccess[RestResponse](
      executeRequest,
      vaultRetryDelay
    )

    logger.debug(s"policy with name: $policyName was deleted")
  }
}
