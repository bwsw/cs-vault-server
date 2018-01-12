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
package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig._
import com.bwsw.cloudstack.vault.server.BaseTestSuite
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Type
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import org.scalatest.FlatSpec

class CloudStackServiceTestSuite extends FlatSpec with TestData with BaseTestSuite {

  //Positive tests
  "getAccountTags" should "return account tags by id" in {
    val key = Tag.Key.VaultRW
    val value = "value1"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagsTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.Account, "resourceType is wrong")
        assert(resourceId == accountId, "resourceId is wrong")
        () => Response.getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val tags = cloudStackService.getAccountTags(accountId)
    assert(Set(Tag(key,value)) == tags)
  }

  "getVmTags" should "return VM tags by id" in {
    val key = Tag.Key.VaultRW
    val value = "value3"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagsTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        () => Response.getTagResponseJson(key, value)
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val tags = cloudStackService.getVmTags(vmId)
    assert(Set(Tag(key,value)) == tags)
  }

  "getVmOwnerAccount" should "return account id by VM id" in {
    val accountName = "admin"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(command: Command, parameters: Map[String, String]): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            assert(Map(idParameter -> vmId.toString) == parameters, "parameters is wrong")
            () => Response.getVmResponseJson(vmId.toString, accountName, domainId.toString)
          case Command.ListAccounts =>
            assert(
              Map(nameParameter -> accountName, domainParameter -> domainId.toString) == parameters,
              "parameters is wrong"
            )
            () => Response.getAccountResponseJson(accountId.toString)
          case _ =>
            assert(false, "command is wrong")
            () => ""
        }
      }
    }

    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    val expectedAccountId = cloudStackService.getVmOwnerAccount(vmId)
    assert(expectedAccountId == accountId)
  }

  "setResourceTags" should "create CloudStack request for creating new tag in VM" in {
    val key = Tag.Key.VaultRW
    val value = "value1"

    val cloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings) {
      override def createSetResourceTagsTask(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): () => Unit = {
        assert(resourceId == vmId, "resourceId is wrong")
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(tagSet == Set(Tag.createTag(key, value)), "set of tags is wrong")
        () => Unit
      }
    }
    val cloudStackService = new CloudStackService(cloudStackTaskCreator, cloudStackServiceSettings)

    assert(cloudStackService.setResourceTags(vmId, Tag.Type.UserVM, Set(Tag.createTag(key, value))).isInstanceOf[Unit])
  }

  //Negative tests
  "getAccountTags" should "not swallow CloudStackFatalException" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagsTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.Account, "resourceType is wrong")
        assert(resourceId == accountId, "resourceId is wrong")
        throw new CloudStackFatalException("test exception")
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getAccountTags(accountId)
    }
  }

  "getVmTags" should "not swallow CloudStackFatalException" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetTagsTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
        assert(resourceType == Tag.Type.UserVM, "resourceType is wrong")
        assert(resourceId == vmId, "resourceId is wrong")
        throw new CloudStackFatalException("test exception")
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getVmTags(vmId)
    }
  }

  "getVmOwnerAccount" should "not swallow CloudStackFatalException" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(command: Command, parameters: Map[String, String]): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            assert(Map(idParameter -> vmId.toString) == parameters, "parameters is wrong")
            throw new CloudStackFatalException("test exception")
          case _ => throw new IllegalArgumentException
        }
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)

    assertThrows[CloudStackFatalException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getVmOwnerAccount" should "throw CloudStackEntityDoesNotExistException if VM with specified id does not exist" in {
    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(command: Command, parameters: Map[String, String]): () => String = {
        assert(Map(idParameter -> vmId.toString) == parameters, "parameters is wrong")
        assert(command == Command.ListVirtualMachines, "command is wrong")
        () => Response.getResponseWithEmptyVmList
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)
    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

  "getVmOwnerAccount" should "throw CloudStackEntityDoesNotExistException if account with specified name does not exist" in {
    val accountName = "accountName"

    val сloudStackTaskCreator = new CloudStackTaskCreator(cloudStackTaskCreatorSettings)  {
      override def createGetEntityTask(command: Command, parameters: Map[String, String]): () => String = {
        command match {
          case Command.ListVirtualMachines =>
            () => Response.getVmResponseJson(vmId.toString, accountName, domainId.toString)
          case Command.ListAccounts =>
            assert(
              Map(nameParameter -> accountName.toString, domainParameter -> domainId.toString) == parameters,
              "parameters is wrong"
            )
            () => Response.getResponseWithEmptyAccountList
          case _ => throw new IllegalArgumentException
        }
      }
    }

    val cloudStackService = new CloudStackService(сloudStackTaskCreator, cloudStackServiceSettings)
    assertThrows[CloudStackEntityDoesNotExistException] {
      cloudStackService.getVmOwnerAccount(vmId)
    }
  }

}
