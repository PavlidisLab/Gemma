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
package ubic.gemma.security.interceptor;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.BeanUtils;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailDao;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.CrudUtils;
import ubic.gemma.security.principal.UserDetailsServiceImpl;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.ReflectionUtil;

/**
 * Manage audit trails on objects.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean name="auditAdvice"
 * @spring.property name="userDao" ref="userDao"
 * @spring.property name="crudUtils" ref="crudUtils"
 * @spring.property name="auditTrailDao" ref="auditTrailDao"
 */
public class AuditInterceptor implements MethodInterceptor {

    private static Log log = LogFactory.getLog( AuditInterceptor.class.getName() );

    AuditTrailDao auditTrailDao;

    UserDao userDao;

    CrudUtils crudUtils;

    private boolean AUDIT_CREATE = true;

    private boolean AUDIT_DELETE = true;

    private boolean AUDIT_READ = false;

    private boolean AUDIT_UPDATE = true;

    /**
     * 
     */
    public AuditInterceptor() {
        super();
        this.crudUtils = new CrudUtils();

        try {
            AUDIT_READ = ConfigUtils.getBoolean( "audit.read" );
            AUDIT_CREATE = ConfigUtils.getBoolean( "audit.create" );
            AUDIT_UPDATE = ConfigUtils.getBoolean( "audit.update" );
            AUDIT_DELETE = ConfigUtils.getBoolean( "audit.delete" );
        } catch ( NoSuchElementException e ) {
            log.error( "Configuration error: " + e.getMessage() + "; will use default values" );
        }
    }

    /**
     * @param method
     * @param args
     */
    @SuppressWarnings("unused")
    public void before( Method method, Object[] args, Object target ) {
        Auditable d = null;

        if ( args != null && args.length > 0 ) { // some don't take args, like 'loadAll' methods.

            Object object = args[0];

            if ( object instanceof Collection ) {
                for ( Object object2 : ( Collection<?> ) object ) {
                    if ( object2 instanceof Auditable ) {
                        processBefore( method, ( Auditable ) object2 );
                    }
                }
                return;
            } else if ( !( object instanceof Auditable ) ) {
                return;
            }

            d = ( Auditable ) object;

        }

        processBefore( method, d );

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public Object invoke( MethodInvocation invocation ) throws Throwable {
        Method m = invocation.getMethod();
        Object[] args = invocation.getArguments();
        Object target = invocation.getThis();
        this.before( m, args, target );
        Object returnValue = invocation.proceed();

        this.after( m, returnValue );
        return returnValue;
    }

    /**
     * @param auditTrailDao The auditTrailDao to set.
     */
    public void setAuditTrailDao( AuditTrailDao auditTrailDao ) {
        this.auditTrailDao = auditTrailDao;
    }

    /**
     * @param crudUtils the crudUtils to set
     */
    public void setCrudUtils( CrudUtils crudUtils ) {
        this.crudUtils = crudUtils;
    }

    /**
     * @param userDao The userDao to set.
     */
    public void setUserDao( UserDao userDao ) {
        this.userDao = userDao;
    }

    /**
     * @param d
     * @param noteExtra Additional text to add to the automatically generated note.
     */
    private void addCreateAuditEvent( Auditable d, String noteExtra ) {
        assert d != null;
        if ( log.isDebugEnabled() ) log.debug( "Create audit event for: " + d.getClass() );
        AuditTrail at = null;
        try {
            at = d.getAuditTrail();
            if ( at != null && at.getEvents() != null && at.getEvents().size() > 0 ) {
                // don't add event if it already has one.
                if ( at.getEvents().iterator().next().getAction() == AuditAction.CREATE ) {
                    // This can happen when we persist objects and then let this interceptor look at them again while
                    // persisting parent objects. Harmless.
                    return;
                }
            }
        } catch ( org.hibernate.LazyInitializationException e ) {
            log
                    .warn( "Could not check audit trail for "
                            + d
                            + ", events were not thawed; probably okay because this only happens when the object already exists in the system." );
            return;
        }

        // initialize the audit trail, if necessary.
        if ( at == null ) {
            at = AuditTrail.Factory.newInstance();
        }

        if ( at.getId() == null ) {
            at = auditTrailDao.create( at );
        }

        d.setAuditTrail( at );

        if ( at.getEvents().size() == 0 ) {
            User user = getCurrentUser();
            at.start( getCreateEventNote( d, noteExtra ), user );
            persistAndLogAuditEvent( d, user, at.getLast().getNote() );
        }

    }

    /**
     * @param d
     */
    private void addDeleteAuditEvent( Auditable d ) {
        assert d != null;
        // what else could we do? But need to keep this record in a good place.
        User user = getCurrentUser();
        if ( log.isInfoEnabled() && user != null ) log.info( "Delete event on " + d + " by " + user.getUserName() );
    }

    /**
     * Dealing with load events has to be done as afterAdvice.
     * 
     * @param auditable
     */
    private void addLoadAuditEvent( Auditable auditable ) {
        assert auditable != null;
        AuditTrail at = auditable.getAuditTrail();
        if ( at == null ) {
            log.warn( "No audit trail for update method call" );
            addCreateAuditEvent( auditable, " - Event added after a load on the existing object." );
        } else {
            this.auditTrailDao.thaw( at );
            User user = getCurrentUser();
            at.read( getLoadEventNote( auditable ), user );
            persistAndLogAuditEvent( auditable, user, at.getLast().getNote() );
        }
    }

    /**
     * @param Auditablet
     */
    private void addLoadOrCreateAuditEvent( Auditable auditable ) {
        assert auditable != null;
        if ( AUDIT_READ && auditable.getAuditTrail() != null && auditable.getAuditTrail().getCreationEvent() != null ) {
            log.trace( "FindOrCreate..just load" );
            addLoadAuditEvent( auditable );
        } else if ( AUDIT_CREATE ) {
            log.trace( "FindOrCreate..create on " + auditable );
            addCreateAuditEvent( auditable, " - from findOrCreate" );
        }
    }

    /**
     * @param d
     */
    private void addUpdateAuditEvent( Auditable d ) {
        assert d != null;
        AuditTrail at = d.getAuditTrail();
        assert at != null;
        if ( at.getId() != null ) {
            this.auditTrailDao.thaw( at );
        }

        if ( at.getEvents().size() == 0 ) {
            log.warn( "No create event for update method call on " + d + ", performing 'create'" );
            addCreateAuditEvent( d, " - Event added on update of existing object." );
        } else {
            User user = getCurrentUser();
            at.update( getUpdateEventNote( d ), user );
            persistAndLogAuditEvent( d, user, at.getLast().getNote() );
        }
    }

    /**
     * @param m Method that was invoked to get the returnValue.
     * @param returnValue
     */
    private void after( Method m, Object returnValue ) {

        if ( returnValue == null ) return;

        if ( log.isTraceEnabled() ) {
            if ( returnValue instanceof Collection && ( ( Collection ) returnValue ).size() > 0 ) {
                log.trace( "After: " + m.getName() + " on Collection of "
                        + ( ( Collection ) returnValue ).iterator().next().getClass().getSimpleName() );
            } else {
                log.trace( "After: " + m.getName() + " on " + returnValue );
            }
        }

        if ( Collection.class.isAssignableFrom( returnValue.getClass() ) ) {
            for ( Object object2 : ( Collection<?> ) returnValue ) {
                if ( object2 instanceof Auditable ) processAfter( m, ( Auditable ) object2, new HashSet<Object>() );
            }
        } else if ( !Auditable.class.isAssignableFrom( returnValue.getClass() ) ) {
            return; // no need to look at it!
        } else {
            Auditable d = ( Auditable ) returnValue;
            processAfter( m, d, new HashSet<Object>() );
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
        return UserDetailsServiceImpl.getCurrentUser();
    }

    /**
     * @param auditable
     * @return
     */
    private String getLoadEventNote( Auditable auditable ) {
        return "Loaded " + auditable.getClass().getSimpleName() + " " + auditable.getId();
    }

    /**
     * @param d
     * @return
     */
    private String getUpdateEventNote( Auditable d ) {
        return "Updated " + d.getClass().getSimpleName() + " " + d.getId();
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
            auditTrailDao.update( at );
            if ( log.isTraceEnabled() ) log.trace( note + " event on " + d + " by " + user.getUserName() );
        } else {
            log.info( "NULL user: Cannot update the audit trail with a null user" );
        }
    }

    /**
     * @param m
     * @param d
     */
    private void processAfter( Method m, Auditable returnValue, Collection<Object> visited ) {

        if ( returnValue == null ) return;

        String methodName = m.getName();

        if ( methodName.equals( "findOrCreate" ) ) {
            addLoadOrCreateAuditEvent( returnValue );
        } else if ( AUDIT_READ && CrudUtils.methodIsLoad( m ) ) {
            if ( Collection.class.isAssignableFrom( returnValue.getClass() ) ) {
                for ( Object object : ( Collection<?> ) returnValue ) {
                    if ( !Auditable.class.isAssignableFrom( object.getClass() ) ) {
                        break;
                    }
                    addLoadAuditEvent( returnValue );
                }
            } else if ( Auditable.class.isAssignableFrom( returnValue.getClass() ) ) {
                addLoadAuditEvent( returnValue );
            }
        } else if ( AUDIT_CREATE && CrudUtils.methodIsCreate( m ) ) {
            addCreateAuditEvent( returnValue, "" );
            visited.add( returnValue );
            processAssociationsAfter( m, returnValue, visited );
        }

    }

    /**
     * Given an object, recursively examine its associations for Auditables that need their AuditTrails to be
     * initialized or updated.
     * 
     * @param m
     * @param object
     */
    private void processAssociationsAfter( Method m, Object object, Collection<Object> visited ) {

        if ( object instanceof AuditTrail ) return;
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

                if ( log.isTraceEnabled() )
                    log.trace( "Checking property " + propertyName + " of " + object + " for cascade audit" );

                /*
                 * If the action being taken will result in a hibernate cascade, we need to update the audit information
                 * for the child objects. (This is because this interceptor is only triggered by service actions.)
                 * Otherwise, low-level hibernate activities (like cascading updates) are not seen.
                 */
                if ( !crudUtils.needCascade( m, cs ) ) {
                    continue;
                }

                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyName );
                Object associatedObject = ReflectionUtil.getProperty( object, descriptor );

                if ( associatedObject == null ) continue;

                Class<?> propertyType = descriptor.getPropertyType();

                if ( Auditable.class.isAssignableFrom( propertyType ) ) {

                    // break vicious cycle in bidirectional relation
                    if ( visited.contains( associatedObject ) ) continue;

                    if ( log.isTraceEnabled() )
                        log.trace( "Processing audit for property " + propertyName + ", Cascade=" + cs );
                    processAfter( m, ( Auditable ) associatedObject, visited );
                } else if ( Collection.class.isAssignableFrom( propertyType ) ) {
                    Collection associatedObjects = ( Collection ) associatedObject;

                    try {
                        for ( Object collectionMember : associatedObjects ) {

                            // break vicious cycle in bidirectional relation
                            if ( visited.contains( collectionMember ) ) continue;

                            if ( !Auditable.class.isAssignableFrom( collectionMember.getClass() ) ) {
                                break; // all the collection members are Auditable, or none of them are.
                            }

                            if ( log.isTraceEnabled() ) {
                                log.trace( "Processing audit for member " + collectionMember + " of collection "
                                        + propertyName + ", Cascade=" + cs );
                            }
                            processAfter( m, ( Auditable ) collectionMember, visited );

                        }
                    } catch ( org.hibernate.LazyInitializationException e ) {
                        // This is almost always not a problem, as it means the collection is already in the system. But
                        // it can indicate a problem in the thaw state of the audited object.
                        log.warn( "Collection " + propertyName + " on " + object
                                + " could not be initialized, it will not be audited" );
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
     * Fills in audit trails on newly created child objects after an 'update'.
     * <p>
     * FIXME this only goes down one level, so if multiple levels of children are created by the cascade they will not
     * be given audit trails.
     * <p>
     * FIXME this does not add update events to all child auditable objects. It is debatable whether it should (probably
     * the answer is yes).
     * 
     * @param m
     * @param object
     */
    private void processAssociationsBefore( Method m, Object object ) {

        if ( object instanceof AuditTrail ) return;
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

                if ( log.isTraceEnabled() )
                    log.trace( "Checking property " + propertyName + " of " + object + " for cascade audit" );

                /*
                 * If the action being taken will result in a hibernate cascade, we need to update the audit information
                 * for the child objects. (This is because this interceptor is only triggered by service actions.)
                 * Otherwise, low-level hibernate activities (like cascading updates) are not seen.
                 */
                if ( !crudUtils.needCascade( m, cs ) ) {
                    continue;
                }

                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyName );
                Object associatedObject = ReflectionUtil.getProperty( object, descriptor );

                if ( associatedObject == null ) continue;

                Class<?> propertyType = descriptor.getPropertyType();

                if ( Auditable.class.isAssignableFrom( propertyType ) ) {

                    if ( log.isTraceEnabled() )
                        log.trace( "Processing audit for property " + propertyName + ", Cascade=" + cs );

                    if ( ( ( Auditable ) associatedObject ).getAuditTrail() == null ) {
                        addCreateAuditEvent( ( Auditable ) associatedObject, " - entity created by cascade from "
                                + object );
                    }

                } else if ( Collection.class.isAssignableFrom( propertyType ) ) {
                    Collection associatedObjects = ( Collection ) associatedObject;
                    if ( !Hibernate.isInitialized( associatedObjects ) ) {
                        // assume it has not changed.
                        continue;
                    }
                    try {
                        for ( Object collectionMember : associatedObjects ) {

                            if ( !Auditable.class.isAssignableFrom( collectionMember.getClass() ) ) {
                                break; // all the collection members are Auditable, or none of them are.
                            }

                            if ( log.isTraceEnabled() ) {
                                log.trace( "Processing audit for member " + collectionMember + " of collection "
                                        + propertyName + ", Cascade=" + cs );
                            }
                            if ( ( ( Auditable ) collectionMember ).getAuditTrail() == null ) {
                                addCreateAuditEvent( ( Auditable ) collectionMember,
                                        " - entity created by cascade from " + object );
                            }
                        }
                    } catch ( org.hibernate.LazyInitializationException e ) {
                        // This is almost always not a problem, as it means the collection is already in the system. But
                        // it can indicate a problem in the thaw state of the audited object.
                        log.warn( "Collection " + propertyName + " on " + object
                                + " could not be initialized, it will not be audited" );
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
     * @param method
     * @param d
     */
    private void processBefore( Method method, Auditable d ) {

        /*
         * Add the audit trail if the object doesn't already have one.
         */
        if ( d != null && d.getAuditTrail() == null ) {
            AuditTrail at = AuditTrail.Factory.newInstance();
            d.setAuditTrail( at );
            if ( log.isDebugEnabled() ) log.debug( "Added auditTrail to " + d );
        }

        if ( method.getName().equals( "create" ) ) {
            // Defer until afterwards.
        } else if ( AUDIT_UPDATE && CrudUtils.methodIsUpdate( method ) ) {
            addUpdateAuditEvent( d ); // no return value so we have to do it before.
            // In case of a cascade that creates new objects. Make sure they get audit trails
            processAssociationsBefore( method, d );
        } else if ( CrudUtils.methodIsLoad( method ) ) {
            // Defer until afterwards.
        } else if ( method.getName().startsWith( "find" ) ) {
            // Defer until afterwards.
        } else if ( AUDIT_DELETE && CrudUtils.methodIsDelete( method ) ) {
            addDeleteAuditEvent( d );
        } else {
            throw new IllegalArgumentException( "Shouldn't be getting method " + method );
        }

    }

}
