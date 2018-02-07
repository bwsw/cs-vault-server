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

import java.util.UUID

import com.bwsw.cloudstack.entities.requests.domain.DomainFindRequest
import com.bwsw.cloudstack.entities.requests.serviceoffering.ServiceOfferingFindRequest
import com.bwsw.cloudstack.entities.requests.template.TemplateFindRequest
import com.bwsw.cloudstack.entities.requests.template.filters.Featured
import com.bwsw.cloudstack.entities.requests.zone.ZoneFindRequest
import com.bwsw.cloudstack.vault.server.util.cloudstack.components.CloudStackTestsComponents
import com.bwsw.cloudstack.vault.server.util.cloudstack.dao.{DomainDao, ServiceOfferingDao, TemplateDao, ZoneDao}

trait CloudStackTestEntities extends CloudStackTestsComponents {
  lazy val retrievedServiceOfferingId: UUID = {
    val serviceOfferingDao = new ServiceOfferingDao(executor, mapper)
    val serviceOfferingFindRequest = new ServiceOfferingFindRequest
    serviceOfferingDao.find(serviceOfferingFindRequest).head.id
  }

  lazy val retrievedZoneId: UUID = {
    val zoneDao = new ZoneDao(executor, mapper)
    val zoneFindRequest = new ZoneFindRequest
    zoneDao.find(zoneFindRequest).head.id
  }

  lazy val retrievedAdminDomainId: UUID = {
    val domainDao = new DomainDao(executor, mapper)
    val domainFindRequest = new DomainFindRequest
    domainDao.find(domainFindRequest).head.id
  }

  lazy val retrievedTemplateId: UUID = {
    val templateDao = new TemplateDao(executor, mapper)
    val templateFindRequest = new TemplateFindRequest(Featured)
    templateDao.find(templateFindRequest).head.id
  }
}
