package com.bwsw.cloudstack.vault.server.controllers

import com.bwsw.cloudstack.vault.server.cloudstack.CloudStackService
import com.bwsw.cloudstack.vault.server.vault.VaultService
import com.bwsw.cloudstack.vault.server.vault.entities.Policy
import com.bwsw.cloudstack.vault.server.zookeeper.ZooKeeperService

import scala.concurrent.ExecutionContext

/**
  * Created by medvedev_vv on 02.08.17.
  */
class CloudStackVaultController(vaultService: VaultService,
                                cloudStackService: CloudStackService,
                                zooKeeperService: ZooKeeperService)
                               (implicit executionContext: ExecutionContext) {
  def handleUserCreate(): Unit = {

  }
  def handleAccountCreate(){}

  def handleVmCreate(): Unit = {
    //val readPolicy = Policy.
    //vaultService.createToken()
  }
  def handleAccountDelete(){}
  def handleVmDelete(){}
}
