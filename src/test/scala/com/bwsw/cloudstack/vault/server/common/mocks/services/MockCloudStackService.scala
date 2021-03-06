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
package com.bwsw.cloudstack.vault.server.common.mocks.services

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Type
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator

class MockCloudStackService extends CloudStackService(
  new CloudStackTaskCreator(MockConfig.cloudStackTaskCreatorSettings),
  MockConfig.cloudStackServiceSettings
){
  override def getUserTagsByAccount(accountId: UUID): Set[Tag] = throw new NotImplementedError("getUserTagsByAccountId not implemented")

  override def getUserTags(userId: UUID): Set[Tag] = throw new NotImplementedError("getUserTagsByUserId not implemented")

  override def getVmTags(vmId: UUID): Set[Tag] = throw new NotImplementedError("getVmTagsById not implemented")

  override def getVmOwnerAccount(vmId: UUID): UUID = throw new NotImplementedError("getAccountIdByVmId not implemented")

  override def getAccountByUser(userId: UUID): UUID = throw new NotImplementedError("getAccountIdByUserId not implemented")

  override def getUsersByAccount(accountId: UUID): List[UUID] = throw new NotImplementedError("getUsersByAccount not implemented")

  override def setResourceTags(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): Unit = throw new NotImplementedError("setResourceTag not implemented")
}
