/*
 * The Gemma project
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
package ubic.gemma.core.security.audit;

import gemma.gsec.authentication.UserManager;
import gemma.gsec.model.User;
import gemma.gsec.util.CrudUtils;
import gemma.gsec.util.CrudUtilsImpl;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ubic.gemma.core.security.authorization.acl.AclAdvice;
import ubic.gemma.model.common.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditHelper;
import ubic.gemma.persistence.util.ReflectionUtil;
import ubic.gemma.persistence.util.Settings;

import javax.annotation.PostConstruct;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Manage audit trails on objects.
 *
 * @author pavlidis
 */
@Component
public class AuditAdvice {

    // Note that we have a special logger configured for this class, so remove events get stored.
    private static final Logger log = LoggerFactory.getLogger( AuditAdvice.class.getName() );

    private boolean AUDIT_CREATE = true;

    private boolean AUDIT_DELETE = true;

    private boolean AUDIT_UPDATE = true;

    @Autowired
    private AuditHelper auditHelper;

    @Autowired
    private CrudUtils crudUtils;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private UserManager userManager;

    /**
     * Entry point. This only takes action if the method involves AbstractAuditables.
     *
     * @param pjp      pjp
     * @param retValue return value
     */
    @SuppressWarnings("unused") // entry point
    public void doAuditAdvice( JoinPoint pjp, Object retValue ) {

        final Signature signature = pjp.getSignature();
        final String methodName = signature.getName();
        final Object[] args = pjp.getArgs();

        Object object = this.getPersistentObject( retValue, methodName, args );

        if ( object == null )
            return;

        User user = userManager.getCurrentUser();

        if ( user == null ) {
            AuditAdvice.log.info( "User could not be determined (anonymous?), audit will be skipped." );
            return;
        }

        if ( object instanceof Collection ) {
            for ( final Object o : ( Collection<?> ) object ) {
                if ( AbstractAuditable.class.isAssignableFrom( o.getClass() ) ) {
                    this.process( methodName, ( AbstractAuditable ) o, user );
                }
            }
        } else if ( ( AbstractAuditable.class.isAssignableFrom( object.getClass() ) ) ) {
            this.process( methodName, ( AbstractAuditable ) object, user );
        }
    }

    @PostConstruct
    protected void init() {

        try {
            AUDIT_UPDATE = Settings.getBoolean( "audit.update" );
            AUDIT_DELETE = Settings.getBoolean( "audit.delete" );
            AUDIT_CREATE = Settings.getBoolean( "audit.create" ) || AUDIT_UPDATE;
        } catch ( NoSuchElementException e ) {
            AuditAdvice.log.error( "Configuration error: " + e.getMessage() + "; will use default values" );
        }
    }

    private boolean canSkipAssociationCheck( Object object, String propertyName ) {

        /*
         * If this is an expression experiment, don't go down the data vectors.
         */
        if ( ExpressionExperiment.class.isAssignableFrom( object.getClass() ) && (
                propertyName.equals( "rawExpressionDataVectors" ) || propertyName
                        .equals( "processedExpressionDataVectors" ) ) ) {
            AuditAdvice.log.trace( "Skipping vectors" );
            return true;
        }

        /*
         * Array designs...
         */
        if ( ArrayDesign.class.isAssignableFrom( object.getClass() ) && ( propertyName.equals( "compositeSequences" )
                || propertyName.equals( "reporters" ) ) ) {
            AuditAdvice.log.trace( "Skipping probes" );
            return true;
        }

        return false;
    }

    /**
     * For cases where don't have a cascade but the other end is auditable.
     * Implementation note. This is kind of inelegant, but the alternative is to check _every_ association, which will
     * often not be reachable.
     *
     * @param object   we are checking
     * @param property of the object
     * @return true if the association should be followed.
     * @see AclAdvice for similar code
     */
    @SuppressWarnings("SimplifiableIfStatement") // Better readability
    private boolean specialCaseForAssociationFollow( Object object, String property ) {

        if ( BioAssay.class.isAssignableFrom( object.getClass() ) && ( property.equals( "samplesUsed" ) || property
                .equals( "arrayDesignUsed" ) ) ) {
            return true;
        } else
            return DesignElementDataVector.class.isAssignableFrom( object.getClass() ) && property
                    .equals( "bioAssayDimension" );

    }

    /**
     * Adds 'create' AuditEvent to audit trail of the passed AbstractAuditable.
     *
     * @param note Additional text to add to the automatically generated note.
     */
    private void addCreateAuditEvent( final AbstractAuditable auditable, User user, final String note ) {

        if ( this.isNullOrTransient( auditable ) )
            return;

        AuditTrail auditTrail = auditable.getAuditTrail();

        this.ensureInSession( auditTrail );

        if ( auditTrail != null && !auditTrail.getEvents().isEmpty() ) {
            // This can happen when we persist objects and then let this interceptor look at them again
            // while persisting parent objects. That's okay.
            if ( AuditAdvice.log.isDebugEnabled() )
                AuditAdvice.log
                        .debug( "Call to addCreateAuditEvent but the auditTrail already has events. AuditTrail id: "
                                + auditTrail.getId() );
            return;
        }

        String details = "Create " + auditable.getClass().getSimpleName() + " " + auditable.getId() + note;

        try {
            auditHelper.addCreateAuditEvent( auditable, details, user );
            if ( AuditAdvice.log.isDebugEnabled() ) {
                AuditAdvice.log
                        .debug( "Audited event: " + ( note.length() > 0 ? note : "[no note]" ) + " on " + auditable
                                .getClass().getSimpleName() + ":" + auditable.getId() + " by " + user.getUserName() );
            }

        } catch ( UsernameNotFoundException e ) {
            AuditAdvice.log.warn( "No user, cannot add 'create' event" );
        }
    }

    private void addDeleteAuditEvent( AbstractAuditable d, User user ) {
        assert d != null;
        // what else could we do? But need to keep this record in a good place. See log4j.properties.
        if ( AuditAdvice.log.isInfoEnabled() ) {
            String un = "";
            if ( user != null ) {
                un = "by " + user.getUserName();
            }
            AuditAdvice.log
                    .info( "Delete event on entity " + d.getClass().getName() + ":" + d.getId() + "  [" + d + "] "
                            + un );
        }
    }

    private void addUpdateAuditEvent( final AbstractAuditable auditable, User user ) {
        assert auditable != null;

        AuditTrail auditTrail = auditable.getAuditTrail();

        this.ensureInSession( auditTrail );

        if ( auditTrail == null || auditTrail.getEvents().isEmpty() ) {
            /*
             * Note: This can happen for ExperimentalFactors when loading from GEO etc. because of the bidirectional
             * association and the way we persist them. See ExpressionPersister. (actually this seems to be fixed...)
             */
            AuditAdvice.log.error( "No create event for update method call on " + auditable
                    + ", performing 'create' instead" );
            this.addCreateAuditEvent( auditable, user, " - Event added on update of existing object." );
        } else {
            String note = "Updated " + auditable.getClass().getSimpleName() + " " + auditable.getId();
            auditHelper.addUpdateAuditEvent( auditable, note, user );
            if ( AuditAdvice.log.isDebugEnabled() ) {
                AuditAdvice.log.debug( "Audited event: " + note + " on " + auditable.getClass().getSimpleName() + ":"
                        + auditable.getId() + " by " + user.getUserName() );
            }
        }
    }

    private void ensureInSession( AuditTrail auditTrail ) {
        if ( auditTrail == null )
            return;
        /*
         * Ensure we have the object in the session. It might not be, if we have flushed the session.
         */
        Session session = sessionFactory.getCurrentSession();
        if ( !session.contains( auditTrail ) ) {
            session.buildLockRequest( LockOptions.NONE ).lock( auditTrail );
        }
    }

    private Object getPersistentObject( Object retValue, String methodName, Object[] args ) {
        if ( retValue == null && ( CrudUtilsImpl.methodIsDelete( methodName ) || CrudUtilsImpl
                .methodIsUpdate( methodName ) ) ) {

            // Only deal with single-argument update methods.
            if ( args.length > 1 )
                return null;

            assert args.length > 0;
            return args[0];
        }
        return retValue;
    }

    private boolean isNullOrTransient( final AbstractAuditable auditable ) {
        return auditable == null || auditable.getId() == null;
    }

    /**
     * Check if the associated object needs to be 'create audited'. Example: gene products are created by cascade when
     * calling update on a gene.
     */
    private void maybeAddCascadeCreateEvent( Object object, AbstractAuditable auditable, User user ) {
        if ( AuditAdvice.log.isDebugEnabled() )
            AuditAdvice.log.debug( "Checking for whether to cascade create event from " + auditable + " to " + object );

        if ( auditable.getAuditTrail() == null || auditable.getAuditTrail().getEvents().isEmpty() ) {
            this.addCreateAuditEvent( auditable, user, " - created by cascade from " + object );
        }
    }

    /**
     * Process auditing on the object.
     */
    private void process( final String methodName, final AbstractAuditable auditable, User user ) {

        // do this here, when we are sure to be in a transaction. But might be repetitive when working on a collection.
        this.sessionFactory.getCurrentSession().setReadOnly( user, true );

        if ( AuditAdvice.log.isTraceEnabled() ) {
            AuditAdvice.log
                    .trace( "***********  Start Audit of " + methodName + " on " + auditable + " *************" );
        }
        assert auditable != null : "Null entity passed to auditing [" + methodName + " on " + null + "]";
        assert auditable.getId() != null :
                "Transient instance passed to auditing [" + methodName + " on " + auditable + "]";

        if ( AUDIT_CREATE && CrudUtilsImpl.methodIsCreate( methodName ) ) {
            this.addCreateAuditEvent( auditable, user, "" );
            this.processAssociations( methodName, auditable, user );
        } else if ( AUDIT_UPDATE && CrudUtilsImpl.methodIsUpdate( methodName ) ) {
            this.addUpdateAuditEvent( auditable, user );

            /*
             * Do not process associations during an update except to add creates to new objects. Otherwise this would
             * result in update events getting added to all child objects, which is silly; and in any case they might be
             * proxies.
             */
            this.processAssociations( methodName, auditable, user );
        } else if ( AUDIT_DELETE && CrudUtilsImpl.methodIsDelete( methodName ) ) {
            this.addDeleteAuditEvent( auditable, user );
        }

        if ( AuditAdvice.log.isTraceEnabled() )
            AuditAdvice.log.trace( "============  End Audit ==============" );
    }

    /**
     * Fills in audit trails on newly created child objects after a 'create' or 'update'. It does not add 'update'
     * events on the child objects.
     * Thus if the update is on an expression experiment that has a new Characteristic, the Characteristic will have a
     * 'create' event, and the EEE will get an added update event (via the addUpdateAuditEvent call elsewhere, not here)
     *
     * @see AclAdvice for similar code for ACLs
     */
    private void processAssociations( String methodName, Object object, User user ) {

        if ( object instanceof AuditTrail )
            return; // don't audit audit trails.

        EntityPersister persister = crudUtils.getEntityPersister( object );
        if ( persister == null ) {
            throw new IllegalArgumentException( "No persister found for " + object.getClass().getName() );
        }
        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
        String[] propertyNames = persister.getPropertyNames();
        try {
            for ( int j = 0; j < propertyNames.length; j++ ) {
                CascadeStyle cs = cascadeStyles[j];

                String propertyName = propertyNames[j];

                if ( !this.specialCaseForAssociationFollow( object, propertyName ) && (
                        this.canSkipAssociationCheck( object, propertyName ) || !crudUtils
                                .needCascade( methodName, cs ) ) ) {
                    continue;
                }

                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyName );
                Object associatedObject = ReflectionUtil.getProperty( object, descriptor );

                if ( associatedObject == null )
                    continue;

                Class<?> propertyType = descriptor.getPropertyType();

                if ( AbstractAuditable.class.isAssignableFrom( propertyType ) ) {

                    AbstractAuditable auditable = ( AbstractAuditable ) associatedObject;
                    try {

                        this.maybeAddCascadeCreateEvent( object, auditable, user );

                        this.processAssociations( methodName, auditable, user );
                    } catch ( LazyInitializationException e ) {
                        // If this happens, it means the object can't be 'new' so adding audit trail can't
                        // be necessary.
                        if ( AuditAdvice.log.isDebugEnabled() )
                            AuditAdvice.log.debug( "Caught lazy init error while processing " + auditable + ": " + e
                                    .getMessage() + " - skipping creation of cascade event." );
                    }

                } else if ( Collection.class.isAssignableFrom( propertyType ) ) {
                    Collection<?> associatedObjects = ( Collection<?> ) associatedObject;

                    try {
                        Hibernate.initialize( associatedObjects );
                        for ( Object collectionMember : associatedObjects ) {

                            if ( AbstractAuditable.class.isAssignableFrom( collectionMember.getClass() ) ) {
                                AbstractAuditable auditable = ( AbstractAuditable ) collectionMember;
                                try {
                                    Hibernate.initialize( auditable );
                                    this.maybeAddCascadeCreateEvent( object, auditable, user );
                                    this.processAssociations( methodName, collectionMember, user );
                                } catch ( LazyInitializationException e ) {

                                    if ( AuditAdvice.log.isDebugEnabled() )
                                        AuditAdvice.log
                                                .debug( "Caught lazy init error while processing " + auditable + ": "
                                                        + e.getMessage() + " - skipping creation of cascade event." );
                                    // If this happens, it means the object can't be 'new' so adding audit trail can't
                                    // be necessary. But keep checking.
                                }

                            }
                        }
                    } catch ( LazyInitializationException e ) {

                        // If this happens, it means the object can't be 'new' so adding audit trail can't
                        // be necessary.
                        if ( AuditAdvice.log.isDebugEnabled() )
                            AuditAdvice.log
                                    .debug( "Caught lazy init error while processing " + object + ": " + e.getMessage()
                                            + " - skipping creation of cascade event." );
                    }

                }
            }
        } catch ( IllegalAccessException | InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
    }

}
