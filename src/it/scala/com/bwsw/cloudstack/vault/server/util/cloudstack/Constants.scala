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
package com.bwsw.cloudstack.vault.server.util.cloudstack

object Constants {
  object RequestParamaterKeys {
    val RESPONSE = "response"
    val AVAILABLE = "available"
    val TEMPLATE_FILTER = "templatefilter"
    val LIST_ALL = "listAll"
  }

  object RequestCommands {
    val LIST_SERVICE_OFFERINGS = "listServiceOfferings"
    val LIST_TEMPLATES = "listTemplates"
    val LIST_ZONES = "listZones"
    val LIST_DOMAINS = "listDomains"
  }

  object RequestParameterValues {
    val JSON = "json"
    val FEATURED = "featured"
  }
}
