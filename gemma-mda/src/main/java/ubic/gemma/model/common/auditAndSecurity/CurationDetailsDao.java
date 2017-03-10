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
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.persistence.AbstractDao;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Data access object for Curation Details
 *
 * @author tesarst
 */
@Component
public class CurationDetailsDao extends AbstractDao<CurationDetails> {

    private static final String ILLEGAL_EVENT_TYPE_ERR_MSG = "Can not set trouble with event of given type.";
    private static final String NULL_ID_ERR_MSG = "Can not load Curation Details with null ID.";

    /**
     * List of event types that can affect the Troubled value in Curation Details
     */
    private static final List<Class<? extends AuditEventType>> LEGAL_EVENT_TYPES_TROUBLE = new LinkedList<Class<? extends AuditEventType>>(
            Arrays.asList( TroubledStatusFlagEvent.class, NotTroubledStatusFlagEvent.class ) );

    /**
     * List of event types that can affect the Needs Attention value in Curation Details
     */
    private static final List<Class<? extends AuditEventType>> LEGAL_EVENT_TYPES_NEEDS_ATTENTION = new LinkedList<Class<? extends AuditEventType>>(
            Arrays.asList( NeedsAttentionEvent.class, DoesNotNeedAttentionEvent.class ) );

    /**
     * List of event types that can affect the Needs Attention value in Curation Details
     */
    private static final List<Class<? extends AuditEventType>> LEGAL_EVENT_TYPES_CURATION_NOTE = new LinkedList<Class<? extends AuditEventType>>(
            Collections.singletonList( CurationNoteUpdateEvent.class ) );

    /* ********************************
     * Constructors
     * ********************************/

    @Autowired
    public CurationDetailsDao( SessionFactory sessionFactory ) {
        super( CurationDetailsDao.class );
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
    public CurationDetails create( AuditEvent createdEvent ) {
        CurationDetails cd = new CurationDetails( createdEvent.getDate(), null, false, null, false, null, null );
        return this.create( cd );
    }

    //TODO check if initialize method is necessary

    /**
     * Sets the troubled flag on the given curatable object. The new troubled value is decided based on the event type
     * of the given audit event.
     * If you are unsure what the audit events effect on the Curation Details should be, use the {@link #update(Curatable, AuditEvent)} method.
     *
     * @param curatable    a curatable object whose trouble should be set.
     * @param troubleEvent an audit event with type of either {@link ubic.gemma.model.common.auditAndSecurity.eventType.TroubledStatusFlagEvent}
     *                     of {@link ubic.gemma.model.common.auditAndSecurity.eventType.NotTroubledStatusFlagEvent}.
     *                     Audit events with any other type will cause an exception.
     * @throws IllegalArgumentException if the given audit event had an unrecognised type.
     */
    public void setTroubled( Curatable curatable, AuditEvent troubleEvent ) {
        if ( !this.isEventLegal( troubleEvent, LEGAL_EVENT_TYPES_TROUBLE ) ) {
            throw new IllegalArgumentException( ILLEGAL_EVENT_TYPE_ERR_MSG );
        }

        Hibernate.initialize( curatable );

        if ( troubleEvent.getEventType() instanceof TroubledStatusFlagEvent ) {
            curatable.getCurationDetails().setTroubled( true );
        } else if ( troubleEvent.getEventType() instanceof NotTroubledStatusFlagEvent ) {
            curatable.getCurationDetails().setTroubled( false );
        }

        this.update( curatable.getCurationDetails() );
    }

    /**
     * Sets the needs-attention flag on the given curatable object. The new needs-attention value is decided based on the event type
     * of the given audit event.
     * If you are unsure what the audit events effect on the Curation Details should be, use the {@link #update(Curatable, AuditEvent)} method.
     *
     * @param curatable           a curatable object whose needs-attention should be set.
     * @param needsAttentionEvent an audit event with type of either {@link ubic.gemma.model.common.auditAndSecurity.eventType.NeedsAttentionEvent}
     *                            of {@link ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent}.
     *                            Audit events with any other type will cause an exception.
     * @throws IllegalArgumentException if the given audit event had an unrecognised type.
     */
    public void setNeedsAttention( Curatable curatable, AuditEvent needsAttentionEvent ) {
        if ( !this.isEventLegal( needsAttentionEvent, LEGAL_EVENT_TYPES_NEEDS_ATTENTION ) ) {
            throw new IllegalArgumentException( ILLEGAL_EVENT_TYPE_ERR_MSG );
        }

        Hibernate.initialize( curatable );

        if ( needsAttentionEvent.getEventType() instanceof NeedsAttentionEvent ) {
            curatable.getCurationDetails().setNeedsAttention( true );
        } else if ( needsAttentionEvent.getEventType() instanceof DoesNotNeedAttentionEvent ) {
            curatable.getCurationDetails().setNeedsAttention( false );
        }

        this.update( curatable.getCurationDetails() );
    }

    /**
     * Sets a new value of the curation note for the given curatable object. The new value is retrieved from the given
     * audit event.
     * If you are unsure what the audit events effect on the Curation Details should be, use the {@link #update(Curatable, AuditEvent)} method.
     *
     * @param curatable               the object whose curation note should be altered.
     * @param curationNoteUpdateEvent an audit event of type {@link ubic.gemma.model.common.auditAndSecurity.eventType.CurationNoteUpdateEvent}.
     *                                Audit events with any other type will cause an exception.
     *                                the event containing the note information. This method will look for the new
     *                                curation note text via {@link AuditEvent#getNote()} call. This text does not
     *                                undergo any inspection. I.e any intended null, sanity or escape checks should be done
     *                                prior to this method.
     * @throws IllegalArgumentException if the given audit event had an unrecognised type.
     */
    public void setNewCurationNote( Curatable curatable, AuditEvent curationNoteUpdateEvent ) {
        if ( !this.isEventLegal( curationNoteUpdateEvent, LEGAL_EVENT_TYPES_CURATION_NOTE ) ) {
            throw new IllegalArgumentException( ILLEGAL_EVENT_TYPE_ERR_MSG );
        }

        Hibernate.initialize( curatable );

        curatable.getCurationDetails().setCurationNote( curationNoteUpdateEvent.getNote() );

        this.update( curatable.getCurationDetails() );
    }

    /**
     * Updates the given curatable object based on the provided event type.
     *
     * @param curatable            the curatable object that should be updated with the given event.
     * @param curationDetailsEvent the event containing information about the necessary update. This method only
     *                             accepts events whose type is an extension of {@link CurationDetailsEvent}. Audit events
     *                             with any other type will cause an exception.
     * @throws IllegalArgumentException if the given audit event had an unrecognised type.
     */
    public void update( Curatable curatable, AuditEvent curationDetailsEvent ) {
        List<Class<? extends AuditEventType>> allLegalTypes = LEGAL_EVENT_TYPES_TROUBLE;
        allLegalTypes.addAll( LEGAL_EVENT_TYPES_NEEDS_ATTENTION );
        allLegalTypes.addAll( LEGAL_EVENT_TYPES_CURATION_NOTE );

        if ( !this.isEventLegal( curationDetailsEvent, allLegalTypes ) ) {
            throw new IllegalArgumentException( ILLEGAL_EVENT_TYPE_ERR_MSG );
        }

        //TODO check if Lock Request is necessary.

        Class<? extends AuditEventType> eventClass = curationDetailsEvent.getEventType().getClass();
        if ( LEGAL_EVENT_TYPES_TROUBLE.contains( eventClass ) ) {
            this.setTroubled( curatable, curationDetailsEvent );
        } else if ( LEGAL_EVENT_TYPES_NEEDS_ATTENTION.contains( eventClass ) ) {
            this.setNeedsAttention( curatable, curationDetailsEvent );
        } else if ( LEGAL_EVENT_TYPES_TROUBLE.contains( eventClass ) ) {
            this.setNewCurationNote( curatable, curationDetailsEvent );
        }

    }

    /* ********************************
     * Private methods
     * ********************************/

    /**
     * Checks whether the auditEvent has all the properties to be accepted for processing.
     *
     * @param auditEvent the audit event to be checked
     * @param legalTypes list of types that the given event can be.
     * @return true, if the given audit event satisfies all the conditions, false otherwise.
     */
    private boolean isEventLegal( AuditEvent auditEvent, List<Class<? extends AuditEventType>> legalTypes ) {
        return auditEvent != null // Can not be null
                && auditEvent.getEventType() != null // Type must be set
                && legalTypes.contains( auditEvent.getEventType().getClass() ); // Type must be one of the given types
    }

}
