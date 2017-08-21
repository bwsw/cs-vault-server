package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import br.com.autonomiccs.apacheCloudStack.client.{ApacheCloudStackClient, ApacheCloudStackRequest}
import br.com.autonomiccs.apacheCloudStack.client.beans.ApacheCloudStackUser
import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, PeriodicRunner}

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackService {
  private val secretKey = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackSecretKey)
  private val apiKey = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackApiKey)
  private val apacheCloudStackUser = new ApacheCloudStackUser(secretKey, apiKey)
  private val cloudStackUrlList: Array[String] = ApplicationConfig
    .getRequiredString(ConfigLiterals.cloudStackApiUrlList)
    .split("[,\\s]+")
  private val apacheCloudStackClientList = cloudStackUrlList.map { x =>
    new ApacheCloudStackClient(x, apacheCloudStackUser)
  }.toList
  apacheCloudStackClientList.foreach(_.setValidateServerHttpsCertificate(false))

  private var threadLocalClientList: ThreadLocal[List[ApacheCloudStackClient]] = new ThreadLocal
  threadLocalClientList.set(apacheCloudStackClientList)

  def getUserTagByAccountId(accountId: UUID): List[Tag] = {
    val jsonSerializer = new JsonSerializer(true)

    val listAccountResponse = getEntityJsonById(accountId, "id", "listAccounts")
    val allUsersIdInAccount = jsonSerializer.deserialize[ListAccountResponse](listAccountResponse)
      .accountResponse
      .accounts
      .flatMap { x =>
        x.users.map(_.id)
      }

    allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getJsonTags("User", userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }
  }

  def getUserTagByUserId(userId: UUID): List[Tag] = {
    val jsonSerializer = new JsonSerializer(true)

    val userList = jsonSerializer.deserialize[ListUserResponse](
      getEntityJsonById(userId, "id", "listUsers")
    ).userResponse.users

    val allUsersIdInAccount: List[UUID] = userList.flatMap { x =>
      val listAccountResponse = getEntityJsonById(x.accountid, "id", "listAccounts")
      jsonSerializer.deserialize[ListAccountResponse](listAccountResponse).accountResponse.accounts.flatMap { x =>
        x.users.map(_.id)
      }
    }

    allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getJsonTags("User", userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }
  }

  def getVMTagById(vmId: UUID): List[Tag] = {
    val jsonSerializer = new JsonSerializer(true)

    val vmIdList = jsonSerializer.deserialize[ListVirtualMachinesResponse](
      getEntityJsonById(vmId, "id", "listVirtualMachines")
    ).vmResponse.virtualMashines.map(_.id)

    vmIdList.flatMap { vmId =>
      val listTagResponse = getJsonTags("UserVM", vmId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }
  }

  def setTagToResourse(tag: Tag, resourseId: UUID, resourseType: String): Unit = {
    val request = new ApacheCloudStackRequest("createTags")
    request.addParameter("response", "json")
    request.addParameter("resourcetype", resourseType)
    request.addParameter("resourceids", resourseId)
    request.addParameter("tags[0].key", tag.key)
    request.addParameter("tags[0].value", tag.value)
    
    PeriodicRunner.runMethod[String](
      executeRequest(request),
      ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
    )
  }

  private def getEntityJsonById(id: UUID, idEntity: String, command: String): String = {
    val request = new ApacheCloudStackRequest(command)
    request.addParameter("response", "json")
    request.addParameter(idEntity, id)

    PeriodicRunner.runMethod[String](
      executeRequest(request),
      ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
    )
  }

  private def getJsonTags(resourceType: String, resourceId: UUID): String = {
    val tagRequest = new ApacheCloudStackRequest("listTags")
    tagRequest.addParameter("response", "json")
    tagRequest.addParameter("resourcetype", resourceType)
    tagRequest.addParameter("listAll", "true")
    tagRequest.addParameter("resourceid", resourceId)

    PeriodicRunner.runMethod[String](
      executeRequest(tagRequest),
      ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
    )
  }

  private def executeRequest(request: ApacheCloudStackRequest)(): String = {
    val clientList = threadLocalClientList.get()
    Try {
      clientList.head.executeRequest(request)
    } match {
      case Success(x) => x
      case Failure(e) =>
        if (clientList.tail.isEmpty) {
          threadLocalClientList.set(apacheCloudStackClientList)
        } else {
          threadLocalClientList.set(clientList.tail)
        }
        throw e
    }
  }

}
