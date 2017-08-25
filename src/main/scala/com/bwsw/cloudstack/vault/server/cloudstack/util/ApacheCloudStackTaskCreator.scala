package com.bwsw.cloudstack.vault.server.cloudstack.util

import java.util.UUID

import br.com.autonomiccs.apacheCloudStack.client.{ApacheCloudStackClient, ApacheCloudStackRequest}
import br.com.autonomiccs.apacheCloudStack.client.beans.ApacheCloudStackUser
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 21.08.17.
  */
class ApacheCloudStackTaskCreator {
  private val logger = LoggerFactory.getLogger(this.getClass)
  //cloud stack client config
  private val secretKey = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackSecretKey)
  private val apiKey = ApplicationConfig.getRequiredString(ConfigLiterals.cloudStackApiKey)
  private val apacheCloudStackUser = new ApacheCloudStackUser(secretKey, apiKey)
  private val cloudStackUrlList: Array[String] = ApplicationConfig
    .getRequiredString(ConfigLiterals.cloudStackApiUrlList)
    .split("[,\\s]+")
  private val apacheCloudStackClientList: List[ApacheCloudStackClient] = cloudStackUrlList.map { x =>
    new ApacheCloudStackClient(x, apacheCloudStackUser)
  }.toList
  apacheCloudStackClientList.foreach(_.setValidateServerHttpsCertificate(false))

  private var threadLocalClientList = new ThreadLocal[List[ApacheCloudStackClient]](){
    override protected def initialValue(): List[ApacheCloudStackClient] = {
      apacheCloudStackClientList
    }
  }

  def createGetTagTask(resourceType: String, resourceId: UUID): () => String = {
    val tagRequest = new ApacheCloudStackRequest("listTags")
    tagRequest.addParameter("response", "json")
    tagRequest.addParameter("resourcetype", resourceType)
    tagRequest.addParameter("listAll", "true")
    tagRequest.addParameter("resourceid", resourceId)

    createRequest(tagRequest, s"get tag by resourse: ($resourceId, $resourceType)")
  }

  def createGetEntityTask(parameterValue: String, parameterName: String, command: String): () => String = {
    val request = new ApacheCloudStackRequest(command)
    request.addParameter("response", "json")
    request.addParameter("listAll", "true")
    request.addParameter(parameterName, parameterValue)

    createRequest(request, s"get entity by command: $command")
  }

  def createSetResourseTagTask(resourseId: UUID, resourseType: String, tag: Tag): () => String = {
    val request = new ApacheCloudStackRequest("createTags")
    request.addParameter("response", "json")
    request.addParameter("resourcetype", resourseType)
    request.addParameter("resourceids", resourseId)
    request.addParameter("tags[0].key", tag.key)
    request.addParameter("tags[0].value", tag.value)

    createRequest(request, s"set tags to resourse: ($resourseId, $resourseType)")
  }

  private def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String = {
    logger.debug(s"Request was executed for action: $requestDescription")
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
