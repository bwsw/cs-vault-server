/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.bwsw.cloudstack.vault.server.cloudstack.entities

import com.bwsw.cloudstack.vault.server.util.{ApplicationConfig, ConfigLiterals}

sealed trait VaultTagKey extends Product with Serializable {

  def oneOf(xs: VaultTagKey*): Boolean = xs.contains(this)

  def oneOf(xs: Set[VaultTagKey]): Boolean = xs.contains(this)

}

object VaultTagKey {
  val prefix: String = ApplicationConfig.getRequiredString(ConfigLiterals.tagNamePrefix) match {
    case "" => ""
    case x => s"$x."
  }
  private val lowerCaseVaultRoString     = s"${prefix}vault.ro"
  private val lowerCaseVaultRwString     = s"${prefix}vault.rw"
  private val lowerCaseVaultHostString   = s"${prefix}vault.host"
  private val lowerCaseVaultPrefixString = s"${prefix}vault.prefix"
  private val upperCaseVaultRoString     = lowerCaseVaultRoString.toUpperCase
  private val upperCaseVaultRwString     = lowerCaseVaultRwString.toUpperCase
  private val upperCaseVaultHostString   = lowerCaseVaultHostString.toUpperCase
  private val upperCaseVaultPrefixString = lowerCaseVaultPrefixString.toUpperCase

  case object VaultRO      extends VaultTagKey
  case object VaultRW      extends VaultTagKey
  case object VaultHost    extends VaultTagKey
  case object VaultPrefix  extends VaultTagKey
  case object Other        extends VaultTagKey

  def fromString(key: String): VaultTagKey = {
    key.toUpperCase match {
      case `upperCaseVaultRoString`       => VaultTagKey.VaultRO
      case `upperCaseVaultRwString`       => VaultTagKey.VaultRW
      case `upperCaseVaultHostString`     => VaultTagKey.VaultHost
      case `upperCaseVaultPrefixString`   => VaultTagKey.VaultPrefix
      case _                              => VaultTagKey.Other
    }
  }

  def toString(x: VaultTagKey): String = x match {
    case  VaultTagKey.VaultRO       => lowerCaseVaultRoString
    case  VaultTagKey.VaultRW       => lowerCaseVaultRwString
    case  VaultTagKey.VaultPrefix   => lowerCaseVaultPrefixString
    case  VaultTagKey.VaultHost     => lowerCaseVaultHostString
    case  VaultTagKey.Other         => ""
  }
}
