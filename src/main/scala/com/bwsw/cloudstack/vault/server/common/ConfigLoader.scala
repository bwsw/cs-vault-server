package com.bwsw.cloudstack.vault.server.common

import com.bwsw.cloudstack.vault.server.Components
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService
import com.bwsw.cloudstack.vault.server.zookeeper.util.ZooKeeperTaskCreator

/**
  * Created by medvedev_vv on 29.08.17.
  */
object ConfigLoader {

  def loadConfig(): Components.Settings = {
    //zookeeper
    val zooKeeperRetryDelay: Int = ApplicationConfig.getRequiredInt(ConfigLiterals.zooKeeperRetryDelay)
    val zooKeeperUrl: String = ApplicationConfig.getRequiredString(ConfigLiterals.zooKeeperUrl)
    //vault
    val vaultTokenPeriod: Int = ApplicationConfig.getRequiredInt(ConfigLiterals.tokenPeriod)
    val vaultRetryDelay: Int = ApplicationConfig.getRequiredInt(ConfigLiterals.vaultRetryDelay)
    val vaultUrl: String = ApplicationConfig.getRequiredString(ConfigLiterals.vaultUrl)
    val vaultRootToken: String = ApplicationConfig.getRequiredString(ConfigLiterals.vaultRootToken)
    //cloudstack
    val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
    val cloudStackUrlList: Array[String] = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackApiUrlList).split("[,\\s]+")
    val cloudStackSecretKey: String = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackSecretKey)
    val cloudStackApiKey: String = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackApiKey)

    Components.Settings(
      CloudStackService.Settings(cloudStackRetryDelay),
      ApacheCloudStackTaskCreator.Settings(cloudStackUrlList, cloudStackSecretKey, cloudStackApiKey),
      VaultService.Settings(vaultTokenPeriod, vaultRetryDelay),
      VaultRestRequestCreator.Settings(vaultUrl, vaultRootToken),
      ZooKeeperService.Settings(zooKeeperRetryDelay),
      ZooKeeperTaskCreator.Settings(zooKeeperUrl)
    )
  }
}
