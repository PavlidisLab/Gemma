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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.common.Auditable;

/**
 * @author anton
 * @version $Id$
 */
@Component
public class AuditHelperImpl implements AuditHelper {

    private static Log log = LogFactory.getLog( AuditHelperImpl.class );

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
    @Override
    public AuditEvent addCreateAuditEvent( Auditable auditable, String note, User user ) {
        this.addAuditTrailIfNeeded( auditable );
        this.addStatusIfNeeded( auditable );
        assert auditable.getAuditTrail() != null;
        assert auditable.getAuditTrail().getEvents().size() == 0;

        AuditEvent auditEvent = AuditEvent.Factory.newInstance( new Date(), AuditAction.CREATE, note, null, user, null );
        try {
            this.statusDao.update( auditable, null );
            AuditEvent a = this.auditTrailDao.addEvent( auditable, auditEvent );
            return a;
        } catch ( Exception e ) {
            log.warn( ">>>>>>> AUDIT ERROR >>>>>>>>  " + e.getMessage() );
            throw e;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditHelper#addUpdateAuditEvent(ubic.gemma.model.common.Auditable,
     * java.lang.String, ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public AuditEvent addUpdateAuditEvent( Auditable auditable, String note, User user ) {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, note, null, user, null );
        try {
            this.statusDao.update( auditable, null );
            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Exception e ) {
            log.warn( ">>>>>>> AUDIT ERROR >>>>>>>>  " + e.getMessage() );
            throw e;
        }
    }

    /**
     * @param auditable
     * @return
     */
    private AuditTrail addAuditTrailIfNeeded( Auditable auditable ) {

        if ( auditable.getAuditTrail() != null ) return auditable.getAuditTrail();

        // no need to persist it here
        AuditTrail auditTrail = AuditTrail.Factory.newInstance();
        auditable.setAuditTrail( auditTrail );

        return auditable.getAuditTrail();
    }

    /**
     * @param auditable
     */
    private void addStatusIfNeeded( Auditable auditable ) {
        if ( auditable.getStatus() == null ) {
            statusDao.initializeStatus( auditable );
            assert auditable.getStatus() != null && auditable.getStatus().getId() != null;
        }
    }

}
