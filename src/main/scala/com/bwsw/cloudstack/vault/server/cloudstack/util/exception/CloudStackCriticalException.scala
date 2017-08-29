package com.bwsw.cloudstack.vault.server.cloudstack.util.exception

import com.bwsw.cloudstack.vault.server.util.exception.CriticalException

/**
  * Created by medvedev_vv on 24.08.17.
  */
class CloudStackCriticalException(private val message: String = "") extends CriticalException(message) {
}
