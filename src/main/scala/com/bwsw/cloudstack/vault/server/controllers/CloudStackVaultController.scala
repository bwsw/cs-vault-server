package com.bwsw.cloudstack.vault.server.controllers

import java.util.UUID

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.util.RequestPath
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService

import scala.concurrent.ExecutionContext

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackVaultController(vaultService: VaultService,
                                cloudStackService: CloudStackService,
                                zooKeeperService: ZooKeeperService) {
  def handleAccountDelete(){}
  def handleVmDelete(){}

  def handleUserCreate(userId: UUID): Unit = {
    import Tag.Key
    var tokenList: List[UUID] = List.empty
    val userTags = cloudStackService.getUserTagByUserId(userId)

    val optionReadToken: Option[UUID] = userTags.find(_.key == Key.VaultRO).map { x =>
      UUID.fromString(x.value)
    }
    val optionWriteToken: Option[UUID] = userTags.find(_.key == Key.VaultRW).map { x =>
      UUID.fromString(x.value)
    }

    val optionTokenWithNameList = List(
      (optionReadToken, Tag.Key.VaultRO),
      (optionWriteToken, Tag.Key.VaultRW)
    )

    optionTokenWithNameList.map {
      case (None, name) =>
        val accountId = cloudStackService.getAccountIdForUser(userId)
        val pathToData = s"${RequestPath.zooKeeperRootNode}/$accountId/$name"
        val isExistNodeWithToken = zooKeeperService.isExistNode(pathToData)
        val token = if (isExistNodeWithToken) {
          zooKeeperService.getData(pathToData)
        } else {

        }
      case (Some(x), name) =>
    }

    def checkTokenFromZooKeeper(entityId: UUID, tokenName: String): Boolean = {
      zooKeeperService.isExistNode(s"${RequestPath.zooKeeperRootNode}/$entityId/$tokenName")
    }

  }

  def handleAccountCreate(accountId: UUID): Unit = {
    import Tag.Key
    val readPolicy = Policy.createAccountReadPolicy(accountId)
    val writePolicy = Policy.createAccountWritePolicy(accountId)

    val tokenWithTagKeyList = createTokenListByPolicies(List(
      (readPolicy, Key.VaultRO),
      (writePolicy, Key.VaultRW)
    ))

    val userIdList = cloudStackService.getUserIdListForAccount(accountId)

    userIdList.foreach { x =>
      writeTokensToCloudStackTags(tokenWithTagKeyList, x, Tag.Type.UserVM)
    }

    writeTokensToZooKeeperNodes(tokenWithTagKeyList, accountId)
  }

  def handleVmCreate(vmId: UUID): Unit = {
    import Tag.Key

    val accountId = cloudStackService.getAccountIdForVm(vmId)
    val readPolicy = Policy.createVmReadPolicy(accountId, vmId)
    val writePolicy = Policy.createVmWritePolicy(accountId, vmId)

    val tokenWithTagKeyList = createTokenListByPolicies(List(
      (readPolicy, Key.VaultRO),
      (writePolicy, Key.VaultRW)
    ))

    writeTokensToCloudStackTags(tokenWithTagKeyList, vmId, Tag.Type.UserVM)
    writeTokensToZooKeeperNodes(tokenWithTagKeyList, vmId)
  }

  private def createTokenListByPolicies(policies: List[(Policy, Tag.Key)]): List[(UUID, Tag.Key)] = {
    policies.map {
      case (policy, tokenTagKey) =>
        val token = vaultService.createToken(policy :: Nil)
        (token, tokenTagKey)
    }
  }

  private def writeTokensToCloudStackTags(tokenWithTagKeyList: List[(UUID, Tag.Key)],
                                          entityId: UUID,
                                          tagType: Tag.Type) = {
    val tagList = tokenWithTagKeyList.map {
      case (tokenId, tokenTagKey) =>
        Tag.createTag(tokenTagKey, tokenId.toString)
    }

    tagList.foreach { x =>
      cloudStackService.setTagToResourse(x, entityId, tagType)
    }
  }

  private def writeTokensToZooKeeperNodes(tokenWithTagKeyList: List[(UUID, Tag.Key)], entityId: UUID) = {
    if (!zooKeeperService.isExistNode(RequestPath.zooKeeperRootNode)) {
      zooKeeperService.createNode(RequestPath.zooKeeperRootNode, "")
    }
    zooKeeperService.createNode(s"${RequestPath.zooKeeperRootNode}/$entityId", "")

    tokenWithTagKeyList.foreach {
      case (tokenId, tokenTagKey) =>
        zooKeeperService.createNode(s"${RequestPath.zooKeeperRootNode}/$entityId/$tokenTagKey", s"$tokenId")
    }
  }
}
