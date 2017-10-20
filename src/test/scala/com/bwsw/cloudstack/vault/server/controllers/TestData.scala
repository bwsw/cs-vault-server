/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.util.{DataPath, RequestPath}

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

  def getDefaultAccountSecretPath(accountId: UUID) = s"${cloudStackVaultControllerSettings.accountSecretPath}$accountId"
  def getDefaultVmSecretPath(vmId: UUID) = s"${cloudStackVaultControllerSettings.vmSecretPath}$vmId"
  def getDefaultRequestAccountSecretPath(accountId: UUID) = s"${RequestPath.vaultRoot}${cloudStackVaultControllerSettings.accountSecretPath}$accountId"
  def getDefaultRequestVmSecretPath(vmId: UUID) = s"${RequestPath.vaultRoot}${cloudStackVaultControllerSettings.vmSecretPath}$vmId"

  def getVmEntityNodePath(entityId: String) = s"$rootNodePath/vms/$entityId"
  def getVmTokenReadNodePath(entityId: String) = s"$rootNodePath/vms/$entityId/vaultro"
  def getVmTokenWriteNodePath(entityId: String) = s"$rootNodePath/vms/$entityId/vaultrw"

  def getAccountEntityNodePath(entityId: String) = s"$rootNodePath/accounts/$entityId"
  def getAccountTokenReadNodePath(entityId: String) = s"$rootNodePath/accounts/$entityId/vaultro"
  def getAccountTokenWriteNodePath(entityId: String) = s"$rootNodePath/accounts/$entityId/vaultrw"
}
