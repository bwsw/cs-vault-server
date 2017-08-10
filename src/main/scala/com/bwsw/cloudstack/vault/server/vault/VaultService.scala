package com.bwsw.cloudstack.vault.server.vault

import java.util.{StringTokenizer, UUID}

import com.bettercloud.vault.VaultException
import com.bettercloud.vault.json.Json
import com.bettercloud.vault.rest.Rest
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util._
import com.bwsw.cloudstack.vault.server.vault.entities.{LookupToken, Policy, Token}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 02.08.17.
  */
class VaultService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createToken(policies: List[Policy]): UUID = {
    logger.debug(s"Create token with policies: $policies")
    if (checkVaultServerWithPariod()) {
      val jsonSerializer = new JsonSerializer(true)
      policies.foreach(writePolicy)

      val tokenParameters = Token.TokenInitParameters(
        policies.map(_.name),
        ApplicationConfig.getRequiredInt(ConfigLiterals.tokenPeriod)*24*60*60 //convert days to seconds
      )

      val response = new Rest()
        .url(s"${ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)}${RequestPath.vaultTokenCreate}")
        .header("X-Vault-Token", ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
        .body(jsonSerializer.serialize(tokenParameters).getBytes("UTF-8"))
        .post()

      if (response.getStatus != HttpStatuses.OK_STATUS) {
        throw new VaultException(s"Could not create the token. Response status: ${response.getStatus}")
      }

      val token = jsonSerializer.deserialize[Token](new String(response.getBody))
      logger.debug(s"token was created: $token")
      token.tokenId.id
    } else {
      throw new VaultException("Vault server is unavailable")
    }
  }

  def revokeToken(tokenId: UUID): Unit = {
    logger.debug(s"revokeToken: $tokenId")
    if (checkVaultServerWithPariod()) {
      val jsonSerializer = new JsonSerializer(true)
      val jsonTokenId = Json.`object`().add("token", tokenId.toString).toString

      val lookupResponse = new Rest()
        .url(s"${ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)}${RequestPath.vaultTokenLookup}")
        .header("X-Vault-Token", ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
        .body(jsonTokenId.getBytes("UTF-8"))
        .post()

      val lookupToken = jsonSerializer.deserialize[LookupToken](new String(lookupResponse.getBody))
      logger.debug(s"lookup token: $lookupToken")

      val revokeResponse = new Rest()
        .url(s"${ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)}${RequestPath.vaultTokenRevoke}")
        .header("X-Vault-Token", ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
        .body(jsonTokenId.getBytes("UTF-8"))
        .post()

      if (revokeResponse.getStatus != HttpStatuses.OK_STATUS_WITH_EMPTY_BODY) {
        throw new VaultException(s"Could not to revoke the token: $tokenId, response status: ${revokeResponse.getStatus}")
      }
      logger.debug(s"Token: $tokenId was revoked")

      lookupToken.tokenData.policies.filter { x =>
        x != "default" && x != "root"
      }.foreach(deletePolicy)

      deleteSecret(lookupToken.tokenData.path)
    }
  }

  private def deleteSecret(pathToSecret: String): Unit = {
    val response = new Rest()
      .url(s"${ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)}${RequestPath.vaultSecret}/$pathToSecret")
      .header("X-Vault-Token", ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
      .delete()

    if (response.getStatus != HttpStatuses.OK_STATUS_WITH_EMPTY_BODY) {
      throw new VaultException(s"Could not to delete the data from path: $pathToSecret, response status: ${response.getStatus}")
    }

    logger.debug(s"data from path: $pathToSecret was deleted")
  }

  private def writePolicy(policy: Policy) = {
    val response = new Rest()
      .url(s"${ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)}${RequestPath.vaultPolicy}/${policy.name}")
      .header("X-Vault-Token", ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
      .body(policy.jsonString.getBytes("UTF-8"))
      .put()

    if (response.getStatus != HttpStatuses.OK_STATUS_WITH_EMPTY_BODY) {
      throw new VaultException(s"Could not to create the policy: ${policy.name}, response status: ${response.getStatus}")
    }

    logger.debug(s"policy was writed: $policy")
  }

  private def deletePolicy(policyName: String) = {
    val response = new Rest()
      .url(s"${ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)}${RequestPath.vaultPolicy}/$policyName")
      .header("X-Vault-Token", ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
      .delete()

    if (response.getStatus != HttpStatuses.OK_STATUS_WITH_EMPTY_BODY) {
      throw new VaultException(s"Could not to delete the policy: $policyName, response status: ${response.getStatus}")
    }
    logger.debug(s"policy with name: $policyName was deleted")
  }

  private def checkVaultServerWithPariod(): Boolean = {
    Try {
      val retryCount = ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryCount)
      val tokenizer = new StringTokenizer(ApplicationConfig.getRequiredString(ConfigLiterals.vaultRetryDelay), ":")
      val startDelay = tokenizer.nextToken().toInt
      val endDelay = tokenizer.nextToken().toInt
      PeriodicChecker.runCheck(checkHealth, startDelay, endDelay, retryCount)
    } match {
      case Success(isAvailable) =>
        logger.debug(s"Vault server is available: $isAvailable")
        isAvailable
      case Failure(e: NumberFormatException) =>
        logger.error("Canfiguration parameters can not to convert to Int. Default configuration will be used")
        PeriodicChecker.runCheck(checkHealth)
    }
  }

  private def checkHealth(): Boolean = {
    val responseStatus = new Rest()
      .url(ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl) + RequestPath.vaultHealthCheckPath)
      .get()
      .getStatus
    responseStatus == HttpStatuses.OK_STATUS
  }
}
