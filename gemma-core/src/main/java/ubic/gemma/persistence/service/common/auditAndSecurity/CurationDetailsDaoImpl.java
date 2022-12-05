/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Date;

/**
 * Data access object for Curation Details
 *
 * @author tesarst
 */
@Repository
public class CurationDetailsDaoImpl extends AbstractDao<CurationDetails> implements CurationDetailsDao {

    @Autowired
    public CurationDetailsDaoImpl( SessionFactory sessionFactory ) {
        super( CurationDetails.class, sessionFactory );
    }

    /**
     * Creates new CurationDetails object and persists it.
     *
     * @return the newly created CurationDetails object.
     */
    @Override
    public CurationDetails create() {
        CurationDetails cd = new CurationDetails( new Date(), null, true, null, false, null, null );
        return this.create( cd );
    }

    /**
     * Updates the given curatable object based on the provided event type.
     *
     * @param curatable  the curatable object that should be updated with the given event.
     * @param auditEvent the event containing information about the necessary update.
     */
    @Override
    public void update( Curatable curatable, AuditEvent auditEvent ) {
        Hibernate.initialize( curatable );
        // Update the lastUpdated property.
        auditEvent.getEventType().updateLastUpdated( curatable );

        // Update other curatable properties, if the event updates them.
        if ( this.isEventCurationUpdate( auditEvent ) ) {
            CurationDetailsEvent eventType = ( CurationDetailsEvent ) auditEvent.getEventType();
            eventType.setCurationDetails( curatable, auditEvent );
        }
        // Persist changes
        this.getSessionFactory().getCurrentSession().merge( curatable.getCurationDetails() );
    }

    /**
     * Checks whether the auditEvent has all the properties to be accepted for processing.
     *
     * @param auditEvent the audit event to be checked
     * @return true, if the given audit event satisfies all the conditions, false otherwise.
     */
    private boolean isEventCurationUpdate( AuditEvent auditEvent ) {
        return auditEvent != null // Can not be null
                && auditEvent.getEventType() != null // Type must be set
                && CurationDetailsEvent.class.isAssignableFrom( auditEvent.getEventType().getClass() )
                // Type must be extension of CurationDetailsEvent...
                && auditEvent.getEventType().getClass()
                != CurationDetailsEvent.class; // ...but not the CurationDetailsEvent itself.
    }

}
