package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.util.{DataPath, RequestPath}

/**
  * Created by medvedev_vv on 04.09.17.
  */
trait TestData {
  val cloudStackVaultControllerSettings = MockConfig.cloudStackVaultControllerSettings
  val accountId = UUID.randomUUID()
  val vmId = UUID.randomUUID()
  val firstUserId = UUID.randomUUID()
  val secondUserId = UUID.randomUUID()
  val readToken = UUID.randomUUID()
  val writeToken = UUID.randomUUID()

  val rootNodePath = cloudStackVaultControllerSettings.zooKeeperRootNode
  val rootNodeAccountPath = s"${cloudStackVaultControllerSettings.zooKeeperRootNode}/accounts"
  val rootNodeVmPath = s"${cloudStackVaultControllerSettings.zooKeeperRootNode}/vms"

  def getDefaultAccountSecretPath(accountId: UUID) = s"${RequestPath.vaultRoot}${cloudStackVaultControllerSettings.accountSecretPath}$accountId"
  def getDefaultVmSecretPath(vmId: UUID) = s"${RequestPath.vaultRoot}${cloudStackVaultControllerSettings.vmSecretPath}$vmId"

  def getVmEntityNodePath(entityId: String) = s"$rootNodePath/vms/$entityId"
  def getVmTokenReadNodePath(entityId: String) = s"$rootNodePath/vms/$entityId/${Tag.prefix}vault.ro"
  def getVmTokenWriteNodePath(entityId: String) = s"$rootNodePath/vms/$entityId/${Tag.prefix}vault.rw"

  def getAccountEntityNodePath(entityId: String) = s"$rootNodePath/accounts/$entityId"
  def getAccountTokenReadNodePath(entityId: String) = s"$rootNodePath/accounts/$entityId/${Tag.prefix}vault.ro"
  def getAccountTokenWriteNodePath(entityId: String) = s"$rootNodePath/accounts/$entityId/${Tag.prefix}vault.rw"
}
