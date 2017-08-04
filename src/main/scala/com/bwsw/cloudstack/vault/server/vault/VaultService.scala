package com.bwsw.cloudstack.vault.server.vault

import java.util.StringTokenizer

import com.bettercloud.vault.VaultConfig
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, PeriodicChecker}
import com.typesafe.config.ConfigException

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 02.08.17.
  */
class VaultService {
  /*val config: VaultConfig = new VaultConfig()
    .address(ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl))
    .token(ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken))
    .openTimeout(5)
    .readTimeout(10)
    .build()*/
  def getToken(policy: String) = {

  }

  private def checkVaultServer() = {
    Try {
      val retryCount = ApplicationConfig.getRequiredString(ConfigLiterals.vaultRetryCount)
      val tokenizer = new StringTokenizer(ApplicationConfig.getRequiredString(ConfigLiterals.vaultRetryDelay), ":")
      val startDelay = tokenizer.nextToken().toInt
      val endDelay = tokenizer.nextToken().toInt
    } match {
      case Success(_) =>
        val checker = new PeriodicChecker( )
      case Failure(e: NumberFormatException) =>
    }

    //val checker = new PeriodicChecker()
  }
}
