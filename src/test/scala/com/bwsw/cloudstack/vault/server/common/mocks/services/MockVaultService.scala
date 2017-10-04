package com.bwsw.cloudstack.vault.server.common.mocks.services

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.vault.util.VaultRestRequestCreator

/**
  * Created by medvedev_vv on 04.09.17.
  */
class MockVaultService extends VaultService (
  new VaultRestRequestCreator(MockConfig.vaultRestRequestCreatorSettings),
  MockConfig.vaultServiceSettings
){
  override def createToken(policies: List[Policy]): UUID = throw new NotImplementedError("createToken not implemented")

  override def revokeToken(tokenId: UUID): List[String] = throw new NotImplementedError("revokeToken not implemented")

  override def deleteSecretsRecursively(pathToRootSecret: String): Unit = throw new NotImplementedError("deleteSecretsRecursive not implemented")
}
