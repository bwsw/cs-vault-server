package com.bwsw.cloudstack.vault.server.vault.util

import com.bettercloud.vault.VaultException
import com.bettercloud.vault.rest.{Rest, RestException, RestResponse}
import com.bwsw.cloudstack.vault.server.util.{HttpStatuses, RequestPath}
import com.bwsw.cloudstack.vault.server.vault.util.exception.VaultCriticalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 21.08.17.
  */
class VaultRestRequestCreator(settings: VaultRestRequestCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def createTokenCreateRequest(tokenParameters: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenCreate}", tokenParameters).post,
      HttpStatuses.OK_STATUS,
      "create token"
    )
  }

  def createTokenRevokeRequest(jsonTokenId: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenRevoke}", jsonTokenId).post,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "revoke token"
    )
  }

  def createPolicyCreateRequest(policyName: String, policyJson: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", policyJson).put,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "write policy"
    )
  }

  def createPolicyDeleteRequest(policyName: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultPolicy}/$policyName", "").delete,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "delete policy"
    )
  }

  def createTokenLookupRequest(jsonTokenId: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultTokenLookup}", jsonTokenId).post,
      HttpStatuses.OK_STATUS,
      "get lookup token"
    )
  }

  def createDeleteSecretRequest(pathToSecret: String):() => String = {
    createRequest(
      createRest(s"${RequestPath.vaultSecret}/$pathToSecret", "").delete,
      HttpStatuses.OK_STATUS_WITH_EMPTY_BODY,
      "delete secret"
    )
  }

  protected def createRest(path: String, data: String): Rest = {
    new Rest()
      .url(s"${settings.vaultUrl}$path")
      .header("X-Vault-Token", settings.vaultRootToken)
      .body(data.getBytes("UTF-8"))
  }

  private def createRequest(request: () => RestResponse,
                            expectedResponseStatus: Int,
                            requestDescription: String)(): String = {
    logger.debug(s"Request was executed for: $requestDescription")
    val response = Try {
      request()
    } match {
      case Success(x) => x
      case Failure(e: RestException) =>
        logger.warn("Vault server is unavailable")
        throw e
      case Failure(e: Throwable) =>
        logger.error(s"Request to vault server is not finish correctly, exception was thrown: $e")
        throw new VaultCriticalException(e)
    }

    if (response.getStatus != expectedResponseStatus) {
      throw new VaultCriticalException(new VaultException(s"Response status: ${response.getStatus} from vault server is not expected"))
    }
    new String(response.getBody)
  }
}

object VaultRestRequestCreator {
  case class Settings(vaultUrl: String, vaultRootToken: String)
}
