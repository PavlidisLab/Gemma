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
package edu.columbia.gemma.security.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.MethodBeforeAdvice;

import edu.columbia.gemma.common.Describable;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrailDao;
import edu.columbia.gemma.common.auditAndSecurity.User;
import edu.columbia.gemma.common.auditAndSecurity.UserDao;

/**
 * Manage audit trails on objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AuditInterceptor implements MethodInterceptor {

    private static Log log = LogFactory.getLog( AuditInterceptor.class.getName() );

    AuditTrailDao auditTrailDao;

    UserDao userDao;

    /**
     * @param auditTrailDao The auditTrailDao to set.
     */
    public void setAuditTrailDao( AuditTrailDao auditTrailDao ) {
        this.auditTrailDao = auditTrailDao;
    }

    /**
     * @param method
     * @return
     */
    private boolean methodRequiresAuditEntry( Method method ) {
        String name = method.getName();
        return ( name.equals( "findOrCreate" ) || name.equals( "create" ) || name.equals( "save" )
                || name.equals( "update" ) || name.equals( "load" ) || name.equals( "read" ) || name.equals( "delete" ) || name
                .equals( "remove" ) );
    }

    /**
     * @param method
     * @param args
     */
    public void before( Method method, Object[] args, Object target ) {
        // if the method is 'update', 'create' or 'read' (?) and the object is 'securable', we need to add to the audit
        // trail.

        if ( !methodRequiresAuditEntry( method ) ) {
            return;
        }

        if ( args.length > 1 || args[0] == null ) {
            return;
        }

        Object object = args[0];

        if ( Collection.class.isAssignableFrom( object.getClass() ) ) {
            for ( Object object2 : ( Collection<?> ) object ) {
                processBefore( method, ( Describable ) object2 );
            }
        } else if ( !Describable.class.isAssignableFrom( object.getClass() ) ) {
            return;
        }

        Describable d = ( Describable ) object;

        processBefore( method, d );

    }

    /**
     * @param method
     * @param d
     */
    private void processBefore( Method method, Describable d ) {
        if ( method.getName().equals( "create" ) ) {
            // defer until afterwards
        } else if ( method.getName().equals( "update" ) ) {
            addUpdateAuditEvent( d );
        } else if ( method.getName().equals( "read" ) || method.getName().equals( "load" ) ) {
            // wait until after
        } else if ( method.getName().startsWith( "find" ) ) {
            // Defer until afterwards.
        } else {
            addDeleteAuditEvent( d );
        }
    }

    /**
     * @param d
     */
    private void addDeleteAuditEvent( Describable d ) {
        // what else could we do? But need to keep this record in a good place.
        User user = getCurrentUser();
        log.info( "Delete event on " + d + " by " + user.getUserName() );
    }

    /**
     * @return
     */
    private User getCurrentUser() {
        String userName = AddOrRemoveFromACLInterceptor.getUsername();
        User user = userDao.findByUserName( userName );
        return user;
    }

    /**
     * @param d
     */
    private void addUpdateAuditEvent( Describable d ) {
        AuditTrail at = d.getAuditTrail();
        if ( at == null ) {
            log.warn( "No audit trail for update method call" );
            at = AuditTrail.Factory.newInstance();
            at.start();
            d.setAuditTrail( at );
            d.setAuditTrail( at );
            addCreateAuditEvent( d );
        } else {
            User user = getCurrentUser();
            at.update( "Updated", user );
            log.info( "Update event on " + d + " by " + user.getUserName() );
        }

    }

    /**
     * @param d
     * @param user
     */
    private void addCreateAuditEvent( Describable d ) {
        AuditTrail at = d.getAuditTrail();
        if ( at == null ) throw new IllegalStateException( "Describable " + d + " had no audit trail" );
        User user = getCurrentUser();
        at.start( "start", user );
        at = auditTrailDao.create( at );
        log.info( "Create event on " + d + " by " + user.getUserName() );
    }

    /**
     * @param userDao The userDao to set.
     */
    public void setUserDao( UserDao userDao ) {
        this.userDao = userDao;
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

        this.after( m, returnValue, target );
        return returnValue;
    }

    /**
     * @param describable
     */
    private void addLoadOrCreateAuditEvent( Describable describable, Object target ) {
        if ( describable.getAuditTrail() != null && describable.getAuditTrail().getCreationEvent() != null ) {
            addLoadAuditEvent( describable );
        } else {
            addCreateAuditEvent( describable );
            // // the target should be a service that hopefully has a update method.
            // try {
            // Method updater = target.getClass().getMethod( "update", new Class[] { Describable.class } );
            // updater.invoke( target, new Object[] { describable } );
            // } catch ( SecurityException e ) {
            // throw new RuntimeException( e );
            // } catch ( NoSuchMethodException e ) {
            // log.error( "No 'update' method available for " + target.getClass().getName()
            // + ", audit trail may not get updated" );
            // } catch ( IllegalArgumentException e ) {
            // throw new RuntimeException( e );
            // } catch ( IllegalAccessException e ) {
            // throw new RuntimeException( e );
            // } catch ( InvocationTargetException e ) {
            // throw new RuntimeException( e );
            // }
        }

    }

    /**
     * @param describable
     */
    private void addLoadAuditEvent( Describable describable ) {
        AuditTrail at = describable.getAuditTrail();
        if ( at == null ) {
            log.warn( "No audit trail for update method call" );
            at = AuditTrail.Factory.newInstance();
            describable.setAuditTrail( at );
            addCreateAuditEvent( describable );
        } else {

            User user = getCurrentUser();
            at.read( "Loaded", user );
            log.info( "Read event on " + describable + " by " + user.getUserName() );
        }

    }

    /**
     * @param m Method that was invoked to get the returnValue.
     * @param returnValue
     */
    private void after( Method m, Object returnValue, Object target ) {
        String methodName = m.getName();

        if ( methodName.equals( "findOrCreate" ) ) {
            addLoadOrCreateAuditEvent( ( Describable ) returnValue, target );
        } else if ( methodName.startsWith( "find" ) || methodName.equals( "load" ) || methodName.equals( "read" ) ) {
            if ( returnValue != null ) {
                if ( Collection.class.isAssignableFrom( returnValue.getClass() ) ) {
                    for ( Object object : ( Collection<?> ) returnValue ) {
                        if ( !Describable.class.isAssignableFrom( object.getClass() ) ) {
                            break;
                        }
                        addLoadAuditEvent( ( Describable ) returnValue );
                    }
                } else if ( Describable.class.isAssignableFrom( returnValue.getClass() ) ) {
                    addLoadAuditEvent( ( Describable ) returnValue );
                }
            }

        } else if ( methodName.equals( "create" ) ) {
            addCreateAuditEvent( ( Describable ) returnValue );
        }
    }
}
