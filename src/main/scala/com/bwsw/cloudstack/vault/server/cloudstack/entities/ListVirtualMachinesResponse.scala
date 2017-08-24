package com.bwsw.cloudstack.vault.server.cloudstack.entities

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Created by medvedev_vv on 11.08.17.
  */
private[cloudstack] case class ListVirtualMachinesResponse(@JsonProperty("listvirtualmachinesresponse") vmResponse: VirtualMashineResponse)

private[cloudstack] case class VirtualMashineResponse(count: Int, @JsonProperty("virtualmachine") virtualMashines: List[VirtualMashine])

private[cloudstack] case class VirtualMashine(id: UUID, @JsonProperty("account") accountName: String)
