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
package com.bwsw.cloudstack.vault.server

import com.bwsw.cloudstack.PasswordAuthenticationClientCreator
import com.bwsw.cloudstack.entities.Executor
import com.bwsw.cloudstack.entities.common.JsonMapper
import com.bwsw.cloudstack.entities.dao.{AccountDao, TagDao, VirtualMachineDao}
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.util.IntegrationTestsSettings
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestExecutor


trait IntegrationTestsComponents {
  //common
  lazy val mapper = new JsonMapper(ignoreUnknownProperties = true)
  //vault
  private val requestExecutorSettings = VaultRestRequestExecutor.Settings(
    IntegrationTestsSettings.vaultEndpoints,
    IntegrationTestsSettings.vaultRootToken,
    IntegrationTestsSettings.vaultRetryDelay
  )
  private val vaultServiceSettings = VaultService.Settings(IntegrationTestsSettings.vaultTokenPeriod)

  lazy val vaultRestRequestExecutor = new VaultRestRequestExecutor(requestExecutorSettings)
  lazy val vaultService = new VaultService(vaultRestRequestExecutor, vaultServiceSettings)

  //cloudstack
  private val executorSettings = Executor.Settings(
    IntegrationTestsSettings.cloudStackEndpoints,
    IntegrationTestsSettings.cloudStackRetryDelay
  )
  private val clientSettings = PasswordAuthenticationClientCreator.Settings(
    IntegrationTestsSettings.cloudStackLogin,
    IntegrationTestsSettings.cloudStackPassword,
    domain = "/"
  )

  private lazy val clientCreator = new PasswordAuthenticationClientCreator(clientSettings)
  lazy val executor = new Executor(executorSettings, clientCreator, waitIfServerUnavailable = false)

  lazy val accountDao = new AccountDao(executor, mapper)
  lazy val virtualMachineDao = new VirtualMachineDao(executor, mapper)
  lazy val tagDao = new TagDao(executor, mapper)

  lazy val cloudStackService = new CloudStackService(accountDao, tagDao, virtualMachineDao)
}
