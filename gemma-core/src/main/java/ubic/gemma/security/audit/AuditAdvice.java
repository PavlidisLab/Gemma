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
import java.util.List;
import java.util.NoSuchElementException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.persister.entity.EntityPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailDao;
import ubic.gemma.model.common.auditAndSecurity.StatusDao;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserDao;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.CrudUtils;
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
@Repository
public class AuditAdvice extends HibernateDaoSupport {

    /*
     * Note that we have a special logger configured for this class, so delete events get stored.
     */
    private static Logger log = LoggerFactory.getLogger( AuditAdvice.class.getName() );

    @Autowired
    AuditTrailDao auditTrailDao;

    @Autowired
    CrudUtils crudUtils;

    @Autowired
    UserDao userDao;

    @Autowired
    UserManager userManager;

    @Autowired
    StatusDao statusDao;

    private boolean AUDIT_CREATE = true;

    private boolean AUDIT_DELETE = true;

    private boolean AUDIT_UPDATE = true;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public AuditAdvice( SessionFactory sessionFactory, PlatformTransactionManager transactionManager ) {

        try {

            AUDIT_UPDATE = ConfigUtils.getBoolean( "audit.update" );
            AUDIT_DELETE = ConfigUtils.getBoolean( "audit.delete" );
            AUDIT_CREATE = ConfigUtils.getBoolean( "audit.create" ) || AUDIT_UPDATE;
        } catch ( NoSuchElementException e ) {
            log.error( "Configuration error: " + e.getMessage() + "; will use default values" );
        }
        transactionTemplate = new TransactionTemplate( transactionManager );
        super.setSessionFactory( sessionFactory );
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

        if ( object instanceof Collection ) {
            for ( final Object o : ( Collection<?> ) object ) {
                if ( Auditable.class.isAssignableFrom( o.getClass() ) ) {
                    process( methodName, ( Auditable ) o );
                }
            }
        } else if ( ( Auditable.class.isAssignableFrom( object.getClass() ) ) ) {
            process( methodName, ( Auditable ) object );
        }
    }

    /**
     * @param a
     */
    private AuditTrail addAuditTrailIfNeeded( Auditable a ) {
        if ( a.getAuditTrail() == null ) {

            try {

                a.setAuditTrail( auditTrailDao.create( AuditTrail.Factory.newInstance() ) );
                /*
                 * Critical to get trail associated in the store.
                 */
                this.getSession().update( a );

            } catch ( Exception e ) {

                /*
                 * This can happen if we hit an auditable during a read-only event: programming error.
                 */
                throw new IllegalStateException( "Invalid attempt to create an audit trail on" + a, e );
            }

        }

        return a.getAuditTrail();
    }

    /**
     * @param d
     * @param noteExtra Additional text to add to the automatically generated note.
     */
    private void addCreateAuditEvent( final Auditable d, String noteExtra ) {

        if ( d == null || d.getId() == null ) return;

        AuditTrail at = addAuditTrailIfNeeded( d );
        addStatusIfNeeded( d );

        assert at != null;

        if ( at.getEvents().size() > 0 ) {
            // findOrCreate, or this can happen when we persist objects and then let this interceptor look at them again
            // while persisting parent objects. Harmless. But make sure the first event is set as a 'create'.

            if ( at.getEvents() instanceof List ) { // should be!
                AuditEvent ae = ( ( List<AuditEvent> ) at.getEvents() ).get( 0 );
                if ( !ae.getAction().equals( AuditAction.CREATE ) ) {
                    log.info( "First event was not 'create', fixing it: " + d );
                    ae.setAction( AuditAction.CREATE );
                    auditTrailDao.update( at );
                }
            }

            return;
        }

        assert at.getEvents().size() == 0;

        try {
            User user = getCurrentUser();
            at.start( getCreateEventNote( d, noteExtra ), user );
            persistAndLogAuditEvent( d, user, at.getLast().getNote() );
        } catch ( UsernameNotFoundException e ) {
            log.warn( "No user, cannot add 'create' event" );
        }

    }

    private void addStatusIfNeeded( Auditable d ) {
        if ( d.getStatus() == null ) {
            if ( log.isDebugEnabled() ) log.debug( "Adding status" );
            statusDao.initializeStatus( d );
            assert d.getStatus() != null && d.getStatus().getId() != null;
        }

    }

    /**
     * @param d
     */
    private void addDeleteAuditEvent( Auditable d ) {
        assert d != null;
        // what else could we do? But need to keep this record in a good place. See log4j.properties.
        User user = getCurrentUser();
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
    private void addUpdateAuditEvent( final Auditable auditable ) {
        assert auditable != null;

        AuditTrail at = addAuditTrailIfNeeded( auditable );
        if ( at.getEvents().size() == 0 ) {
            /*
             * Note: This can happen for ExperimentalFactors when loading from GEO etc. because of the bidirectional
             * association and the way we persist them. See ExpressionPersister. (actually this seems to be fixed...)
             */
            log.warn( "No create event for update method call on " + auditable + ", performing 'create' instead" );
            addCreateAuditEvent( auditable, " - Event added on update of existing object." );
        } else {
            /*
             * FIXME only save an update if the event has a note or is otherwise 'distinctive' ...
             */

            User user = getCurrentUser();
            at.update( getUpdateEventNote( auditable ), user );
            persistAndLogAuditEvent( auditable, user, at.getLast().getNote() );
            updateStatus( auditable );
        }
    }

    /**
     * @param d
     * @return
     */
    private String getCreateEventNote( Auditable d, String extra ) {
        return "Create " + d.getClass().getSimpleName() + " " + d.getId() + extra;
    }

    /**
     * @return
     */
    private User getCurrentUser() {
        try {
            return userManager.getCurrentUser();
        } catch ( UsernameNotFoundException e ) {
            /* probably anonymous */
            return null;
        }
    }

    /**
     * @param retValue
     * @param m
     * @param args
     * @return
     */
    private Object getPersistentObject( Object retValue, String methodName, Object[] args ) {
        if ( CrudUtils.methodIsDelete( methodName ) || CrudUtils.methodIsUpdate( methodName ) ) {

            /*
             * Only deal with single-argument update methods.
             */
            if ( args.length > 1 ) return null;

            assert args.length > 0;
            return args[0];
        }
        return retValue;
    }

    /**
     * @param d
     * @return
     */
    private String getUpdateEventNote( Auditable d ) {

        // TODO: perhaps we can put more information here.
        // String fullStackTrace = ExceptionUtils.getFullStackTrace( new Exception() );

        return "Updated " + d.getClass().getSimpleName() + " " + d.getId();

    }

    /**
     * @param object
     * @param auditable
     * @param at
     */
    private void maybeAddCascadeCreateEvent( Object object, Auditable auditable, AuditTrail at ) {

        if ( !Hibernate.isInitialized( at ) ) {
            return;
        }

        if ( at.getEvents().size() == 0 ) {
            addCreateAuditEvent( auditable, " - created by cascade from " + object );
        }
    }

    /**
     * Updates and logs the audit trail provided certain conditions are met (ie. user is not null).
     * 
     * @param at
     */
    private void persistAndLogAuditEvent( Auditable d, User user, String note ) {
        if ( user != null ) {
            AuditTrail at = d.getAuditTrail();
            assert at != null;
            if ( at.getEvents().size() == 0 ) {
                log.warn( "No events!" );
                return;
            } else if ( at.getEvents().size() == 1 ) {
                // making sure this gets initialized correctly.
                at.getEvents().iterator().next().setAction( AuditAction.CREATE );
            }
            auditTrailDao.update( at );
            if ( log.isDebugEnabled() )
                log.debug( "Audited event: " + note + " on " + d.getClass().getSimpleName() + ":" + d.getId() + " by "
                        + user.getUserName() );
        } else {
            log.info( "NULL user: Cannot update the audit trail with a null user" );
        }
    }

    private void updateStatus( Auditable d ) {
        statusDao.update( d );
    }

    /**
     * Process auditing on the object.
     * 
     * @param methodName
     * @param object
     */
    private void process( final String methodName, final Auditable a ) {
        if ( log.isTraceEnabled() )
            log.trace( "***********  Start Audit of " + methodName + " on " + a + " *************" );

        transactionTemplate.execute( new TransactionCallbackWithoutResult() {
            @SuppressWarnings("synthetic-access")
            @Override
            protected void doInTransactionWithoutResult( TransactionStatus status ) {

                assert a != null;
                Session session = getSessionFactory().getCurrentSession();

                if ( !CrudUtils.methodIsDelete( methodName ) ) {
                    session.lock( a, LockMode.NONE );
                }

                Hibernate.initialize( a );

                if ( AUDIT_CREATE && CrudUtils.methodIsCreate( methodName ) ) {
                    addCreateAuditEvent( a, "" );
                    processAssociations( methodName, a );
                } else if ( AUDIT_UPDATE && CrudUtils.methodIsUpdate( methodName ) ) {
                    addUpdateAuditEvent( a );

                    /*
                     * Do not process associations during an update except to add creates to new objects. Otherwise this
                     * would result in update events getting added to all child objects, which is silly; and in any case
                     * they might be proxies.
                     */
                    processAssociations( methodName, a );
                } else if ( AUDIT_DELETE && CrudUtils.methodIsDelete( methodName ) ) {
                    addDeleteAuditEvent( a );
                }

            }

        } );

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
    private void processAssociations( String methodName, Object object ) {

        if ( object instanceof AuditTrail ) return; // don't audit audit trails.

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

                if ( ( canSkipAssociationCheck( object, propertyName ) || !crudUtils.needCascade( methodName, cs ) )
                        && !specialCaseForAssociationFollow( object, propertyName ) ) {
                    continue;
                }

                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyName );
                Object associatedObject = ReflectionUtil.getProperty( object, descriptor );

                if ( associatedObject == null ) continue;

                Class<?> propertyType = descriptor.getPropertyType();

                if ( Auditable.class.isAssignableFrom( propertyType ) ) {

                    Auditable auditable = ( Auditable ) associatedObject;
                    try {
                        this.getSession().lock( auditable, LockMode.NONE );
                        Hibernate.initialize( auditable );

                        AuditTrail at = this.addAuditTrailIfNeeded( auditable );

                        maybeAddCascadeCreateEvent( object, auditable, at );

                        processAssociations( methodName, auditable );
                    } catch ( HibernateException e ) {
                        // If this happens, it means the object can't be 'new' so adding audit trail can't
                        // be necessary.
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
                                    AuditTrail at = this.addAuditTrailIfNeeded( auditable );
                                    maybeAddCascadeCreateEvent( object, auditable, at );
                                    processAssociations( methodName, collectionMember );
                                } catch ( HibernateException e ) {
                                    // If this happens, it means the object can't be 'new' so adding audit trail can't
                                    // be necessary. But keep checking.
                                }

                            }
                        }
                    } catch ( HibernateException e ) {
                        // If this happens, it means the object can't be 'new' so adding audit trail can't
                        // be necessary.
                    }

                }
            }
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
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

        if ( BioAssay.class.isAssignableFrom( object.getClass() ) && property.equals( "samplesUsed" ) ) {
            return true;
        } else if ( DesignElementDataVector.class.isAssignableFrom( object.getClass() )
                && property.equals( "bioAssayDimension" ) ) {
            return true;
        }

        return false;

    }

}
