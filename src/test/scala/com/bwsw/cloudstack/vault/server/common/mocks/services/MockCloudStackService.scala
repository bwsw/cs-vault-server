package com.bwsw.cloudstack.vault.server.common.mocks.services

import java.util.UUID

import com.bwsw.cloudstack.vault.server.MockConfig
import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag.Type
import com.bwsw.cloudstack.vault.server.cloudstack.util.CloudStackTaskCreator

/**
  * Created by medvedev_vv on 04.09.17.
  */
class MockCloudStackService extends CloudStackService(
  new CloudStackTaskCreator(MockConfig.cloudStackTaskCreatorSettings),
  MockConfig.cloudStackServiceSettings
){
  override def getUserTagsByAccountId(accountId: UUID): Set[Tag] = throw new NotImplementedError("getUserTagsByAccountId not implemented")

  override def getUserTagsByUserId(userId: UUID): Set[Tag] = throw new NotImplementedError("getUserTagsByUserId not implemented")

  override def getVmTagsById(vmId: UUID): Set[Tag] = throw new NotImplementedError("getVmTagsById not implemented")

  override def getAccountIdByVmId(vmId: UUID): UUID = throw new NotImplementedError("getAccountIdByVmId not implemented")

  override def getAccountIdByUserId(userId: UUID): UUID = throw new NotImplementedError("getAccountIdByUserId not implemented")

  override def getUserIdsByAccountId(accountId: UUID): List[UUID] = throw new NotImplementedError("getUserIdsByAccountId not implemented")

  override def setResourceTags(resourceId: UUID, resourceType: Type, tagSet: Set[Tag]): Unit = throw new NotImplementedError("setResourceTag not implemented")
}
