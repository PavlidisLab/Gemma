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
package ubic.gemma.model.common.auditAndSecurity;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.common.auditAndSecurity.eventType.CurationDetailsEvent;
import ubic.gemma.persistence.AbstractDao;

/**
 * Data access object for Curation Details
 *
 * @author tesarst
 */
@Component
public class CurationDetailsDaoImpl extends AbstractDao<CurationDetails> implements CurationDetailsDao {

    private static final String ILLEGAL_EVENT_TYPE_ERR_MSG = "Can not set trouble with event of given type.";
    private static final String NULL_ID_ERR_MSG = "Can not load Curation Details with null ID.";

    /* ********************************
     * Constructors
     * ********************************/

    @Autowired
    public CurationDetailsDaoImpl( SessionFactory sessionFactory ) {
        super( CurationDetailsDaoImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    /* ********************************
     * Public methods
     * ********************************/

    /**
     * CurationDetails are not lazy loaded, so this should not be necessary unless you are trying to only
     * retrieve the CurationDetails object and not the Curatable object it is attached to.
     *
     * @param id the id of the CurationDetails to be loaded.
     * @return CurationDetails with given id, null if such object does not exist.
     * @throws IllegalArgumentException if the given id is null.
     */
    @Override
    public CurationDetails load( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( NULL_ID_ERR_MSG );
        }
        final Object entity = this.getHibernateTemplate().get( CurationDetails.class, id );
        return ( CurationDetails ) entity;
    }

    /**
     * Creates new CurationDetails object and persists it.
     *
     * @param createdEvent the creation event of the Curatable object these details will be attached to.
     * @return the newly created CurationDetails object.
     */
    @Override
    public CurationDetails create( AuditEvent createdEvent ) {
        CurationDetails cd = new CurationDetails( createdEvent.getDate(), null, false, null, false, null, null );
        return this.create( cd );
    }

    /**
     * Updates the given curatable object based on the provided event type.
     *
     * @param curatable  the curatable object that should be updated with the given event.
     * @param auditEvent the event containing information about the necessary update. This method only
     *                   accepts events whose type is an extension of {@link CurationDetailsEvent}. Audit events
     *                   with any other type will cause an exception.
     * @throws IllegalArgumentException if the given audit event had an unrecognised type.
     */
    @Override
    public void update( Curatable curatable, AuditEvent auditEvent ) {

        if ( !this.isEventLegal( auditEvent ) ) {
            throw new IllegalArgumentException( ILLEGAL_EVENT_TYPE_ERR_MSG );
        }

        //TODO check if Lock Request is necessary.

        Hibernate.initialize( curatable );

        CurationDetailsEvent eventType = ( CurationDetailsEvent ) auditEvent.getEventType();

        eventType.setCurationDetails( curatable, auditEvent );

        this.update( curatable.getCurationDetails() );

    }

    /* ********************************
     * Private methods
     * ********************************/

    /**
     * Checks whether the auditEvent has all the properties to be accepted for processing.
     *
     * @param auditEvent the audit event to be checked
     * @return true, if the given audit event satisfies all the conditions, false otherwise.
     */
    private boolean isEventLegal( AuditEvent auditEvent ) {
        return auditEvent != null // Can not be null
                && auditEvent.getEventType() != null // Type must be set
                && CurationDetailsEvent.class.isAssignableFrom( auditEvent.getEventType().getClass() )
                // Type must be extension of CurationDetailsEvent...
                && auditEvent.getEventType().getClass()
                != CurationDetailsEvent.class; // ...but not the CurationDetailsEvent itself.
    }

}
