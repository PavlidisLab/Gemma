/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
 * Adds security controls to newly created objects, and removes them for objects that are deleted.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class AddOrRemoveFromACLInterceptor implements AfterReturningAdvice {
    private static Log log = LogFactory.getLog( AddOrRemoveFromACLInterceptor.class.getName() );

    private BasicAclExtendedDao basicAclExtendedDao;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.AfterReturningAdvice#afterReturning(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[], java.lang.Object)
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public void afterReturning( Object retValue, Method m, Object[] args, Object target ) throws Throwable {

        Object object = null;
        if ( log.isDebugEnabled() ) log.debug( "Before: method=[" + m + "], Target: " + target );

        if ( m.getName().equals( "findOrCreate" ) || m.getName().equals( "remove" ) ) {

            object = args[0];

            if ( Collection.class.isAssignableFrom( object.getClass() ) ) {
                for ( Object o : ( Collection<Object> ) object ) {
                    processPermissions( m, o );
                }
            } else {
                processPermissions( m, object );
            }

        }

    }

    /**
     * @param m
     * @param object
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void processPermissions( Method m, Object object ) throws IllegalAccessException, InvocationTargetException {
        if ( log.isDebugEnabled() ) {
            log.debug( "The object is: " + object.getClass().getName() );
        }

        if ( m.getName().equals( "findOrCreate" ) )
            addPermission( object, getUsername(), getAuthority() );

        else if ( m.getName().equals( "remove" ) ) deletePermission( object, getUsername() );
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
    public void addPermission( Object object, String recipient, Integer permission ) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
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
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // basicAclExtendedDao.delete( makeObjectIdentity( object ), recipient );
        basicAclExtendedDao.delete( makeObjectIdentity( object ) );
        if ( log.isDebugEnabled() ) {
            log.debug( "Deleted object " + object + " ACL permissions for recipient " + recipient );
        }
    }

    /**
     * Forms the object identity to be inserted in acl_object_identity table.
     * 
     * @param object
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    private AclObjectIdentity makeObjectIdentity( Object object ) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        return new NamedEntityObjectIdentity( object );
    }

    /**
     * Returns a String username.
     * 
     * @return
     */
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
                if ( log.isDebugEnabled() ) log.debug( "Granting ADMINISTRATION privileges" );
                return new Integer( SimpleAclEntry.ADMINISTRATION );
            }
        }
        if ( log.isDebugEnabled() ) log.debug( "Granting READ_WRITE privileges" );
        return new Integer( SimpleAclEntry.READ_WRITE );

    }

    /**
     * @param basicAclExtendedDao
     */
    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

}
