package com.bwsw.cloudstack.vault.server.cloudstack.entities

/**
  * Created by medvedev_vv on 23.08.17.
  */
sealed trait Command extends Product with Serializable

object Command {
  case object ListVirtualMachines     extends Command
  case object ListUsers               extends Command
  case object ListAccounts            extends Command
  case object ListTags                extends Command
  case object CreateTags              extends Command

  def fromString: PartialFunction[String, Command] = {
    case "listVirtualMachines"           => Command.ListVirtualMachines
    case "listUsers"                     => Command.ListUsers
    case "listAccounts"                  => Command.ListAccounts
    case "listTags"                      => Command.ListTags
    case "createTags"                    => Command.CreateTags
  }

  def toString(x: Command): String = x match {
    case Command.ListVirtualMachines     => "listVirtualMachines"
    case Command.ListUsers               => "listUsers"
    case Command.ListAccounts            => "listAccounts"
    case Command.ListTags                => "listTags"
    case Command.CreateTags              => "createTags"
  }
}
