package com.bwsw.cloudstack.vault.server.common

/**
  * Created by medvedev_vv on 21.08.17.
  */
object Converter {
  def daysToSeconds(dayCount: Int): Int = dayCount*24*60*60
}
