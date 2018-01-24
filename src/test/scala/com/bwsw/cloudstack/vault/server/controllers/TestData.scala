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

import java.nio.file.Paths
import java.util.UUID

import com.bwsw.cloudstack.vault.server.mocks.MockConfig
import com.bwsw.cloudstack.vault.server.util.RequestPath

trait TestData {
  val cloudStackVaultControllerSettings = MockConfig.cloudStackVaultControllerSettings
  val accountId = UUID.randomUUID()
  val vmId = UUID.randomUUID()
  val firstUserId = UUID.randomUUID()
  val secondUserId = UUID.randomUUID()
  val readToken = UUID.randomUUID()
  val writeToken = UUID.randomUUID()

  val rootNodePath = cloudStackVaultControllerSettings.zooKeeperRootNode
  val rootNodeAccountPath = Paths.get(cloudStackVaultControllerSettings.zooKeeperRootNode, "accounts").toString
  val rootNodeVmPath = Paths.get(cloudStackVaultControllerSettings.zooKeeperRootNode, "vms").toString

  def getDefaultAccountSecretPath(accountId: UUID): String = Paths.get(
    cloudStackVaultControllerSettings.accountSecretPath,
    accountId.toString
  ).toString
  def getDefaultVmSecretPath(vmId: UUID): String = Paths.get(
    cloudStackVaultControllerSettings.vmSecretPath,
    vmId.toString
  ).toString
  def getDefaultRequestAccountSecretPath(accountId: UUID): String = Paths.get(
    RequestPath.vaultRoot,
    cloudStackVaultControllerSettings.accountSecretPath,
    accountId.toString
  ).toString
  def getDefaultRequestVmSecretPath(vmId: UUID): String = Paths.get(
    RequestPath.vaultRoot,
    cloudStackVaultControllerSettings.vmSecretPath,
    vmId.toString
  ).toString

  def getVmEntityNodePath(entityId: String): String = Paths.get(rootNodePath, "vms", entityId).toString
  def getVmTokenReadNodePath(entityId: String): String = Paths.get(rootNodePath, "vms", entityId, "vaultro").toString
  def getVmTokenWriteNodePath(entityId: String): String = Paths.get(rootNodePath, "vms", entityId, "vaultrw").toString

  def getAccountEntityNodePath(entityId: String): String = Paths.get(rootNodePath, "accounts", entityId).toString
  def getAccountTokenReadNodePath(entityId: String): String = Paths.get(rootNodePath, "accounts", entityId, "vaultro").toString
  def getAccountTokenWriteNodePath(entityId: String): String = Paths.get(rootNodePath,"accounts", entityId, "vaultrw").toString
}
