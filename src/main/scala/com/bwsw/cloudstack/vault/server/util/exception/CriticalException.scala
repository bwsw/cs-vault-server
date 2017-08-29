package com.bwsw.cloudstack.vault.server.util.exception

/**
  * Created by medvedev_vv on 28.08.17.
  */
class CriticalException(private val message: String = "") extends Exception(message) {

}
