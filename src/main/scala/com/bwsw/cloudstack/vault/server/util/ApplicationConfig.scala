package com.bwsw.cloudstack.vault.server.util

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

import scala.util.Try

/**
  * Created by medvedev_vv on 28.07.17.
  */
object ApplicationConfig extends StrictLogging {

  /**
    * Application config object.
    */
  private val config = ConfigFactory.load()

  /**
    * Gets the required string from the config file or throws
    * an exception if the string is not found.
    *
    * @param path path to string
    *
    * @return string fetched by path
    */
  def getRequiredString(path: String): String = getRequiredString(config, path)

  private def getRequiredString(config: Config, path: String): String = {
    Try(config.getString(path)).getOrElse {
      handleError(path)
    }
  }

  /**
    * Gets the required int from the config file or throws
    * an exception if the int is not found.
    *
    * @param path path to int
    *
    * @return int fetched by path
    */
  def getRequiredInt(path: String): Int = getRequiredInt(config, path)

  private def getRequiredInt(config: Config, path: String): Int = {
    Try(config.getInt(path)).getOrElse {
      handleError(path)
    }
  }

  /**
    * Gets the required string list from the config file or throws
    * an exception if the string list is not found.
    *
    * @param path path to string list
    *
    * @return string list fetched by path
    */
  def getRequiredStringList(path: String): List[String] = getRequiredStringList(config, path)

  private def getRequiredStringList(config: Config, path: String): List[String] = {
    import scala.collection.JavaConverters._

    Try(config.getStringList(path).asScala.toList).getOrElse {
      handleError(path)
    }
  }

  private def handleError(path: String) = {
    val errMsg = s"Missing required configuration entry: $path"
    logger.error(errMsg)
    throw new ConfigException.Missing(errMsg)
  }

}
