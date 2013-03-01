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
package ubic.gemma.security.audit;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
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

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditHelper;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.CrudUtils;
import ubic.gemma.persistence.CrudUtilsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.security.authorization.acl.AclAdvice;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.ReflectionUtil;

/**
 * Manage audit trails on objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class AuditAdvice {

    // Note that we have a special logger configured for this class, so delete events get stored.
    private static Logger log = LoggerFactory.getLogger( AuditAdvice.class.getName() );

    @Autowired
    private CrudUtils crudUtils;

    @Autowired
    private UserManager userManager;

    @Autowired
    private AuditHelper auditHelper;

    @Autowired
    private SessionFactory sessionFactory;

    private boolean AUDIT_CREATE = true;
    private boolean AUDIT_DELETE = true;
    private boolean AUDIT_UPDATE = true;

    @PostConstruct
    protected void init() {

        try {
            AUDIT_UPDATE = ConfigUtils.getBoolean( "audit.update" );
            AUDIT_DELETE = ConfigUtils.getBoolean( "audit.delete" );
            AUDIT_CREATE = ConfigUtils.getBoolean( "audit.create" ) || AUDIT_UPDATE;
        } catch ( NoSuchElementException e ) {
            log.error( "Configuration error: " + e.getMessage() + "; will use default values" );
        }
    }

    /**
     * Entry point
     * 
     * @param pjp
     * @return
     * @throws Throwable
     */
    public void doAuditAdvice( JoinPoint pjp, Object retValue ) throws Throwable {

        final Signature signature = pjp.getSignature();
        final String methodName = signature.getName();
        final Object[] args = pjp.getArgs();

        Object object = getPersistentObject( retValue, methodName, args );

        if ( object == null ) return;
        User user = userManager.getCurrentUser();

        if ( user == null ) {
            log.info( "User could not be determined (anonymous?), audit will be skipped." );
            return;
        }

        assert user != null;
        if ( object instanceof Collection ) {
            for ( final Object o : ( Collection<?> ) object ) {
                if ( Auditable.class.isAssignableFrom( o.getClass() ) ) {
                    process( methodName, ( Auditable ) o, user );
                }
            }
        } else if ( ( Auditable.class.isAssignableFrom( object.getClass() ) ) ) {
            process( methodName, ( Auditable ) object, user );
        }
    }

    /**
     * Adds 'create' AuditEvent to audit trail of the passed Auditable.
     * 
     * @param auditable
     * @param note Additional text to add to the automatically generated note.
     */
    private void addCreateAuditEvent( final Auditable auditable, User user, final String note ) {

        if ( isNullOrTransient( auditable ) ) return;

        AuditTrail auditTrail = auditable.getAuditTrail();

        ensureInSession( auditTrail );

        if ( auditTrail != null && !auditTrail.getEvents().isEmpty() ) {
            // This can happen when we persist objects and then let this interceptor look at them again
            // while persisting parent objects. That's okay.
            if ( log.isDebugEnabled() )
                log.debug( "Call to addCreateAuditEvent but the auditTrail already has events. AuditTrail id: "
                        + auditTrail.getId() );
            return;
        }

        String details = "Create " + auditable.getClass().getSimpleName() + " " + auditable.getId() + note;

        try {
            auditHelper.addCreateAuditEvent( auditable, details, user );
            if ( log.isDebugEnabled() ) {
                log.debug( "Audited event: " + ( note.length() > 0 ? note : "[no note]" ) + " on "
                        + auditable.getClass().getSimpleName() + ":" + auditable.getId() + " by " + user.getUserName() );
            }

        } catch ( UsernameNotFoundException e ) {
            log.warn( "No user, cannot add 'create' event" );
        }
    }

    /**
     * @param auditable
     * @return
     */
    private boolean isNullOrTransient( final Auditable auditable ) {
        return auditable == null || auditable.getId() == null;
    }

    /**
     * @param d
     */
    private void addDeleteAuditEvent( Auditable d, User user ) {
        assert d != null;
        // what else could we do? But need to keep this record in a good place. See log4j.properties.
        if ( log.isInfoEnabled() ) {
            String un = "";
            if ( user != null ) {
                un = "by " + user.getUserName();
            }
            log.info( "Delete event on entity " + d.getClass().getName() + ":" + d.getId() + "  [" + d + "] " + un );
        }
    }

    /**
     * @param auditable
     */
    private void addUpdateAuditEvent( final Auditable auditable, User user ) {
        assert auditable != null;

        AuditTrail auditTrail = auditable.getAuditTrail();

        ensureInSession( auditTrail );

        if ( auditTrail == null || auditTrail.getEvents().isEmpty() ) {
            /*
             * Note: This can happen for ExperimentalFactors when loading from GEO etc. because of the bidirectional
             * association and the way we persist them. See ExpressionPersister. (actually this seems to be fixed...)
             */
            log.error( "No create event for update method call on " + auditable + ", performing 'create' instead" );
            addCreateAuditEvent( auditable, user, " - Event added on update of existing object." );
        } else {
            String note = "Updated " + auditable.getClass().getSimpleName() + " " + auditable.getId();
            auditHelper.addUpdateAuditEvent( auditable, note, user );
            if ( log.isDebugEnabled() ) {
                log.debug( "Audited event: " + note + " on " + auditable.getClass().getSimpleName() + ":"
                        + auditable.getId() + " by " + user.getUserName() );
            }
        }
    }

    /**
     * @param auditTrail
     */
    private void ensureInSession( AuditTrail auditTrail ) {
        if ( auditTrail == null ) return;
        /*
         * Ensure we have the object in the session. It might not be, if we have flushed the session.
         */
        Session session = sessionFactory.getCurrentSession();
        if ( !session.contains( auditTrail ) ) {
            session.buildLockRequest( LockOptions.NONE ).lock( auditTrail );
        }
    }

    /**
     * @param retValue
     * @param m
     * @param args
     * @return
     */
    private Object getPersistentObject( Object retValue, String methodName, Object[] args ) {
        if ( retValue == null
                && ( CrudUtilsImpl.methodIsDelete( methodName ) || CrudUtilsImpl.methodIsUpdate( methodName ) ) ) {

            // Only deal with single-argument update methods.
            if ( args.length > 1 ) return null;

            assert args.length > 0;
            return args[0];
        }
        return retValue;
    }

    /**
     * Check if the associated object needs to be 'create audited'. Example: gene products are created by cascade when
     * calling update on a gene.
     * 
     * @param object
     * @param auditable
     * @param auditTrail
     */
    private void maybeAddCascadeCreateEvent( Object object, Auditable auditable, User user ) {
        if ( log.isDebugEnabled() )
            log.debug( "Checking for whether to cascade create event from " + auditable + " to " + object );

        // TODO: I don't think we need this.
        if ( !Hibernate.isInitialized( auditable ) ) {
            return;
        }
        if ( auditable.getAuditTrail() == null || auditable.getAuditTrail().getEvents().isEmpty() ) {
            addCreateAuditEvent( auditable, user, " - created by cascade from " + object );
        }
    }

    /**
     * Process auditing on the object.
     * 
     * @param methodName
     * @param object
     */
    private void process( final String methodName, final Auditable a, User user ) {
        if ( log.isTraceEnabled() ) {
            log.trace( "***********  Start Audit of " + methodName + " on " + a + " *************" );
        }
        assert a != null : "Null entity passed to auditing [" + methodName + " on " + a + "]";
        assert a.getId() != null : "Transient instance passed to auditing [" + methodName + " on " + a + "]";

        if ( AUDIT_CREATE && CrudUtilsImpl.methodIsCreate( methodName ) ) {
            addCreateAuditEvent( a, user, "" );
            processAssociations( methodName, a, user );
        } else if ( AUDIT_UPDATE && CrudUtilsImpl.methodIsUpdate( methodName ) ) {
            addUpdateAuditEvent( a, user );

            /*
             * Do not process associations during an update except to add creates to new objects. Otherwise this would
             * result in update events getting added to all child objects, which is silly; and in any case they might be
             * proxies.
             */
            processAssociations( methodName, a, user );
        } else if ( AUDIT_DELETE && CrudUtilsImpl.methodIsDelete( methodName ) ) {
            addDeleteAuditEvent( a, user );
        }

        if ( log.isTraceEnabled() ) log.trace( "============  End Audit ==============" );
    }

    /**
     * Fills in audit trails on newly created child objects after a 'create' or 'update'. It does not add 'update'
     * events on the child objects.
     * <p>
     * Thus if the update is on an expression experiment that has a new Characteristic, the Characteristic will have a
     * 'create' event, and the EEE will get an added update event (via the addUpdateAuditEvent call elsewhere, not here)
     * 
     * @param m
     * @param object
     * @see AclAdvice for similar code for ACLs
     */
    private void processAssociations( String methodName, Object object, User user ) {

        if ( object instanceof AuditTrail ) return; // don't audit audit trails.

        EntityPersister persister = crudUtils.getEntityPersister( object );
        if ( persister == null ) {
            throw new IllegalArgumentException( "No persister found for " + object.getClass().getName() );
        }
        boolean hadErrors = false;
        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
        String[] propertyNames = persister.getPropertyNames();
        try {
            for ( int j = 0; j < propertyNames.length; j++ ) {
                CascadeStyle cs = cascadeStyles[j];

                String propertyName = propertyNames[j];

                if ( !specialCaseForAssociationFollow( object, propertyName )
                        && ( canSkipAssociationCheck( object, propertyName ) || !crudUtils.needCascade( methodName, cs ) ) ) {
                    continue;
                }

                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyName );
                Object associatedObject = ReflectionUtil.getProperty( object, descriptor );

                if ( associatedObject == null ) continue;

                Class<?> propertyType = descriptor.getPropertyType();

                if ( Auditable.class.isAssignableFrom( propertyType ) ) {

                    Auditable auditable = ( Auditable ) associatedObject;
                    try {

                        maybeAddCascadeCreateEvent( object, auditable, user );

                        processAssociations( methodName, auditable, user );
                    } catch ( HibernateException e ) {
                        // If this happens, it means the object can't be 'new' so adding audit trail can't
                        // be necessary.
                        hadErrors = true;
                        if ( log.isDebugEnabled() )
                            log.debug( "Hibernate error while processing " + auditable + ": " + e.getMessage() );
                    }

                } else if ( Collection.class.isAssignableFrom( propertyType ) ) {
                    Collection<?> associatedObjects = ( Collection<?> ) associatedObject;

                    try {
                        Hibernate.initialize( associatedObjects );
                        for ( Object collectionMember : associatedObjects ) {

                            if ( Auditable.class.isAssignableFrom( collectionMember.getClass() ) ) {
                                Auditable auditable = ( Auditable ) collectionMember;
                                try {
                                    Hibernate.initialize( auditable );
                                    maybeAddCascadeCreateEvent( object, auditable, user );
                                    processAssociations( methodName, collectionMember, user );
                                } catch ( HibernateException e ) {
                                    hadErrors = true;
                                    if ( log.isDebugEnabled() )
                                        log.debug( "Hibernate error while processing " + auditable + ": "
                                                + e.getMessage() );
                                    // If this happens, it means the object can't be 'new' so adding audit trail can't
                                    // be necessary. But keep checking.
                                }

                            }
                        }
                    } catch ( HibernateException e ) {
                        hadErrors = true;
                        // If this happens, it means the object can't be 'new' so adding audit trail can't
                        // be necessary.
                        if ( log.isDebugEnabled() )
                            log.debug( "Hibernate error while processing " + object + ": " + e.getMessage() );
                    }

                }
            }
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        }
        if ( hadErrors ) {
            // log.warn( "There were hibernate errors during association checking for " + object
            // + "; probably not critical." );
        }
    }

    /**
     * @param object
     * @param propertyName
     * @return
     */
    private boolean canSkipAssociationCheck( Object object, String propertyName ) {

        /*
         * If this is an expression experiment, don't go down the data vectors.
         */
        if ( ExpressionExperiment.class.isAssignableFrom( object.getClass() )
                && ( propertyName.equals( "rawExpressionDataVectors" ) || propertyName
                        .equals( "processedExpressionDataVectors" ) ) ) {
            log.trace( "Skipping vectors" );
            return true;
        }

        /*
         * Array designs...
         */
        if ( ArrayDesign.class.isAssignableFrom( object.getClass() )
                && ( propertyName.equals( "compositeSequences" ) || propertyName.equals( "reporters" ) ) ) {
            log.trace( "Skipping probes" );
            return true;
        }

        return false;
    }

    /**
     * For cases where don't have a cascade but the other end is auditable.
     * <p>
     * Implementation note. This is kind of inelegant, but the alternative is to check _every_ association, which will
     * often not be reachable.
     * 
     * @param object we are checking
     * @param property of the object
     * @return true if the association should be followed.
     * @see AclAdvice for similar code
     */
    private boolean specialCaseForAssociationFollow( Object object, String property ) {

        if ( BioAssay.class.isAssignableFrom( object.getClass() )
                && ( property.equals( "samplesUsed" ) || property.equals( "arrayDesignUsed" ) ) ) {
            return true;
        } else if ( DesignElementDataVector.class.isAssignableFrom( object.getClass() )
                && property.equals( "bioAssayDimension" ) ) {
            return true;
        }

        return false;
    }

}
