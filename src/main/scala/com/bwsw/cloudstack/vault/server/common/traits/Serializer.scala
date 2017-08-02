package com.bwsw.cloudstack.vault.server.common.traits

trait Serializer {
  def serialize(value: Any): String
  def deserialize[T: Manifest](value: String) : T
  def setIgnoreUnknown(ignore: Boolean)
  def getIgnoreUnknown() : Boolean
}

