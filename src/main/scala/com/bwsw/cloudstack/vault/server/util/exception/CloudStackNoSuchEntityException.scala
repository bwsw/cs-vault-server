package com.bwsw.cloudstack.vault.server.util.exception

/**
  * Created by medvedev_vv on 24.08.17.
  */
class CloudStackNoSuchEntityException(private val message: String = "") extends NoSuchElementException(message) {
}
