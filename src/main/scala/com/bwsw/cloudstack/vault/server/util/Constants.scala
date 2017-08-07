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
  val vaultRetryCount = s"$vaultDomain.retryCount"

  val cloudStackApiUrlList = s"$cloudStackDomain.apiUrlList"
  val cloudStackApiKey = s"$cloudStackDomain.apiKey"
  val cloudStackSecretKey = s"$cloudStackDomain.secretKey"
  val cloudStackRetryDelay = s"$cloudStackDomain.retryDelay"
  val cloudStackRetryCount = s"$cloudStackDomain.retryCount"
}
