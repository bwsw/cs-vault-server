package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.util.{DataPath, RequestPath}

/**
  * Created by medvedev_vv on 04.09.17.
  */
trait TestData {
  val accountId = UUID.randomUUID()
  val vmId = UUID.randomUUID()
  val firstUserId = UUID.randomUUID()
  val secondUserId = UUID.randomUUID()
  val readToken = UUID.randomUUID()
  val writeToken = UUID.randomUUID()

  val rootNodePath = DataPath.zooKeeperRootNode
  val rootNodeAccountPath = s"${DataPath.zooKeeperRootNode}/accounts"
  val rootNodeVmPath = s"${DataPath.zooKeeperRootNode}/vms"

  def getDefaultAccountSecretPath(accountId: UUID) = s"${RequestPath.vaultSecretAccount}$accountId"
  def getDefaultVmSecretPath(vmId: UUID) = s"${RequestPath.vaultSecretVm}$vmId"

  def getVmEntityNodePath(entityId: String) = s"${DataPath.zooKeeperRootNode}/vms/$entityId"
  def getVmTokenReadNodePath(entityId: String) = s"${DataPath.zooKeeperRootNode}/vms/$entityId/${Tag.prefix}vault.ro"
  def getVmTokenWriteNodePath(entityId: String) = s"${DataPath.zooKeeperRootNode}/vms/$entityId/${Tag.prefix}vault.rw"

  def getAccountEntityNodePath(entityId: String) = s"${DataPath.zooKeeperRootNode}/accounts/$entityId"
  def getAccountTokenReadNodePath(entityId: String) = s"${DataPath.zooKeeperRootNode}/accounts/$entityId/${Tag.prefix}vault.ro"
  def getAccountTokenWriteNodePath(entityId: String) = s"${DataPath.zooKeeperRootNode}/accounts/$entityId/${Tag.prefix}vault.rw"
}
