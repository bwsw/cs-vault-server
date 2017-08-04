package com.bwsw.cloudstack.vault.server.util

/**
  * Created by medvedev_vv on 03.08.17.
  */
object PeriodicChecker {

  def runCheck(checkFunction: () => Boolean,
               timeForFirstCheck: Int = 1000,
               timeForLastCheck: Int = 10000,
               retryCount: Int = 10): Boolean = {
    val incrementTime: Int = (timeForLastCheck - timeForFirstCheck) / (retryCount - 1)
    loop(checkFunction, timeForFirstCheck, timeForLastCheck, incrementTime, retryCount - 1)
  }

  private def loop(checkFunc: () => Boolean,
                   waitingTime: Int,
                   timeForLastCheck: Int,
                   incrementTime: Int,
                   retryCount: Int): Boolean = {
    if (checkFunc()) {
      true
    } else {
      Thread.sleep(waitingTime)
      if (retryCount == -1) {
        if (waitingTime < timeForLastCheck) {
          loop(checkFunc, waitingTime + incrementTime, timeForLastCheck, incrementTime, retryCount)
        } else {
          loop(checkFunc, waitingTime, timeForLastCheck, incrementTime, retryCount)
        }
      } else if (retryCount > 0) {
        loop(checkFunc, waitingTime + incrementTime, timeForLastCheck, incrementTime, retryCount - 1)
      } else {
        false
      }
    }
  }
}
