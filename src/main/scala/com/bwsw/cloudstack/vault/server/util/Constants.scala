package com.bwsw.cloudstack.vault.server.util

/**
  * Created by medvedev_vv on 31.07.17.
  */
object ConfigLiterals {
  final val applicationDomain = "app"
  final val kafkaDomain = s"$applicationDomain.kafka"
  final val vaultDomain = s"$applicationDomain.vault"
  final val cloudStackDomain = s"$applicationDomain.cloudStack"

  val tokenPeriod = s"$applicationDomain.tokenPeriod"
  val accountsVaultBasicPath = s"$applicationDomain.accountsVaultBasicPath"

  val kafkaServerList = s"$kafkaDomain.serverList"
  val kafkaTopic = s"$kafkaDomain.topic"

  val vaultUrl = s"$vaultDomain.url"
  val vaultRootToken = s"$vaultDomain.rootToken"
  val vaultRetryDelay = s"$vaultDomain.retryDelay"

  val cloudStackApiUrlList = s"$cloudStackDomain.apiUrlList"
  val cloudStackApiKey = s"$cloudStackDomain.apiKey"
  val cloudStackSecretKey = s"$cloudStackDomain.secretKey"
  val cloudStackRetryDelay = s"$cloudStackDomain.retryDelay"
}

object HttpStatuses {
  val OK_STATUS = 200
  val OK_STATUS_WITH_EMPTY_BODY = 204
}

object RequestPath {
  val vaultHealthCheckPath = "/v1/sys/health"
  val vaultTokenCreate     = "/v1/auth/token/create"
  val vaultTokenLookup     = "/v1/auth/token/lookup"
  val vaultPolicy          = "/v1/sys/policy"
  val vaultSecret          = "/v1/secret"
  val vaultTokenRevoke     = "/v1/auth/token/revoke"
  val vmSecret             = "secret/cs/vms/"
  val accountSecret        = "secret/cs/accounts/"
}
