package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import br.com.autonomiccs.apacheCloudStack.exceptions.ApacheCloudStackClientRuntimeException
import com.bwsw.cloudstack.vault.server.cloudstack.entities._
import com.bwsw.cloudstack.vault.server.cloudstack.util.ApacheCloudStackTaskCreator
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
  private val apacheCloudStackTaskCreator = new ApacheCloudStackTaskCreator
  private val cloudStackRetryDelay = ApplicationConfig.getRequiredInt(ConfigLiterals.cloudStackRetryDelay)
  private val threadLocalJsonSerializer: ThreadLocal[JsonSerializer] = new ThreadLocal
  threadLocalJsonSerializer.get.setIgnoreUnknown(true)
  private val id = "id"
  private val name = "name"

  def getUserTagsByAccountId(accountId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByAccountId(accountId: $accountId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val allUsersIdInAccount = getUserIdsForAccount(accountId)

    val tags = allUsersIdInAccount.flatMap { userId =>
      val tagResponse = getTagsJson(Tag.Type.User, userId)
      jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags
    }

    logger.debug(s"Tags were got for account: $accountId)")
    tags
  }

  def getUserTagsByUserId(userId: UUID): List[Tag] = {
    logger.debug(s"getUserTagsByUserId(userId: $userId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val users = jsonSerializer.deserialize[UserResponse](
      getEntityJson(userId.toString, id, Command.ListUsers)
    ).userList.users

    val allUsersIdInAccount: List[UUID] = users.flatMap { x =>
      getUserIdsForAccount(x.accountId)
    }

    val tags = allUsersIdInAccount.flatMap { userId =>
      val tagResponse = getTagsJson(Tag.Type.User, userId)
      jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags
    }

    logger.debug(s"Tags were got for user: $userId)")
    tags
  }

  def getVmTagsById(vmId: UUID): List[Tag] = {
    logger.debug(s"getVmTagsById(vmId: $vmId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val vmIds = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(vmId.toString, id, Command.ListVirtualMachines)
    ).virtualMashineList.virtualMashines.map(_.id)

    val tags = vmIds.flatMap { vmId =>
      val tagResponse = getTagsJson(Tag.Type.UserVM, vmId)
      jsonSerializer.deserialize[TagResponse](tagResponse).tagList.tags
    }

    logger.debug(s"Tags were got for vm: $vmId)")
    tags
  }

  def setResourseTag(resourseId: UUID, resourseType: Tag.Type, tag: Tag): Unit = {
    logger.debug(s"setTagToResourse(resourseId: $resourseId, resourseType: $resourseType)")
    def task = apacheCloudStackTaskCreator.createSetResourseTagTask(resourseId, resourseType, tag)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
    logger.debug(s"Tag was set to resourse: $resourseId, $resourseType")
  }

  def getAccountIdByVmId(vmId: UUID): UUID = {
    logger.debug(s"getAccountIdForVm(vmId: $vmId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val accountName = jsonSerializer.deserialize[VirtualMachinesResponse](
      getEntityJson(vmId.toString, id, Command.ListVirtualMachines)
    ).virtualMashineList.virtualMashines.map(_.accountName)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountName for VM with id: $vmId"))

    val accountId: UUID = jsonSerializer.deserialize[AccountResponse](
      getEntityJson(accountName, name, Command.ListAccounts)
    ).accountList.accounts.map(_.id)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find account by name: $accountName"))

    logger.debug(s"accountId was got for vm: $vmId)")
    accountId
  }

  def getAccountIdByUserId(userId: UUID): UUID = {
    logger.debug(s"getAccountIdForUser(userId: $userId)")
    val jsonSerializer = threadLocalJsonSerializer.get()

    val accountId = jsonSerializer.deserialize[UserResponse](
      getEntityJson(userId.toString, id, Command.ListUsers)
    ).userList.users.map(_.accountId)
      .headOption
      .getOrElse(throw new NoSuchElementException(s"Can not find accountId for user: $userId"))

    logger.debug(s"accountId was got for user: $userId)")
    accountId
  }

  def getUserIdsForAccount(accountId: UUID): List[UUID] = {
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
    def task = apacheCloudStackTaskCreator.createGetEntityTask(parameterValue, parameterName, command)

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
    def task = apacheCloudStackTaskCreator.createGetTagTask(resourceType, resourceId)

    TaskRunner.tryRunUntilSuccess[String](task, cloudStackRetryDelay)
  }

}
