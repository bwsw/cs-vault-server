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
  override def getUserTagsByAccount(accountId: UUID): List[Tag] = throw new NotImplementedError("getUserTagsByAccountId not implemented")

  override def getUserTags(userId: UUID): List[Tag] = throw new NotImplementedError("getUserTagsByUserId not implemented")

  override def getVmTags(vmId: UUID): List[Tag] = throw new NotImplementedError("getVmTagsById not implemented")

  override def getVmOwnerAccount(vmId: UUID): UUID = throw new NotImplementedError("getAccountIdByVmId not implemented")

  override def getAccountByUser(userId: UUID): UUID = throw new NotImplementedError("getAccountIdByUserId not implemented")

  override def getUsersByAccount(accountId: UUID): List[UUID] = throw new NotImplementedError("getUsersByAccount not implemented")

  override def setResourceTags(resourceId: UUID, resourceType: Type, tagList: List[Tag]): Unit = throw new NotImplementedError("setResourceTag not implemented")
}
