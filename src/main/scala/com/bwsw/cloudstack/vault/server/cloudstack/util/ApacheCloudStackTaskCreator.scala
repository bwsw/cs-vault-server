package com.bwsw.cloudstack.vault.server.cloudstack.util

import java.util.UUID

import br.com.autonomiccs.apacheCloudStack.client.{ApacheCloudStackClient, ApacheCloudStackRequest}
import br.com.autonomiccs.apacheCloudStack.client.beans.ApacheCloudStackUser
import br.com.autonomiccs.apacheCloudStack.exceptions.ApacheCloudStackClientRuntimeException
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.CloudStackCriticalException
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 21.08.17.
  */
class ApacheCloudStackTaskCreator(settings: ApacheCloudStackTaskCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  //cloud stack client config
  protected val apacheCloudStackUser = new ApacheCloudStackUser(settings.secretKey, settings.apiKey)
  protected val apacheCloudStackClientList: List[ApacheCloudStackClient] = settings.urlList.map { x =>
    new ApacheCloudStackClient(x, apacheCloudStackUser)
  }.toList
  apacheCloudStackClientList.foreach(_.setValidateServerHttpsCertificate(false))

  private var threadLocalClientList = new ThreadLocal[List[ApacheCloudStackClient]](){
    override protected def initialValue(): List[ApacheCloudStackClient] = {
      apacheCloudStackClientList
    }
  }
  val idParameter = "id"
  val nameParameter = "name"

  def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
    val tagRequest = new ApacheCloudStackRequest(Command.toString(Command.ListTags))
    tagRequest.addParameter("response", "json")
    tagRequest.addParameter("resourcetype", Tag.Type.toString(resourceType))
    tagRequest.addParameter("listAll", "true")
    tagRequest.addParameter("resourceid", resourceId)

    createRequest(tagRequest, s"get tag by resource: ($resourceId, $resourceType)")
  }

  def createGetEntityTask(parameterValue: String, parameterName: String, command: Command): () => String = {
    val request = new ApacheCloudStackRequest(Command.toString(command))
    request.addParameter("response", "json")
    request.addParameter("listAll", "true")
    request.addParameter(parameterName, parameterValue)

    createRequest(request, s"get entity by command: $command")
  }

  def createSetResourceTagTask(resourceId: UUID, resourceType: Tag.Type, tagList: List[Tag]): () => String = {
    val request = new ApacheCloudStackRequest(Command.toString(Command.CreateTags))
    request.addParameter("response", "json")
    request.addParameter("resourcetype", Tag.Type.toString(resourceType))
    request.addParameter("resourceids", resourceId)

    loop(0, tagList)

    def loop(index: Int, tagList: List[Tag]): Unit = {
      if (tagList.nonEmpty) {
        val tag = tagList.head
        request.addParameter(s"tags[$index].key", tag.key)
        request.addParameter(s"tags[$index].value", tag.value)
        loop(index + 1, tagList.tail)
      }
    }

    createRequest(request, s"set tags to resource: ($resourceId, $resourceType)")
  }

  private def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String = {
    logger.debug(s"Request was executed for action: $requestDescription")
    val clientList = threadLocalClientList.get()
    Try {
      clientList.head.executeRequest(request)
    } match {
      case Success(x) => x
      case Failure(e: ApacheCloudStackClientRuntimeException) =>
        logger.warn(s"CloudStack server is unavailable")
        if (clientList.tail.isEmpty) {
          threadLocalClientList.set(apacheCloudStackClientList)
        } else {
          threadLocalClientList.set(clientList.tail)
        }
        throw e
      case Failure(e :Throwable) =>
        logger.error(s"Request execution thrown an critical exception: $e")
        throw new CloudStackCriticalException(e)
    }
  }
}

object ApacheCloudStackTaskCreator {
  case class Settings(urlList: Array[String], secretKey: String, apiKey: String)
}
