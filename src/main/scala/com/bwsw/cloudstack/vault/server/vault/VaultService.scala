package com.bwsw.cloudstack.vault.server.vault

import java.util.UUID

import com.bettercloud.vault.VaultException
import com.bettercloud.vault.json.Json
import com.bettercloud.vault.rest.{Rest, RestResponse}
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util._
import com.bwsw.cloudstack.vault.server.vault.entities.{LookupToken, Policy, Token}
import org.slf4j.LoggerFactory

/**
  * Created by medvedev_vv on 02.08.17.
  */
class VaultService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createToken(policies: List[Policy])(): UUID = {
    logger.debug(s"Create token with policies: $policies")
    val jsonSerializer = new JsonSerializer(true)
    policies.foreach(writePolicy)

    val tokenParameters = Token.TokenInitParameters(
      policies.map(_.name),
      ApplicationConfig.getRequiredInt(ConfigLiterals.tokenPeriod)*24*60*60 //convert days to seconds
    )

    val request = getRequest(RequestPath.vaultTokenCreate, jsonSerializer.serialize(tokenParameters))

    val response = PeriodicRunner.runMethod[RestResponse](
      executeRequest(request.post, HttpStatuses.OK_STATUS, "create token"),
      ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    )

    val token = jsonSerializer.deserialize[Token](new String(response.getBody))
    logger.debug(s"Token was created")
    token.tokenId.id
  }

  def revokeToken(tokenId: UUID)(): Unit = {
    logger.debug(s"revokeToken")
    val jsonSerializer = new JsonSerializer(true)
    val jsonTokenId = Json.`object`().add("token", tokenId.toString).toString

    val lookupRequest = getRequest(RequestPath.vaultTokenLookup, jsonTokenId)

    val lookupResponse = PeriodicRunner.runMethod[RestResponse](
      executeRequest(lookupRequest.post, HttpStatuses.OK_STATUS, "get lookup token"),
      ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    )

    val lookupToken = jsonSerializer.deserialize[LookupToken](new String(lookupResponse.getBody))

    val revokeRequest = getRequest(RequestPath.vaultTokenRevoke, jsonTokenId)

    val revokeResponse = PeriodicRunner.runMethod[RestResponse](
      executeRequest(revokeRequest.post, HttpStatuses.OK_STATUS_WITH_EMPTY_BODY, "revoke token"),
      ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    )
    logger.debug(s"Token was revoked")

    lookupToken.tokenData.policies.filter { x =>
      x != "default" && x != "root"
    }.foreach(deletePolicy)

    deleteSecret(lookupToken.tokenData.path)
  }

  private def deleteSecret(pathToSecret: String): Unit = {
    logger.debug(s"deleteSecret: $pathToSecret")
    val request = getRequest(s"${RequestPath.vaultSecret}/$pathToSecret", "")

    PeriodicRunner.runMethod[RestResponse](
      executeRequest(request.delete, HttpStatuses.OK_STATUS_WITH_EMPTY_BODY, "delete secret"),
      ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    )

    logger.debug(s"data from path: $pathToSecret was deleted")
  }

  private def writePolicy(policy: Policy) = {
    logger.debug(s"writePolicy: $policy")

    val request = getRequest(s"${RequestPath.vaultPolicy}/${policy.name}", policy.jsonString)

    PeriodicRunner.runMethod[RestResponse](
      executeRequest(request.put, HttpStatuses.OK_STATUS_WITH_EMPTY_BODY, "write policy"),
      ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    )
    logger.debug(s"policy was writed: $policy")
  }

  private def deletePolicy(policyName: String) = {
    logger.debug(s"deletePolicy: $policyName")

    val request = getRequest(s"${RequestPath.vaultPolicy}/$policyName", "")

    PeriodicRunner.runMethod[RestResponse](
      executeRequest(request.delete, HttpStatuses.OK_STATUS_WITH_EMPTY_BODY, "delete policy"),
      ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    )

    logger.debug(s"policy with name: $policyName was deleted")
  }

  private def executeRequest(method: () => RestResponse,
                             expectedResponseStatus: Int,
                             performedAction: String)() = {
    logger.debug(s"Ececute request for: $performedAction")
    val response = method()

    if (response.getStatus != expectedResponseStatus) {
      throw new VaultException(s"Response status: ${response.getStatus} is not expected")
    }
    response
  }

  private def getRequest(requestPath: String, data: String): Rest = {
    new Rest()
      .url(s"${ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)}$requestPath")
      .header("X-Vault-Token", ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
      .body(data.getBytes("UTF-8"))
  }
}
