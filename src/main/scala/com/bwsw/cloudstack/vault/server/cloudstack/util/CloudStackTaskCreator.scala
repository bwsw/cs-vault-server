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
import com.bwsw.cloudstack.vault.server.cloudstack.util.exception.{CloudStackEntityDoesNotExistException, CloudStackFatalException}
import com.bwsw.cloudstack.vault.server.common.WeightedQueue
import com.bwsw.cloudstack.vault.server.util.HttpStatus
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * Class is responsible for creating tasks for interaction with CloudStack server with help of CloudStack library
  *
  * @param settings contains the settings for interaction with CloudStack
  */
class CloudStackTaskCreator(settings: CloudStackTaskCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  protected val apacheCloudStackUser = new ApacheCloudStackUser(settings.secretKey, settings.apiKey)
  protected val endpointQueue = new WeightedQueue[String](settings.endpoints.toList)

  val idParameter = "id"
  val nameParameter = "name"
  val domainParameter = "domainid"

  /**
    * Creates task for getting tags fro specified entity
    *
    * @param resourceType type of tags for getting
    * @param resourceId id of resource which tags will be got
    *
    * @return task for getting tags
    */
  def createGetTagTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
    val tagRequest = new ApacheCloudStackRequest(Command.toString(Command.ListTags))
    tagRequest.addParameter("response", "json")
    tagRequest.addParameter("resourcetype", Tag.Type.toString(resourceType))
    tagRequest.addParameter("listAll", "true")
    tagRequest.addParameter("resourceid", resourceId)

    createRequest(tagRequest, s"get tag by resource: ($resourceId, $resourceType)")
  }

  /**
    * Creates task for getting entity with specified parameters
    *
    * @param parameters Map where key -> filter parameter for getting entity
    *        value -> value of filter parameter for getting entity
    * @return task for getting entity
    */
  def createGetEntityTask(command: Command, parameters: Map[String, String]):() => String = {
    val request = new ApacheCloudStackRequest(Command.toString(command))
    request.addParameter("response", "json")
    request.addParameter("listAll", "true")
    parameters.foreach {
      case (parameterName, value) => request.addParameter(parameterName, value)
    }

    createRequest(request, s"get entity by command: $command")
  }

  /**
    * Creates task for setting tag to tags of specified entity
    *
    * @param resourceId id of resource for setting tag
    * @param resourceType type of resources tags
    * @param tagSet Set with tags for setting in resource tags
    *
    * @return task for setting tag to entity
    */
  def createSetResourceTagsTask(resourceId: UUID, resourceType: Tag.Type, tagSet: Set[Tag]):() => Unit = {
    @tailrec
    def addTagsToRequest(index: Int, tags: Set[Tag], request: ApacheCloudStackRequest): ApacheCloudStackRequest = {
      if (tags.nonEmpty) {
        request.addParameter(s"tags[$index].key", Tag.Key.toString(tags.head.key))
        request.addParameter(s"tags[$index].value", tags.head.value)
        addTagsToRequest(index + 1, tags.tail, request)
      } else {
        request
      }
    }

    val request = new ApacheCloudStackRequest(Command.toString(Command.CreateTags))
    request.addParameter("response", "json")
    request.addParameter("resourcetype", Tag.Type.toString(resourceType))
    request.addParameter("resourceids", resourceId)

    val requestWithTags = addTagsToRequest(0, tagSet, request)

    createRequest(requestWithTags, s"set tags to resource: ($resourceId, $resourceType)")
  }

  /**
    * Handles request execution
    * does not swallowed ApacheCloudStackClientRuntimeException if it wrapping NoRouteToHostException
    * @throws CloudStackEntityDoesNotExistException if ApacheCloudStackClientRequestRuntimeException which have
    *                                               StatusCode == 431 was thrown
    *
    */
  protected def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String = {
    logger.debug(s"Request was executed for action: $requestDescription")
    val currentEndpoint = endpointQueue.getElement
    val client = createClient(currentEndpoint)
    Try {
      client.executeRequest(request)
    } match {
      case Success(x) => x
      case Failure(e: ApacheCloudStackClientRuntimeException) if e.getCause.isInstanceOf[NoRouteToHostException] =>
        logger.warn(s"CloudStack server is unavailable by endpoint: $currentEndpoint")
        endpointQueue.moveElementToEnd(currentEndpoint)
        throw e
      case Failure(e: ApacheCloudStackClientRequestRuntimeException)
        if e.getStatusCode == HttpStatus.CLOUD_STACK_ENTITY_DOES_NOT_EXIST =>
        throw new CloudStackEntityDoesNotExistException(e.toString)
      case Failure(e: Throwable) =>
        logger.error(s"Request execution thrown an exception: $e")
        throw new CloudStackFatalException(e.toString)
    }
  }

  protected def createClient(endpoint: String): ApacheCloudStackClient = {
    new ApacheCloudStackClient(endpoint, apacheCloudStackUser)
  }
}

object CloudStackTaskCreator {
  case class Settings(endpoints: Array[String], secretKey: String, apiKey: String)
}
