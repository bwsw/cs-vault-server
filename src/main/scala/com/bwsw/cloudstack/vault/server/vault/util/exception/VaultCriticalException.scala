package com.bwsw.cloudstack.vault.server.vault.util.exception

import com.bwsw.cloudstack.vault.server.util.exception.CriticalException

/**
  * Created by medvedev_vv on 28.08.17.
  */
class VaultCriticalException(override val exception: Throwable) extends CriticalException(exception) {
}
