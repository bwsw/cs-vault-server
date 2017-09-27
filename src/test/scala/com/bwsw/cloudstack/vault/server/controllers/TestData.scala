package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.util.RequestPath

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

  val rootNodePath = RequestPath.zooKeeperRootNode
  val rootNodeAccountPath = s"${RequestPath.zooKeeperRootNode}/account"
  val rootNodeVmPath = s"${RequestPath.zooKeeperRootNode}/vm"

  def getTestPathToAccountSecretFromToken(accountId: UUID) = s"${RequestPath.accountSecret}test/path/$accountId"
  def getTestPathToVmSecretFromToken(vmId: UUID) = s"${RequestPath.vmSecret}/test/path/$vmId"
  def getDefaultAccountSecretPath(accountId: UUID) = s"${RequestPath.accountSecret}$accountId"
  def getDefaultVmSecretPath(vmId: UUID) = s"${RequestPath.vmSecret}$vmId"

  def getVmEntityNodePath(entityId: String) = s"${RequestPath.zooKeeperRootNode}/vm/$entityId"
  def getVmTokenReadNodePath(entityId: String) = s"${RequestPath.zooKeeperRootNode}/vm/$entityId/vault.ro"
  def getVmTokenWriteNodePath(entityId: String) = s"${RequestPath.zooKeeperRootNode}/vm/$entityId/vault.rw"

  def getAccountEntityNodePath(entityId: String) = s"${RequestPath.zooKeeperRootNode}/account/$entityId"
  def getAccountTokenReadNodePath(entityId: String) = s"${RequestPath.zooKeeperRootNode}/account/$entityId/vault.ro"
  def getAccountTokenWriteNodePath(entityId: String) = s"${RequestPath.zooKeeperRootNode}/account/$entityId/vault.rw"
}
