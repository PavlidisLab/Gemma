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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.acl.basic.AbstractBasicAclEntry;
import org.acegisecurity.acl.basic.AclObjectIdentity;
import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.CascadingAction;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate3.HibernateInterceptor;

import edu.columbia.gemma.association.Relationship;
import edu.columbia.gemma.common.Securable;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssayData.BioAssayDataVector;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.gene.GeneAlias;
import edu.columbia.gemma.genome.gene.GeneProduct;

/**
 * Adds security controls to newly created objects, and removes them for objects that are deleted. Methods in this
 * interceptor are run for all new objects (to add security if needed) and when objects are deleted.
 * <p>
 * Implementation Note: For permissions modification to be triggered, the method name must match certain patterns, which
 * include "create" and "remove". Other methods that would require changes to permissions will not work without
 * modifying the source code.
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

    /*
     * Classes to skip because they aren't secured. Either these are always "public" objects, or they are secured by
     * composition. In principle this shouldn't needed in most cases because the methods for the corresponding services
     * are not interccepted anyway.
     */
    static {
        unsecuredClasses.add( BioAssayDataVector.class );
        unsecuredClasses.add( DatabaseEntry.class );
        unsecuredClasses.add( BioSequence.class );
        unsecuredClasses.add( Relationship.class );
        unsecuredClasses.add( DesignElement.class );
        unsecuredClasses.add( Taxon.class );
        unsecuredClasses.add( Gene.class );
        unsecuredClasses.add( GeneProduct.class );
        unsecuredClasses.add( GeneAlias.class );
        unsecuredClasses.add( QuantitationType.class );
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

    private HibernateInterceptor hibernateInterceptor;

    private Map metaData;

    /**
     * Creates the acl_permission object and the acl_object_identity object.
     * 
     * @param method - method called to trigger this action
     * @param object - represents the domain object.
     * @param recipient
     * @param permission
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void addPermission( Method method, Object object ) throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        AbstractBasicAclEntry simpleAclEntry = getAclEntry( object );

        /* By default we assign the object to have the default global parent. */
        simpleAclEntry.setAclObjectParentIdentity( new NamedEntityObjectIdentity( DEFAULT_PARENT, DEFAULT_PARENT_ID ) );

        try {
            basicAclExtendedDao.create( simpleAclEntry );
            if ( log.isDebugEnabled() ) {
                log.debug( "Added permission " + getAuthority() + " for recipient " + getUsername() + " on " + object );
            }
        } catch ( DataIntegrityViolationException e ) {
            if ( method.getName().equals( "findOrCreate" ) ) {
                // do nothing. This happens when the object already exists and has permissions assigned (for example,
                // findOrCreate resulted in a 'find')
                // FIXME this is an unpleasant hack.
            } else {
                // something else must be wrong.
                throw e;
            }
        }

    }

    /**
     * @param object
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static AbstractBasicAclEntry getAclEntry( Object object ) throws IllegalAccessException,
            InvocationTargetException {
        SimpleAclEntry simpleAclEntry = new SimpleAclEntry();
        simpleAclEntry.setAclObjectIdentity( makeObjectIdentity( object ) );
        simpleAclEntry.setMask( getAuthority() );
        simpleAclEntry.setRecipient( getUsername() );
        return simpleAclEntry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.aop.AfterReturningAdvice#afterReturning(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[], java.lang.Object)
     */
    @SuppressWarnings( { "unused", "unchecked" })
    public void afterReturning( Object retValue, Method m, Object[] args, Object target ) throws Throwable {

        if ( methodTriggersACLAction( m ) ) {

            assert args != null;
            assert args.length == 1;
            Object persistentObject = getPersistentObject( retValue, m, args );
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
     * @param class1
     * @return
     */
    private boolean unsecuredClassesContains( Class<? extends Object> c ) {
        for ( Class<? extends Object> clazz : unsecuredClasses ) {
            if ( clazz.isAssignableFrom( c ) ) return true;
        }
        return false;
    }

    /**
     * @param retValue
     * @param m
     * @param args
     * @return
     */
    private Object getPersistentObject( Object retValue, Method m, Object[] args ) {
        if ( methodTriggersACLDelete( m ) ) {
            return args[0];
        }
        assert retValue != null;
        return retValue;
    }

    /**
     * Delete acl permissions for an object.
     * 
     * @param object
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws DataAccessException
     */
    public void deletePermission( Object object ) throws DataAccessException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        basicAclExtendedDao.delete( makeObjectIdentity( object ) );
        if ( log.isDebugEnabled() ) {
            log.debug( "Deleted object " + object + " ACL permissions for recipient " + getUsername() );
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
     */
    private static AclObjectIdentity makeObjectIdentity( Object object ) throws IllegalAccessException,
            InvocationTargetException {
        assert checkValidPrimaryKey( object ) : "No valid primary key for object " + object;
        return new NamedEntityObjectIdentity( object );
    }

    /**
     * @param object
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static boolean checkValidPrimaryKey( Object object ) throws IllegalAccessException,
            InvocationTargetException {
        Class clazz = object.getClass();
        try {
            String methodName = "getId";
            Method m = clazz.getMethod( methodName, new Class[] {} );
            Object result = m.invoke( object, new Object[] {} );
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
        return m.getName().equals( "create" ) || m.getName().equals( "save" ) || m.getName().equals( "findOrCreate" );
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
        return m.getName().equals( "remove" ) || m.getName().equals( "delete" );
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

        if ( !Securable.class.isAssignableFrom( object.getClass() ) || unsecuredClassesContains( object.getClass() ) ) {
            if ( log.isDebugEnabled() ) {
                log.debug( object.getClass().getName() + " is not a secured object, skipping permissions processing." );
            }
            return;
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "Processing permissions for: " + object.getClass().getName() + " for method " + m.getName() );
        }
        if ( methodsTriggersACLAddition( m ) ) {
            addPermission( m, object );
            processAssociations( m, object );
        } else if ( methodTriggersACLDelete( m ) ) {
            deletePermission( object );
            processAssociations( m, object );
        } else {
            // nothing to do.
        }
    }

    /**
     * @param m
     * @param object
     */
    private void processAssociations( Method m, Object object ) throws IllegalAccessException,
            InvocationTargetException {

        EntityPersister persister = ( ( EntityPersister ) metaData.get( object.getClass().getName() ) );
        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
        String[] propertyNames = persister.getPropertyNames();

        for ( int j = 0; j < propertyNames.length; j++ ) {
            CascadeStyle cs = cascadeStyles[j];
            if ( !needCascade( m, cs ) ) {
                // log.debug( "Not processing association " + propertyNames[j] + ", Cascade=" + cs );
                continue;
            }

            PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyNames[j] );
            Class<?> propertyType = descriptor.getPropertyType();

            if ( Securable.class.isAssignableFrom( propertyType ) ) {
                Object associatedObject = getProperty( object, descriptor );
                log.debug( "Processing ACL for " + propertyNames[j] + ", Cascade=" + cs );
                processObject( m, associatedObject );
            } else if ( Collection.class.isAssignableFrom( propertyType ) ) {
                Collection associatedObjects = ( Collection ) getProperty( object, descriptor );
                for ( Object object2 : associatedObjects ) {
                    if ( Securable.class.isAssignableFrom( object2.getClass() ) ) {
                        log.debug( "Processing ACL for member " + object2 + " of collection " + propertyNames[j]
                                + ", Cascade=" + cs );
                        processObject( m, object2 );
                    }
                }
            }
        }
    }

    /**
     * FIXME put this method somewhere more useful...
     * 
     * @param object
     * @param descriptor
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private Object getProperty( Object object, PropertyDescriptor descriptor ) throws IllegalAccessException,
            InvocationTargetException {
        Method getter = descriptor.getReadMethod();
        Object associatedObject = getter.invoke( object, new Object[] {} );
        return associatedObject;
    }

    /**
     * @param m
     * @param cs
     * @return
     */
    private boolean needCascade( Method m, CascadeStyle cs ) {

        if ( methodTriggersACLDelete( m ) ) {
            return cs.doCascade( CascadingAction.DELETE );
        }
        return cs.doCascade( CascadingAction.PERSIST ) || cs.doCascade( CascadingAction.SAVE_UPDATE )
                || cs.doCascade( CascadingAction.SAVE_UPDATE_COPY );

    }

    /**
     * For the current principal (user), return the permissions mask. If the current principal has role "admin", they
     * are granted ADMINISTRATION authority. If they are role "user", they are granted READ_WRITE authority.
     * 
     * @return
     */
    protected static Integer getAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        GrantedAuthority[] ga = auth.getAuthorities();
        for ( int i = 0; i < ga.length; i++ ) {
            if ( ga[i].equals( "admin" ) ) {
                // if ( log.isDebugEnabled() ) log.debug( "Granting ADMINISTRATION privileges" );
                return SimpleAclEntry.ADMINISTRATION;
            }
        }
        // if ( log.isDebugEnabled() ) log.debug( "Granting READ_WRITE privileges" );
        return SimpleAclEntry.READ_WRITE;
    }

    /**
     * Returns a String username (the principal).
     * 
     * @return
     */
    protected static String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if ( auth.getPrincipal() instanceof UserDetails ) {
            return ( ( UserDetails ) auth.getPrincipal() ).getUsername();
        }
        return auth.getPrincipal().toString();

    }

    /**
     * @param hibernateInterceptor
     */
    public void setHibernateInterceptor( HibernateInterceptor hibernateInterceptor ) {
        this.hibernateInterceptor = hibernateInterceptor;
        initMetaData( hibernateInterceptor );
    }

    /**
     * @param hibernateInterceptor
     */
    private void initMetaData( HibernateInterceptor hibernateInterceptor ) {
        metaData = hibernateInterceptor.getSessionFactory().getAllClassMetadata();
    }

}
