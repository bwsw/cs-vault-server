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
package com.bwsw.cloudstack.vault.server.e2e

import java.util.UUID

import com.bwsw.cloudstack.entities.requests.tag.TagFindRequest
import com.bwsw.cloudstack.entities.requests.tag.types.AccountTagType
import com.bwsw.cloudstack.vault.server.cloudstack.entities.VaultTagKey
import com.bwsw.cloudstack.vault.server.util.IntegrationTestsSettings
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FaultToleranceVaultTestSuite extends FlatSpec with Checks with BeforeAndAfterAll {
  import sys.process._

  commitToEndForGroup(IntegrationTestsSettings.kafkaGroupId)

  val components = new TestComponents

  Future(components.eventManager.execute())

  "cs-vault-server" should "handle account creation if vault server was been unavailable and then available" in {
    //stop vault docker container
    val stopDockerCommand = s"docker stop ${IntegrationTestsSettings.vaultDockerContainerName}"
    stopDockerCommand !
    
    val accountId = UUID.randomUUID()
    accountDao.create(getAccountCreateRequest.withId(accountId))

    //wait account creation in CloudStack server
    Thread.sleep(15000)

    val emptyTags = tagDao.find(new TagFindRequest().withResource(accountId).withResourceType(AccountTagType))
    assert(emptyTags.isEmpty)

    //run vault docker container
    val dockerRunCommand = "docker run --cap-add IPC_LOCK " +
      s"-e VAULT_DEV_ROOT_TOKEN_ID=${IntegrationTestsSettings.vaultRootToken} " +
      s"-e VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:${IntegrationTestsSettings.vaultPort} " +
      s"-p ${IntegrationTestsSettings.vaultPort}:${IntegrationTestsSettings.vaultPort} " +
      s"--rm -d --name ${IntegrationTestsSettings.vaultDockerContainerName} vault:${IntegrationTestsSettings.vaultVersion}"
    dockerRunCommand !

    //wait account creation handling
    Thread.sleep(15000)

    //check vault token tags creation
    val tags = tagDao.find(new TagFindRequest().withResource(accountId).withResourceType(AccountTagType))

    val roTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRO
    }

    val rwTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRW
    }

    assert(roTokenTagOpt.nonEmpty)
    assert(rwTokenTagOpt.nonEmpty)
  }

  override def afterAll(): Unit = {
    components.close()
  }
}
