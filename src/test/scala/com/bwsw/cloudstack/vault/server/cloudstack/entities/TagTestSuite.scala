package com.bwsw.cloudstack.vault.server.cloudstack.entities

import com.bwsw.cloudstack.vault.server.BaseTestSuite
import org.scalatest.FlatSpec

/**
  * Created by medvedev_vv on 03.10.17.
  */
class TagTestSuite extends FlatSpec with BaseTestSuite {
  "Tag.Key.fromString" should "return Tag.Key.VaultRO" in {
    val testString = "VAULT.RO"
    assert(Tag.Key.fromString(s"${Tag.prefix}$testString") == Tag.Key.VaultRO, s"${Tag.prefix}$testString")
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultRW" in {
    val testString = "VAULT.RW"
    assert(Tag.Key.fromString(s"${Tag.prefix}$testString") == Tag.Key.VaultRW)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultHost" in {
    val testString = "VAULT.HOST"
    assert(Tag.Key.fromString(s"${Tag.prefix}$testString") == Tag.Key.VaultHost)
  }

  "Tag.Key.fromString" should "return Tag.Key.VaultPrefix" in {
    val testString = "VAULT.PREFIX"
    assert(Tag.Key.fromString(s"${Tag.prefix}$testString") == Tag.Key.VaultPrefix)
  }

  "Tag.Key.toString" should "return vault.ro" in {
    val testString = "vault.ro"
    assert(Tag.Key.toString(Tag.Key.VaultRO) == s"${Tag.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.rw" in {
    val testString = "vault.rw"
    assert(Tag.Key.toString(Tag.Key.VaultRW) == s"${Tag.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.host" in {
    val testString = "vault.host"
    assert(Tag.Key.toString(Tag.Key.VaultHost) == s"${Tag.prefix}$testString")
  }

  "Tag.Key.toString" should "return vault.prefix" in {
    val testString = "vault.prefix"
    assert(Tag.Key.toString(Tag.Key.VaultPrefix) == s"${Tag.prefix}$testString")
  }
}
