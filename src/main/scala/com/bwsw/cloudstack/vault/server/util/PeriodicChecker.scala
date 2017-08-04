package com.bwsw.cloudstack.vault.server.util

/**
  * Created by medvedev_vv on 03.08.17.
  */
class PeriodicChecker(checkFunction: () => Boolean,
                      timeForFirstCheck: Int = 1000,
                      timeForLastCheck: Int = 10000,
                      retryCount: Int = 10) {

  val incrementTime: Int = (timeForLastCheck - timeForFirstCheck) / (retryCount - 1)

  private def loop(checkFunc: () => Boolean, waitingTime: Int, retryCount: Int): Boolean = {
    if (checkFunc()) {
      true
    } else {
      Thread.sleep(waitingTime)
      if (retryCount == -1) {
        if (waitingTime < timeForLastCheck) {
          loop(checkFunc, waitingTime + incrementTime, retryCount)
        } else {
          loop(checkFunc, waitingTime, retryCount)
        }
      } else if (retryCount > 0) {
        loop(checkFunc, waitingTime + incrementTime, retryCount - 1)
      } else {
        false
      }
    }
  }

  def runCheck: Boolean = {
    loop(checkFunction, timeForFirstCheck, retryCount - 1)
  }
}
