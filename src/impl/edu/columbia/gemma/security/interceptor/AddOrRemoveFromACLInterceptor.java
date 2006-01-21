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
import java.util.HashSet;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.acl.basic.AclObjectIdentity;
import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.context.SecurityContextHolder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.dao.DataAccessException;

import edu.columbia.gemma.expression.bioAssayData.BioAssayDataVector;
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;

/**
 * Adds security controls to newly created objects, and removes them for objects that are deleted. Methods in this
 * interceptor are run for all new objects (to add security if needed) and when objects are deleted.
 * <p>
 * Implementation Note: For permissions modification to be triggered, the method name must match certain patterns, which
 * include "create" and "remove". Other methods that would require changes to permissions will not work without
 * modifying the source code.
 * <hr>
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class AddOrRemoveFromACLInterceptor implements AfterReturningAdvice {

    /**
     * For some types of objects, we don't put permissions on them directly, but on the containing object. Example:
     * reporter - we secure the arrayDesign, but not the reporter.
     */
    private static final Collection<Class> unsecuredClasses = new HashSet<Class>();

    static {
        unsecuredClasses.add( Reporter.class );
        unsecuredClasses.add( CompositeSequence.class );
        unsecuredClasses.add( BioAssayDataVector.class );
    }

    /**
     * Objects are grouped in a hierarchy. A default 'parent' is defined in the database. This must match an entry in
     * the ACL_OBJECT_IDENTITY table. In Gemma this is added as part of database initialization (see mysql-acegy-acl.sql
     * for MySQL version)
     */
    private static final String DEFAULT_PARENT = "globalDummyParent";

    /**
     * @see DEFAULT_PARENT
     */
    private static final String DEFAULT_PARENT_ID = "1";

    private static Log log = LogFactory.getLog( AddOrRemoveFromACLInterceptor.class.getName() );

    private BasicAclExtendedDao basicAclExtendedDao;

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

        /* By default we assign the object to have the default global parent. */
        simpleAclEntry.setAclObjectParentIdentity( new NamedEntityObjectIdentity( DEFAULT_PARENT, DEFAULT_PARENT_ID ) );

        basicAclExtendedDao.create( simpleAclEntry );

        if ( log.isDebugEnabled() ) {
            log.debug( "Added permission " + permission + " for recipient " + recipient + " on " + object );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.AfterReturningAdvice#afterReturning(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[], java.lang.Object)
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public void afterReturning( Object retValue, Method m, Object[] args, Object target ) throws Throwable {

        if ( log.isDebugEnabled() ) log.debug( "Before: method=[" + m + "], Target: " + target );

        if ( methodTriggersACLAction( m ) ) {

            // This works because create methods modify the argument. Otherwise we would have to look at the return
            // value.
            assert args != null;
            assert args.length == 1;
            Object persistentObject = args[0];

            if ( unsecuredClasses.contains( persistentObject.getClass() ) ) {
                if ( log.isDebugEnabled() ) {
                    log.debug( persistentObject.getClass().getName()
                            + " is not a secured object, skipping permissions modification." );
                }
                return;
            }

            if ( Collection.class.isAssignableFrom( persistentObject.getClass() ) ) {
                for ( Object o : ( Collection<Object> ) persistentObject ) {
                    processObject( m, o );
                }
            } else {
                processObject( m, persistentObject );
            }
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
        basicAclExtendedDao.delete( makeObjectIdentity( object ) );
        if ( log.isDebugEnabled() ) {
            log.debug( "Deleted object " + object + " ACL permissions for recipient " + recipient );
        }
    }

    /**
     * @param basicAclExtendedDao
     */
    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

    /**
     * Forms the object identity to be inserted in acl_object_identity table.
     * 
     * @param object
     * @return object identity.
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    private AclObjectIdentity makeObjectIdentity( Object object ) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        assert checkValidPrimaryKey( object ) : "No valid primary key for object " + object;
        return new NamedEntityObjectIdentity( object );
    }

    /**
     * For debugging purposes.
     * 
     * @param object
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private boolean checkValidPrimaryKey( Object object ) throws IllegalAccessException, InvocationTargetException {
        Class clazz = object.getClass();
        try {
            Method method = clazz.getMethod( "getId", new Class[] {} );
            Object result = method.invoke( object, new Object[] {} );
            if ( result == null ) {
                return false;
            }
        } catch ( NoSuchMethodException nsme ) {
            throw new IllegalArgumentException( "Object of class '" + clazz
                    + "' does not provide the required getId() method: " + object );
        }
        return true;
    }

    /**
     * Test whether a method requires ACL permissions to be added.
     * 
     * @param m
     * @return
     */
    private boolean methodsTriggersACLAddition( Method m ) {
        return m.getName().equals( "create" );
    }

    /**
     * Test whether a method requires any ACL action at all.
     * 
     * @param m
     * @return
     */
    private boolean methodTriggersACLAction( Method m ) {
        return methodsTriggersACLAddition( m ) || methodTriggersACLDelete( m );
    }

    /**
     * Test whether a method requires ACL permissions to be deleted.
     * 
     * @param m
     * @return
     */
    private boolean methodTriggersACLDelete( Method m ) {
        return m.getName().equals( "remove" );
    }

    /**
     * @param m method that was called. This is used to determine what action to take.
     * @param object. If null, no action is taken.
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void processObject( Method m, Object object ) throws IllegalAccessException, InvocationTargetException {

        if ( object == null ) return;

        assert m != null;

        if ( log.isDebugEnabled() ) {
            log.debug( "Processing permissions for: " + object.getClass().getName() + " for method " + m.getName() );
        }
        if ( methodsTriggersACLAddition( m ) ) {
            addPermission( object, getUsername(), getAuthority() );
        } else if ( methodTriggersACLDelete( m ) ) {
            deletePermission( object, getUsername() );
        } else {
            // nothing to do.
        }
    }

    /**
     * For the current principal (user), return the permissions mask. If the current principal has role "admin", they
     * are granted ADMINISTRATION authority. If they are role "user", they are granted READ_WRITE authority.
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
     * Returns a String username (the principal).
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

}
