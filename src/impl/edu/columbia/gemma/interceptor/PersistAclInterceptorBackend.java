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
import org.springframework.dao.DataAccessException;

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
     * @param retValue
     * @param m
     * @param args - the argument to the method that is invoked.
     * @param target
     * @throws Throwable
     */
    public void afterReturning( Object retValue, Method m, Object[] args, Object target ) throws Throwable {

        Object object = null;
        log.info( "Before: method=[" + m + "]" );

        if ( m.getName().contains( "save" ) || m.getName().contains( "remove" ) ) {

            object = args[0];

            String fullyQualifiedName = object.getClass().getName();
            log.info( "The object is: " + fullyQualifiedName );

            if ( m.getName().startsWith( "save" ) )
                addPermission( object, getUsername(), getAuthority() );

            else if ( m.getName().startsWith( "remove" ) ) deletePermission( object, getUsername() );

        }

    }

    /**
     * Creates the acl_permission object and the acl_object_identity object.
     * 
     * @param object - represents the domain object.
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
     * Delete object and acl permissions
     * 
     * @param object
     * @param recipient
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws DataAccessException
     */
    public void deletePermission( Object object, String recipient ) throws DataAccessException,
            IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        basicAclExtendedDao.delete( makeObjectIdentity( object ), recipient );

        if ( log.isDebugEnabled() ) {
            log.debug( "Deleted object " + object + " ACL permissions for recipient " + recipient );
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
        }
        return auth.getPrincipal().toString();

    }

    /**
     * For the current principal, return the permissions mask. If the current principal has role "admin", they are
     * granted ADMINISTRATION authority. If they are role "user", they are granted READ_WRITE authority.
     * 
     * @return
     */
    protected Integer getAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        GrantedAuthority[] ga = auth.getAuthorities();
        for ( int i = 0; i < ga.length; i++ ) {
            if ( ga[i].equals( "admin" ) ) {
                log.debug("Granting ADMINISTRATION privileges");
                return new Integer( SimpleAclEntry.ADMINISTRATION );
            }
        }
        log.debug("Granting READ_WRITE privileges");
        return new Integer( SimpleAclEntry.READ_WRITE );

    }

    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

    public BasicAclExtendedDao getBasicAclExtendedDao() {
        return basicAclExtendedDao;
    }

}
