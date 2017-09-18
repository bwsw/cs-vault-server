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
package com.bwsw.cloudstack.vault.server.cloudstack.util

import java.net.NoRouteToHostException
import java.util.UUID

import br.com.autonomiccs.apacheCloudStack.client.{ApacheCloudStackClient, ApacheCloudStackRequest}
import br.com.autonomiccs.apacheCloudStack.client.beans.ApacheCloudStackUser
import br.com.autonomiccs.apacheCloudStack.exceptions.{ApacheCloudStackClientRequestRuntimeException, ApacheCloudStackClientRuntimeException}
import com.bwsw.cloudstack.vault.server.cloudstack.entities.{Command, Tag}
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackCriticalException, CloudStackEntityDoesNotExistException}
import com.bwsw.cloudstack.vault.server.util.HttpStatuses
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

class CloudStackTaskCreator(settings: CloudStackTaskCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  //cloud stack client config
  protected val apacheCloudStackUser = new ApacheCloudStackUser(settings.secretKey, settings.apiKey)
  protected val apacheCloudStackClientList: List[ApacheCloudStackClient] = settings.urlList.map { x =>
    new ApacheCloudStackClient(x, apacheCloudStackUser)
  }.toList

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

  def createSetResourceTagTask(resourceId: UUID, resourceType: Tag.Type, tagList: List[Tag]):() => Unit = {
    val request = new ApacheCloudStackRequest(Command.toString(Command.CreateTags))
    request.addParameter("response", "json")
    request.addParameter("resourcetype", Tag.Type.toString(resourceType))
    request.addParameter("resourceids", resourceId)

    loop(0, tagList)

    def loop(index: Int, tagList: List[Tag]): Unit = {
      if (tagList.nonEmpty) {
        val tag = tagList.head
        request.addParameter(s"tags[$index].key", Tag.Key.toString(tag.key))
        request.addParameter(s"tags[$index].value", tag.value)
        loop(index + 1, tagList.tail)
      }
    }

    createRequest(request, s"set tags to resource: ($resourceId, $resourceType)")
  }

  protected def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String = {
    logger.debug(s"Request was executed for action: $requestDescription")
    val clientList = threadLocalClientList.get()
    Try {
      clientList.head.executeRequest(request)
    } match {
      case Success(x) => x
      case Failure(e: ApacheCloudStackClientRuntimeException) if e.getCause.isInstanceOf[NoRouteToHostException] =>
        logger.warn(s"CloudStack server is unavailable")
        if (clientList.tail.isEmpty) {
          threadLocalClientList.set(apacheCloudStackClientList)
        } else {
          threadLocalClientList.set(clientList.tail)
        }
        throw e
      case Failure(e: ApacheCloudStackClientRequestRuntimeException)
        if e.getStatusCode == HttpStatuses.CLOUD_STACK_ENTITY_DOES_NOT_EXIST =>
        throw new CloudStackCriticalException(new CloudStackEntityDoesNotExistException(e.toString))
      case Failure(e :Throwable) =>
        logger.error(s"Request execution thrown an critical exception: $e")
        throw new CloudStackCriticalException(e)
    }
  }
}

object CloudStackTaskCreator {
  case class Settings(urlList: Array[String], secretKey: String, apiKey: String)
}
