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
import com.bwsw.cloudstack.vault.server.util.vault.components.FaultToleranceVaultTestComponents
import com.bwsw.cloudstack.vault.server.util.{IntegrationTestsSettings, TestComponents}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FaultToleranceVaultTestSuite extends FlatSpec with Checks with BeforeAndAfterAll {
  commitToEndForGroup(IntegrationTestsSettings.kafkaGroupId)

  val components = new TestComponents(new FaultToleranceVaultTestComponents)

  Future(components.eventManager.execute())

  "cs-vault-server" should "handle account creation if vault server was been unavailable and then available" in {
    import sys.process._

    val accountId = UUID.randomUUID()
    val accountCreateRequest = getAccountCreateRequest
    accountCreateRequest.withId(accountId)
    accountDao.create(accountCreateRequest)

    //wait account creation in CloudStack server
    Thread.sleep(5000)

    val emptyTags = tagDao.find({
      val tagFindRequest = new TagFindRequest
      tagFindRequest.withResource(accountId)
      tagFindRequest.withResourceType(AccountTagType)
      tagFindRequest
    })

    assert(emptyTags.isEmpty)

    //run vault docker container
    val dockerRunCommand = "docker run " +
      s"-e VAULT_DEV_ROOT_TOKEN_ID=${IntegrationTestsSettings.FaultTolerance.vaultRootToken} " +
      s"-e VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:${IntegrationTestsSettings.FaultTolerance.vaultPort} " +
      s"-p ${IntegrationTestsSettings.FaultTolerance.vaultPort}:${IntegrationTestsSettings.FaultTolerance.vaultPort} " +
      s"--cap-add=IPC_LOCK --rm -d --name ${IntegrationTestsSettings.FaultTolerance.vaultDockerContainerName} " +
      s"vault:${IntegrationTestsSettings.FaultTolerance.vaultVersion}"
    dockerRunCommand.!

    //wait account creation handling
    Thread.sleep(20000)

    //check vault token tags creation
    val tags = tagDao.find({
      val tagFindRequest = new TagFindRequest
      tagFindRequest.withResource(accountId)
      tagFindRequest.withResourceType(AccountTagType)
      tagFindRequest
    })

    val roTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRO
    }

    val rwTokenTagOpt = tags.find { tag =>
      VaultTagKey.fromString(tag.key) == VaultTagKey.VaultRW
    }

    assert(roTokenTagOpt.nonEmpty, s"tags: $tags are not containing tag with key: ${VaultTagKey.toString(VaultTagKey.VaultRO)}")
    assert(rwTokenTagOpt.nonEmpty, s"tags: $tags are not containing tag with key: ${VaultTagKey.toString(VaultTagKey.VaultRW)}")
  }

  override def afterAll(): Unit = {
    import sys.process._
    //stop vault docker container
    val stopDockerCommand = s"docker stop ${IntegrationTestsSettings.FaultTolerance.vaultDockerContainerName}"
    stopDockerCommand.!

    components.close()
  }
}
