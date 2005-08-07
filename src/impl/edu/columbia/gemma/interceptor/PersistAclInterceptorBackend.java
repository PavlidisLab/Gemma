package edu.columbia.gemma.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.acegisecurity.Authentication;
import net.sf.acegisecurity.GrantedAuthority;
import net.sf.acegisecurity.UserDetails;
import net.sf.acegisecurity.acl.basic.AclObjectIdentity;
import net.sf.acegisecurity.acl.basic.BasicAclExtendedDao;
import net.sf.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import net.sf.acegisecurity.acl.basic.SimpleAclEntry;
import net.sf.acegisecurity.context.SecurityContextHolder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AfterReturningAdvice;

/**
 * This is an 'AfterAdvice' implementation of the 'AroundAdvice' PersistAclInterceptor
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptorBackend implements AfterReturningAdvice {
    private static Log log = LogFactory.getLog( PersistAclInterceptorBackend.class.getName() );

    private BasicAclExtendedDao basicAclExtendedDao;

    /**
     * Must implement this method if this class is going to be used as an interceptor
     * 
     * @param invocation
     * @return
     * @throws Throwable
     */
    public void afterReturning( Object retValue, Method m, Object[] args, Object target ) throws Throwable {

        if ( isValidMethodToIntercept( m ) ) {
            Object object = null;
            log.info( "Before: method=[" + m + "]" );

            log.info( "The method is: " + m.getName() );

            Object[] arguments = args;
            for ( Object obj : arguments ) {
                object = obj;
            }

            String fullyQualifiedName = object.getClass().getName();
            log.info( "The object is: " + fullyQualifiedName );

            addPermission( object, getUsername(), getAuthority() );

        }

    }

    /**
     * Test to see if this is a valid method to intercept. This will be fleshed out to add the actual methods intercept
     * to the xml configuration file.
     * 
     * @param m
     * @return
     */
    private boolean isValidMethodToIntercept( Method m ) {
        if ( m.getName().contains( "save" ) ) return true;

        return false;

    }
    
    /**
     * Creates the acl_permission object and the acl_object_identity object.
     * 
     * @param object
     * @param recipient
     * @param permission
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void addPermission( Object object, String recipient, Integer permission ) throws NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        SimpleAclEntry simpleAclEntry = new SimpleAclEntry();
        simpleAclEntry.setAclObjectIdentity( makeObjectIdentity( object ) );
        simpleAclEntry.setMask( permission.intValue() );
        simpleAclEntry.setRecipient( recipient );

        simpleAclEntry.setAclObjectParentIdentity( new NamedEntityObjectIdentity( "dummy", "1" ) );

        basicAclExtendedDao.create( simpleAclEntry );

        if ( log.isDebugEnabled() ) {
            log.debug( "Added permission " + permission + " for recipient " + recipient + " on " + object );
        }
    }

    /**
     * Forms the object identity to be inserted in acl_object_identity table.
     * 
     * @param object
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    private AclObjectIdentity makeObjectIdentity( Object object ) throws NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        log.info( object );

        return new NamedEntityObjectIdentity( object );
    }

    protected String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if ( auth.getPrincipal() instanceof UserDetails ) {
            return ( ( UserDetails ) auth.getPrincipal() ).getUsername();
        } else {
            return auth.getPrincipal().toString();
        }
    }

    protected Integer getAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if ( auth.getPrincipal() instanceof UserDetails ) {
            GrantedAuthority[] ga = auth.getAuthorities();
            for ( int i = 0; i < ga.length; i++ ) {
                if ( ga[i].equals( "admin" ) )
                    return new Integer( SimpleAclEntry.ADMINISTRATION );
                else if ( ga[i].equals( "user" ) ) return new Integer( SimpleAclEntry.READ );
            }
            return new Integer( SimpleAclEntry.READ );
            // return ga[i].getAuthority();
        } else {
            return new Integer( SimpleAclEntry.ADMINISTRATION );
        }

    }

    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

    public BasicAclExtendedDao getBasicAclExtendedDao() {
        return basicAclExtendedDao;
    }

}
