package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import br.com.autonomiccs.apacheCloudStack.exceptions.ApacheCloudStackClientRuntimeException
import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskWrapper
import com.bwsw.cloudstack.vault.server.common.JsonSerializer
import com.bwsw.cloudstack.vault.server.util.exception.CloudStackNoSuchEntityException
import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals, TaskRunner}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackService {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val apacheCloudStackTaskWrapper= new ApacheCloudStackTaskWrapper
  private val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
  private val id = "id"
  private val name = "name"

  def getUserTagByAccountId(accountId: UUID): List[Tag] = {
    logger.debug(s"getUserTagByAccountId(accountId: $accountId)")
    val jsonSerializer = new JsonSerializer(true)

    val allUsersIdInAccount = getUserIdListForAccount(accountId)

    val r = allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getTagsJson(Tag.Type.User, userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for account: $accountId)")
    r
  }

  def getUserTagByUserId(userId: UUID): List[Tag] = {
    logger.debug(s"getUserTagByUserId(userId: $userId)")
    val jsonSerializer = new JsonSerializer(true)

    val userList = jsonSerializer.deserialize[ListUserResponse](
      getEntityJson(userId.toString, id, Command.ListUsers)
    ).userResponse.users

    val allUsersIdInAccount: List[UUID] = userList.flatMap { x =>
      getUserIdListForAccount(x.accountId)
    }

    val r = allUsersIdInAccount.flatMap { userId =>
      val listTagResponse = getTagsJson(Tag.Type.User, userId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for user: $userId)")
    r
  }

  def getVmTagById(vmId: UUID): List[Tag] = {
    logger.debug(s"getVmTagById(vmId: $vmId)")
    val jsonSerializer = new JsonSerializer(true)

    val vmIdList = jsonSerializer.deserialize[ListVirtualMachinesResponse](
      getEntityJson(vmId.toString, id, Command.ListVirtualMachines)
    ).vmResponse.virtualMashines.map(_.id)

    val r = vmIdList.flatMap { vmId =>
      val listTagResponse = getTagsJson(Tag.Type.UserVM, vmId)
      jsonSerializer.deserialize[ListTagResponse](listTagResponse).tagResponse.tags
    }

    logger.debug(s"Tag was got for vm: $vmId)")
    r
  }

  def setTagToResourse(tag: Tag, resourseId: UUID, resourseType: Tag.Type): Unit = {
    logger.debug(s"setTagToResourse(resourseId: $resourseId, resourseType: $resourseType)")
    def task = apacheCloudStackTaskWrapper.setTagToResourseTask(tag, resourseId, resourseType)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
    logger.debug(s"Tag was set to resourse: $resourseId, $resourseType")
  }

  def getAccountIdForVm(vmId: UUID): UUID = {
    logger.debug(s"getAccountIdForVm(vmId: $vmId)")
    val jsonSerializer = new JsonSerializer(true)

    val accountName = jsonSerializer.deserialize[ListVirtualMachinesResponse](
      getEntityJson(vmId.toString, id, Command.ListVirtualMachines)
    ).vmResponse.virtualMashines.map(_.accountName)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountName for VM with id: $vmId"))

    val accountId: UUID = jsonSerializer.deserialize[ListAccountResponse](
      getEntityJson(accountName, name, Command.ListAccounts)
    ).accountResponse.accounts.map(_.id)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find account by name: $accountName"))

    logger.debug(s"accountId was got for vm: $vmId)")
    accountId
  }

  def getAccountIdForUser(userId: UUID): UUID = {
    logger.debug(s"getAccountIdForUser(userId: $userId)")
    val jsonSerializer = new JsonSerializer(true)

    val accountId = jsonSerializer.deserialize[ListUserResponse](
      getEntityJson(userId.toString, id, Command.ListUsers)
    ).userResponse.users.map(_.accountId)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountId for user: $userId"))

    logger.debug(s"accountId was got for user: $userId)")
    accountId
  }

  def getUserIdListForAccount(accountId: UUID): List[UUID] = {
    logger.debug(s"getUserIdsForAccount(accountId: $accountId)")
    val jsonSerializer = new JsonSerializer(true)

    val listAccountResponse = getEntityJson(accountId.toString, id, Command.ListAccounts)
    val allUsersIdInAccount = jsonSerializer.deserialize[ListAccountResponse](listAccountResponse)
      .accountResponse
      .accounts
      .flatMap { x =>
        x.users.map(_.id)
      }

    logger.debug(s"Users were got for account: $accountId)")
    allUsersIdInAccount
  }

  private def getEntityJson(parameterValue: String, parameterName: String, command: Command): String = {
    logger.debug(s"getEntityJson(parameterValue: $parameterValue, parameterName: $parameterName, command: $command)")
    def task = apacheCloudStackTaskWrapper.getEntityTask(parameterValue, parameterName, command)

    Try {
      task()
    } match {
      case Success(x) => x
      case Failure(e: ApacheCloudStackClientRuntimeException) =>
        logger.warn(s"CloudStack server is unavailable")
        Thread.sleep(cloudStackRetryDelay)
        TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
      case Failure(e: Throwable) =>
        logger.error(s"The command: $command has not executed, exception was thrown: $e")
        throw new CloudStackNoSuchEntityException(s"The entity with " +
          s"parameterName: $parameterName, " +
          s"parameterValue: $parameterValue " +
          s"could not been got")
    }
  }

  private def getTagsJson(resourceType: Tag.Type, resourceId: UUID): String = {
    def task = apacheCloudStackTaskWrapper.getTagTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

}
