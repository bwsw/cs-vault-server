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
  * Creates tasks for interaction with CloudStack
  *
  * @param settings contains settings for interaction with CloudStack
  */
class CloudStackTaskCreator(settings: CloudStackTaskCreator.Settings) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  protected val apacheCloudStackUser = new ApacheCloudStackUser(settings.secretKey, settings.apiKey)
  protected val endpointQueue = new WeightedQueue[String](settings.endpoints.toList)

  val idParameter = "id"
  val nameParameter = "name"
  val domainParameter = "domainid"

  /**
    * Creates task to retrieve tags for specified entity
    *
    * @param resourceType type of tags to extract
    * @param resourceId id of resource which tags are retrieved
    *
    * @return task to retrieve tags
    */
  def createGetTagsTask(resourceType: Tag.Type, resourceId: UUID): () => String = {
    val tagRequest = new ApacheCloudStackRequest(Command.toString(Command.ListTags))
    tagRequest.addParameter("response", "json")
    tagRequest.addParameter("resourcetype", Tag.Type.toString(resourceType))
    tagRequest.addParameter("listAll", "true")
    tagRequest.addParameter("resourceid", resourceId)

    createRequest(tagRequest, s"Retrieve tags by resource: ($resourceId, $resourceType)")
  }

  /**
    * Creates task to retrieve entity with specified parameters
    *
    * @param parameters Map where key -> filter parameter to retrieve entity
    *        value -> value of filter parameter to retrieve entity
    * @return task to retrieve entity
    */
  def createGetEntityTask(command: Command, parameters: Map[String, String]):() => String = {
    val request = new ApacheCloudStackRequest(Command.toString(command))
    request.addParameter("response", "json")
    request.addParameter("listAll", "true")
    parameters.foreach {
      case (parameterName, value) => request.addParameter(parameterName, value)
    }

    createRequest(request, s"Retrieve entity by command: $command with parameters: $parameters")
  }

  /**
    * Creates task to include tags into tags of specified entity
    *
    * @param resourceId id of resource to include tag
    * @param resourceType type of resources’ tags
    * @param tagSet Set of tags to include into resource
    *
    * @return task to include tags into entity tags
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

    createRequest(requestWithTags, s"Include tags into resource: ($resourceId, $resourceType)")
  }

  /**
    * Handles request
    * does not swallow ApacheCloudStackClientRuntimeException if it wraps NoRouteToHostException
    * @throws CloudStackEntityDoesNotExistException if ApacheCloudStackClientRequestRuntimeException with
    *                                               431 status code occurred
    *
    *
    */
  protected def createRequest(request: ApacheCloudStackRequest, requestDescription: String)(): String = {
    logger.debug(s"Request executed for action: $requestDescription")
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
        logger.error(s"Request execution threw an exception: $e")
        throw new CloudStackEntityDoesNotExistException(e.toString)
      case Failure(e: Throwable) =>
        logger.error(s"Request execution threw an exception: $e")
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
