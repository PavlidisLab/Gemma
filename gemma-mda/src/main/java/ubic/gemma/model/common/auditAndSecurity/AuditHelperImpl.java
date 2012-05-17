/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.common.Auditable;

/**
 * @author anton
 * @version $Id$
 */
@Component
public class AuditHelperImpl implements AuditHelper {

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Autowired
    private StatusDao statusDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditHelper#addCreateAuditEvent(ubic.gemma.model.common.Auditable,
     * java.lang.String, ubic.gemma.model.common.auditAndSecurity.User)
     */
    public AuditEvent addCreateAuditEvent( Auditable auditable, String note, User user ) {

        this.addAuditTrailIfNeeded( auditable );
        this.addStatusIfNeeded( auditable );

        assert auditable.getAuditTrail() != null;
        assert auditable.getAuditTrail().getEvents().size() == 0;

        try {
            AuditEvent auditEvent = AuditEvent.Factory.newInstance();
            auditEvent.setDate( new Date() );
            auditEvent.setAction( AuditAction.CREATE );
            auditEvent.setNote( note );
            this.statusDao.update( auditable, null );

            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addUpdateEvent(Auditable auditable, String note)' --> " + th,
                    th );
        }

    }

    private AuditTrail addAuditTrailIfNeeded( Auditable auditable ) {
        if ( auditable.getAuditTrail() == null ) {

            try {

                AuditTrail auditTrail = AuditTrail.Factory.newInstance();
                auditable.setAuditTrail( auditTrailDao.create( auditTrail ) );

            } catch ( Exception e ) {

                /*
                 * This can happen if we hit an auditable during a read-only event: programming error.
                 */
                throw new IllegalStateException( "Invalid attempt to create an audit trail on: " + auditable, e );
            }

        }

        return auditable.getAuditTrail();
    }

    private void addStatusIfNeeded( Auditable auditable ) {
        if ( auditable.getStatus() == null ) {
            statusDao.initializeStatus( auditable );
            assert auditable.getStatus() != null && auditable.getStatus().getId() != null;
        }
    }

    @Override
    public AuditEvent addUpdateAuditEvent( Auditable auditable, String note, User user ) {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( new Date() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setNote( note );
        this.statusDao.update( auditable, null );
        return this.auditTrailDao.addEvent( auditable, auditEvent );
    }

}
