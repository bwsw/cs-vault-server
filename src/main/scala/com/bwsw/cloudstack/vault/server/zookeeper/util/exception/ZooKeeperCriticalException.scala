package com.bwsw.cloudstack.vault.server.zookeeper.util.exception

import com.bwsw.cloudstack.vault.server.util.exception.CriticalException

/**
  * Created by medvedev_vv on 29.08.17.
  */
class ZooKeeperCriticalException(override val exception: Throwable) extends CriticalException(exception) {
}
