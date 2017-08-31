package com.bwsw.cloudstack.vault.server.cloudstack

import java.util.UUID

import br.com.autonomiccs.apacheCloudStack.client.ApacheCloudStackRequest
import com.bwsw.cloudstack.vault.server.cloudstack.entities.Tag

/**
  * Created by medvedev_vv on 25.08.17.
  */
trait TestData {
  val userId: UUID = UUID.randomUUID()
  val accountId: UUID = UUID.randomUUID()
  val vmId: UUID = UUID.randomUUID()


  val listUsersCommand = "listUsers"
  val listVirtualMachines = "listVirtualMachines"

  val vmUserResourseType = "UserVM"
  val idParameter = "id"
  val nameParameter = "name"

  object Response {
    def getTagResponseJson(key: Tag.Key, value: String): String = "{\"listtagsresponse\":{\"count\":1,\"tag\":[{\"key\":\"" + s"${Tag.Key.toString(key)}" + "\",\"value\":\"" + s"$value" + "\"}]}}"
    def getAccountResponseJson(account: String, user: String): String = "{\"listaccountsresponse\":{\"count\":1,\"account\":[{\"id\":\"" + s"$account" + "\",\"user\":[{\"id\":\"" + s"$user" + "\",\"accountid\":\"" + s"$account" + "\"}]}]}}"
    def getUserResponseJson(user: String, account: String): String = "{\"listusersresponse\":{\"count\":1,\"user\":[{\"id\":\"" + s"$user" + "\", \"accountid\":\" " + s"$account" + "\"}]}}"
    def getVmResponseJson(vm: String, accountName: String): String = "{\"listvirtualmachinesresponse\":{\"virtualmachine\":[{\"id\":\"" + s"$vm" + "\",\"account\":\"" + s"$accountName" + "\"}]}}"

  }

  object Request {
    def getUserTagsRequest(userId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listTags")
      .addParameter("response", "json")
      .addParameter("resourcetype", "User")
      .addParameter("listAll", "true")
      .addParameter("resourceid", userId)

    def getVmTagsRequest(vmId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listTags")
      .addParameter("response", "json")
      .addParameter("resourcetype", "UserVM")
      .addParameter("listAll", "true")
      .addParameter("resourceid", vmId)

    def getAccountRequest(accountId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listAccounts")
      .addParameter("response", "json")
      .addParameter("listAll", "true")
      .addParameter("id", accountId)

    def getAccountRequestByName(name: String): ApacheCloudStackRequest = new ApacheCloudStackRequest("listAccounts")
      .addParameter("response", "json")
      .addParameter("listAll", "true")
      .addParameter("name", name)

    def getVmRequest(vmId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listVirtualMachines")
      .addParameter("response", "json")
      .addParameter("listAll", "true")
      .addParameter("id", vmId)

    def getUserRequest(userId: UUID): ApacheCloudStackRequest = new ApacheCloudStackRequest("listUser")
      .addParameter("response", "json")
      .addParameter("listAll", "true")
      .addParameter("id", userId)
  }


}
