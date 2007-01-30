/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.analysis.report;

import java.util.Date;

import ubic.gemma.model.common.auditAndSecurity.AuditEventService;

/**
 * Service to collect data on object that are new in the system.
 * 
 * @spring.bean id="whatsNewService"
 * @spring.property name="auditEventService" ref="auditEventService"
 * @author pavlidis
 * @version $Id$
 */
public class WhatsNewService {
    AuditEventService auditEventService;

    public void setAuditEventService( AuditEventService auditEventService ) {
        this.auditEventService = auditEventService;
    }

    /**
     * @param date
     * @return representing the updated or new objects.
     */
    @SuppressWarnings("unchecked")
    public WhatsNew getReport( Date date ) {
        WhatsNew wn = new WhatsNew( date );
        wn.setUpdatedObjects( auditEventService.getUpdatedSinceDate( date ) );
        wn.setNewObjects( auditEventService.getNewSinceDate( date ) );
        return wn;
    }

}
