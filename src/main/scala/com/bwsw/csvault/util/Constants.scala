package com.bwsw.csvault.util

/**
  * Created by medvedev_vv on 31.07.17.
  */
object ConfigLiterals {
  final val applicationDomain = "app"
  final val kafkaDomain = s"$applicationDomain.kafka"
  final val vaultDomain = s"$applicationDomain.vault"
  final val cloudStackDomain = s"$applicationDomain.cs"

  val tokenPeriod = s"$applicationDomain.tokenPeriod"
  val accountsVaultBasicPath = s"$applicationDomain.accountsVaultBasicPath"

  val kafkaServerList = s"$kafkaDomain.serverList"
  val kafkaTopic = s"$kafkaDomain.topic"

  val vaultUrl = s"$vaultDomain.url"
  val vaultRootToken = s"$vaultDomain.rootToken"
  val vaultRetryDelay = s"$vaultDomain.retryDelay"
  val vaultRetryCount = s"$vaultDomain.retryCount"

  val csApiUrlList = s"$cloudStackDomain.apiUrlList"
  val csApiKey = s"$cloudStackDomain.apiKey"
  val csSecretKey = s"$cloudStackDomain.secretKey"
  val csRetryDelay = s"$cloudStackDomain.retryDelay"
  val csRetryCount = s"$cloudStackDomain.retryCount"
}
