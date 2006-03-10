/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Auditable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailServiceImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceBase {

    private static Log log = LogFactory.getLog( AuditTrailServiceImpl.class.getName() );

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailService#audit(ubic.gemma.model.common.Describable,
     *      ubic.gemma.model.common.auditAndSecurity.AuditEvent)
     */
    @Override
    protected void handleAudit( Auditable entity, ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent )
            throws java.lang.Exception {

        if ( entity == null || entity.getId() == null ) return;

        AuditTrail at = entity.getAuditTrail();
        if ( at == null ) {
            at = AuditTrail.Factory.newInstance();
            at.start(); // uh-oh, have to update the entity. Hard to do from here.
            if ( auditEvent != null ) at.addEvent( auditEvent ); // should we do that? I guess so.
            this.getAuditTrailDao().create( at );
            log.warn( "Creating new audit trail for " + entity );
        } else {
            if ( auditEvent == null ) throw new IllegalArgumentException( "auditEvent cannot be null" );
            at.addEvent( auditEvent );
            this.getAuditTrailDao().update( at );
            log.debug( "Added event " + auditEvent.getAction() + " to " + entity );
        }
    }

    /**
     * @param auditTrail
     * @return
     */
    @Override
    protected AuditTrail handleCreate( AuditTrail auditTrail ) {
        return this.getAuditTrailDao().create( auditTrail );
    }

}