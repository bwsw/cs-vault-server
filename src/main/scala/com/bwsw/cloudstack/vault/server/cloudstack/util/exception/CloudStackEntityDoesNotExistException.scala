package com.bwsw.cloudstack.vault.server.cloudstack.util.exception

/**
  * Created by medvedev_vv on 12.09.17.
  */
class CloudStackEntityDoesNotExistException(private val message: String) extends Exception(message) {
}
