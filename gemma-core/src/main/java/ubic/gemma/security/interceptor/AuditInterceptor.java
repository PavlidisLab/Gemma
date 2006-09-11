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
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private boolean AUDIT_CREATE = true;

    private boolean AUDIT_DELETE = true;

    private boolean AUDIT_READ = false;

    private boolean AUDIT_UPDATE = true;

    /**
     * Cache of users. FIXME this is too primitive.
     */
    private Map<String, User> currentUsers = new HashMap<String, User>();

    AuditTrailDao auditTrailDao;

    UserDao userDao;

    CrudUtils crudUtils;

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

        if ( args != null ) { // some don't take args, like 'loadAll' methods.

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
     * @param userDao The userDao to set.
     */
    public void setUserDao( UserDao userDao ) {
        this.userDao = userDao;
    }

    /**
     * @param d
     * @param user
     */
    private void addCreateAuditEvent( Auditable d ) {
        assert d != null;
        AuditTrail at = d.getAuditTrail();

        if ( at != null && at.getEvents().size() == 1
                && at.getEvents().iterator().next().getAction() == AuditAction.CREATE ) {
            log.warn( "Expected empty or null audit trail for creating object, but got "

            + at.getEvents().size() + " events for " + d + ", first one was "
                    + at.getEvents().iterator().next().getAction() );
            return;
        }

        // initialize the audit trail, if necessary.
        if ( at == null ) {
            at = AuditTrail.Factory.newInstance();
            at = auditTrailDao.create( at );
            d.setAuditTrail( at );
        }

        if ( at.getEvents().size() == 0 ) {
            User user = getCurrentUser();
            at.start( "create " + d, user );
            auditTrailDao.update( at );
            if ( log.isTraceEnabled() ) log.trace( "Create event on " + d + " by " + user.getUserName() );
        }
    }

    /**
     * @param d
     */
    private void addDeleteAuditEvent( Auditable d ) {
        assert d != null;
        // what else could we do? But need to keep this record in a good place.
        User user = getCurrentUser();
        if ( log.isInfoEnabled() ) log.info( "Delete event on " + d + " by " + user.getUserName() );
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
            addCreateAuditEvent( auditable );
        } else {
            User user = getCurrentUser();
            at.read( "Loaded", user );
            auditTrailDao.update( at );
            if ( log.isTraceEnabled() ) log.trace( "Read event on " + auditable + " by " + user.getUserName() );
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
            addCreateAuditEvent( auditable );
        }
    }

    /**
     * @param d
     */
    private void addUpdateAuditEvent( Auditable d ) {
        assert d != null;
        AuditTrail at = d.getAuditTrail();
        if ( at == null || at.getEvents().size() == 0 ) {
            log.warn( "No audit trail for update method call, performing 'create'" );
            addCreateAuditEvent( d );
        } else {
            User user = getCurrentUser();
            at.update( "Updated" + d, user );
            auditTrailDao.update( at );
            if ( log.isTraceEnabled() ) log.trace( "Update event on " + d + " by " + user.getUserName() );
        }

    }

    /**
     * @param m Method that was invoked to get the returnValue.
     * @param returnValue
     */
    private void after( Method m, Object returnValue ) {

        if ( returnValue == null ) return;

        log.debug( "After: " + m.getName() + " on " + returnValue );
        if ( Collection.class.isAssignableFrom( returnValue.getClass() ) ) {
            for ( Object object2 : ( Collection<?> ) returnValue ) {
                processAfter( m, ( Auditable ) object2 );
            }
        } else if ( !Auditable.class.isAssignableFrom( returnValue.getClass() ) ) {
            return; // no need to look at it!
        } else {
            Auditable d = ( Auditable ) returnValue;
            processAfter( m, d );
        }

    }

    /**
     * @return
     */
    private User getCurrentUser() {
        String userName = UserDetailsServiceImpl.getCurrentUsername();
        assert userName != null;
        if ( currentUsers.get( userName ) == null ) {
            assert userDao != null;
            currentUsers.put( userName, userDao.findByUserName( userName ) );
        }
        return currentUsers.get( userName );
    }

    /**
     * @param m
     * @param d
     */
    private void processAfter( Method m, Auditable returnValue ) {

        if ( returnValue == null ) return;

        String methodName = m.getName();

        if ( methodName.equals( "findOrCreate" ) ) {
            addLoadOrCreateAuditEvent( returnValue );
        } else if ( AUDIT_READ && CrudUtils.methodIsLoad( m ) ) {
            if ( returnValue != null ) {
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
            }

        } else if ( AUDIT_CREATE && CrudUtils.methodIsCreate( m ) ) {
            addCreateAuditEvent( returnValue );
        }
        processAssociations( m, returnValue );

    }

    /**
     * @param m
     * @param object
     */
    private void processAssociations( Method m, Object object ) {

        EntityPersister persister = crudUtils.getEntityPersister( object );
        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
        String[] propertyNames = persister.getPropertyNames();
        try {
            for ( int j = 0; j < propertyNames.length; j++ ) {
                CascadeStyle cs = cascadeStyles[j];

                if ( log.isTraceEnabled() ) log.trace( "Checking " + propertyNames[j] + " for cascade audit" );

                /*
                 * If the action being taken will result in a hibernate cascade, we need to update the audit information
                 * for the child objects. This is because this interceptor is expected to be triggered by service
                 * actions. Low-level hibernate activities (like cascading updates) are not seen.
                 */
                if ( !crudUtils.needCascade( m, cs ) ) {
                    if ( log.isTraceEnabled() )
                        log.trace( "Not processing association " + propertyNames[j] + ", Cascade=" + cs );
                    continue;
                }

                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyNames[j] );
                Object associatedObject = ReflectionUtil.getProperty( object, descriptor );

                if ( associatedObject == null ) continue;

                Class<?> propertyType = descriptor.getPropertyType();

                if ( Auditable.class.isAssignableFrom( propertyType ) ) {

                    if ( log.isTraceEnabled() )
                        log.trace( "Processing audit for " + propertyNames[j] + ", Cascade=" + cs );
                    processAfter( m, ( Auditable ) associatedObject );
                } else if ( Collection.class.isAssignableFrom( propertyType ) ) {
                    Collection associatedObjects = ( Collection ) associatedObject;

                    for ( Object object2 : associatedObjects ) {
                        if ( Auditable.class.isAssignableFrom( object2.getClass() ) ) {
                            if ( log.isTraceEnabled() ) {
                                log.trace( "Processing audit for member " + object2 + " of collection "
                                        + propertyNames[j] + ", Cascade=" + cs );
                            }
                            processAfter( m, ( Auditable ) object2 );
                        }
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

        // saves us the trouble...
        if ( d != null && d.getAuditTrail() == null ) {
            AuditTrail at = AuditTrail.Factory.newInstance();
            d.setAuditTrail( at );
            if ( log.isDebugEnabled() ) log.debug( "Added auditTrail to " + d );
        }

        if ( method.getName().equals( "create" ) ) {
            // Defer until afterwards.
        } else if ( AUDIT_UPDATE && CrudUtils.methodIsUpdate( method ) ) {
            addUpdateAuditEvent( d ); // no return value so we have to do it before.
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

    /**
     * @param crudUtils the crudUtils to set
     */
    public void setCrudUtils( CrudUtils crudUtils ) {
        this.crudUtils = crudUtils;
    }

}
