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
package com.bwsw.cloudstack.vault.server.util

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

import scala.util.Try

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
