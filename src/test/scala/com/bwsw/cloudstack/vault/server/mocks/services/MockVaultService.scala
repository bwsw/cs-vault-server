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
package com.bwsw.cloudstack.vault.server.mocks.services

import java.util.UUID

import com.bwsw.cloudstack.vault.server.mocks.MockConfig
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestExecutor

class MockVaultService extends VaultService (
  new VaultRestRequestExecutor(MockConfig.vaultRestRequestExecutorSettings),
  MockConfig.vaultServiceSettings
){
  override def createToken(policies: List[Policy]): UUID = throw new NotImplementedError("createToken not implemented")

  override def revokeToken(tokenId: UUID): List[String] = throw new NotImplementedError("revokeToken not implemented")

  override def deleteSecretsRecursively(pathToRootSecret: String): Unit = throw new NotImplementedError("deleteSecretsRecursive not implemented")
}
