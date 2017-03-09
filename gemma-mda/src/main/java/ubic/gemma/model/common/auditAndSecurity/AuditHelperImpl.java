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

import gemma.gsec.model.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.common.Auditable;

import java.util.Date;

/**
 * @author anton
 */
@Component
public class AuditHelperImpl implements AuditHelper {

    private static Log log = LogFactory.getLog( AuditHelperImpl.class );

    @Autowired
    private AuditTrailDao auditTrailDao;

    @Override
    public AuditEvent addCreateAuditEvent( Auditable auditable, String note, User user ) {
        this.addAuditTrailIfNeeded( auditable );
        assert auditable.getAuditTrail() != null;
        assert auditable.getAuditTrail().getEvents().size() == 0;

        AuditEvent auditEvent = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.CREATE, note, null, user, null );
        return this.tryUpdate( auditable, auditEvent );

    }

    @Override
    public AuditEvent addUpdateAuditEvent( Auditable auditable, String note, User user ) {
        AuditEvent auditEvent = AuditEvent.Factory
                .newInstance( new Date(), AuditAction.UPDATE, note, null, user, null );
        return this.tryUpdate( auditable, auditEvent );
    }

    private AuditEvent tryUpdate( Auditable auditable, AuditEvent auditEvent ) {
        try {
            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Exception e ) {
            log.warn( ">>>>>>> AUDIT ERROR >>>>>>>>  " + e.getMessage() );
            throw e;
        }
    }

    private AuditTrail addAuditTrailIfNeeded( Auditable auditable ) {

        if ( auditable.getAuditTrail() != null )
            return auditable.getAuditTrail();

        // no need to persist it here
        AuditTrail auditTrail = AuditTrail.Factory.newInstance();
        auditable.setAuditTrail( auditTrail );

        return auditable.getAuditTrail();
    }

}
