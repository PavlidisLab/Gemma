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
package ubic.gemma.persistence;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.CascadingAction;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Convenience methods needed to perform CRUD operations on entities.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class CrudUtilsImpl implements InitializingBean, CrudUtils {

    static Log log = LogFactory.getLog( CrudUtilsImpl.class.getName() );

    private static Set<String> crudMethods;

    static {
        crudMethods = new HashSet<String>();
        crudMethods.add( "update" );
        crudMethods.add( "create" );
        crudMethods.add( "remove" );
        crudMethods.add( "save" );
        crudMethods.add( "find" );
        crudMethods.add( "findOrCreate" );
        crudMethods.add( "delete" );
        crudMethods.add( "load" );
        crudMethods.add( "loadAll" );
        crudMethods.add( "persist" );
        crudMethods.add( "persistOrUpdate" );
    }

    /**
     * @param entity
     * @return
     */
    public static boolean isTransient( Object entity ) {
        if ( entity == null ) return true;
        try {
            return org.apache.commons.beanutils.BeanUtils.getSimpleProperty( entity, "id" ) == null;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Test whether a method creates objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsCreate( Method m ) {
        return methodIsCreate( m.getName() );
    }

    public static boolean methodIsCreate( String m ) {
        return m.equals( "create" ) || m.equals( "save" ) || m.equals( "findOrCreate" ) || m.equals( "persist" );
    }

    /**
     * Test whether a method is a CRUD method.
     * 
     * @param m
     * @return
     */
    public static boolean methodIsCrud( Method m ) {
        if ( log.isTraceEnabled() ) log.trace( "Testing " + m.getName() );
        return crudMethods.contains( m.getName() );
    }

    /**
     * Test whether a method deletes objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsDelete( Method m ) {
        return methodIsDelete( m.getName() );
    }

    public static boolean methodIsDelete( String s ) {
        return s.equals( "remove" ) || s.equals( "delete" );
    }

    /**
     * Test whether a method loads objects. Patterns accepted include "read", "find", "load*" (latter for load or
     * loadAll)
     * 
     * @param m
     * @return
     */
    public static boolean methodIsLoad( Method m ) {
        return methodIsLoad( m.getName() );
    }

    public static boolean methodIsLoad( String m ) {
        return m.equals( "read" ) || m.startsWith( "find" ) || m.startsWith( "load" );
    }

    /**
     * Test whether a method updates objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsUpdate( Method m ) {
        return methodIsUpdate( m.getName() );
    }

    public static boolean methodIsUpdate( String s ) {
        return s.equals( "update" ) || s.equals( "persistOrUpdate" );
    }

    private Map<String, CollectionMetadata> collectionMetaData;

    private Map<String, ClassMetadata> metaData;

    @Autowired
    private SessionFactory sessionFactory;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {
        metaData = sessionFactory.getAllClassMetadata();
        collectionMetaData = sessionFactory.getAllCollectionMetadata();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudUtils#getCollectionPersister(java.lang.String)
     */
    @Override
    public CollectionPersister getCollectionPersister( String role ) {
        return ( CollectionPersister ) collectionMetaData.get( role );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudUtils#getEntityPersister(java.lang.Object)
     */
    @Override
    public EntityPersister getEntityPersister( Object object ) {
        String className = object.getClass().getName();
        // hack to remove "$$EnhancerByCGLib... or _$$_javaassist_
        className = className.replaceAll( "(_)?\\$\\$.+$", "" );

        return ( ( EntityPersister ) metaData.get( className ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudUtils#getSessionFactory()
     */
    @Override
    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudUtils#needCascade(java.lang.String, org.hibernate.engine.CascadeStyle)
     */
    @Override
    public boolean needCascade( String m, CascadeStyle cs ) {

        if ( methodIsDelete( m ) ) {
            return cs.doCascade( CascadingAction.DELETE );
        }

        return needCascade( cs );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudUtils#setSessionFactory(org.hibernate.SessionFactory)
     */
    @Override
    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudUtils#willCascade(org.hibernate.engine.CascadeStyle)
     */
    @Override
    public boolean willCascade( CascadeStyle cs ) {
        return cs == CascadeStyle.ALL || cs == CascadeStyle.ALL_DELETE_ORPHAN || cs == CascadeStyle.UPDATE;
    }

    /**
     * @param service The service where we look for the method
     * @param entity The type of entity to be created
     * @return
     */
    Method getCreateMethod( Object service, Object entity ) {
        return getMethodForNames( service, entity, new String[] { "create", "save" } );
    }

    /**
     * @param service The service where we look for the method
     * @param entity The type of entity to be deleted
     * @return
     */
    Method getDeleteMethod( Object service, Object entity ) {
        return getMethodForNames( service, entity, new String[] { "delete", "remove" } );
    }

    /**
     * @param service The service where we look for the method
     * @param entity The type of entity to be found
     * @return
     */
    Method getFindMethod( Object service, Object entity ) {
        return getMethodForNames( service, entity, new String[] { "find" } );
    }

    /**
     * @param service The service where we look for the method
     * @param entity The type of entity to be found or created
     * @return
     */
    Method getFindOrCreateMethod( Object service, Object entity ) {
        return getMethodForNames( service, entity, new String[] { "findOrCreate" } );
    }

    /**
     * @param target The object that might have the method (e.g., a service)
     * @param argument The single argument the method is expected to take. (e.g., an entity)
     * @param possibleNames An array of names of the methods (e.g., "create", "save").
     * @return
     */
    Method getMethodForNames( Object target, Object argument, String[] possibleNames ) {
        String argInterfaceName = StringUtils.chomp( argument.getClass().getName(), "Impl" );

        Class<?>[] argArray = null;
        try {
            Class<?> entityInterface = Class.forName( argInterfaceName );
            argArray = new Class[] { entityInterface };
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        for ( String methodName : possibleNames ) {
            try {
                return target.getClass().getMethod( methodName, argArray );
            } catch ( Exception e1 ) {
                // okay keep trying
            }
        }
        log.warn( target.getClass().getSimpleName() + " does not have method with " + argInterfaceName + " argument" );
        return null;
    }

    /**
     * @param service The service where we look for the method
     * @param entity The type of entity to be updated
     * @return
     */
    Method getUpdateMethod( Object service, Object entity ) {
        return getMethodForNames( service, entity, new String[] { "update" } );
    }

    /**
     * Determine if cascading an association is required.
     * 
     * @param cs
     * @return
     */
    private boolean needCascade( CascadeStyle cs ) {
        return cs.doCascade( CascadingAction.PERSIST ) || cs.doCascade( CascadingAction.SAVE_UPDATE )
                || cs.doCascade( CascadingAction.SAVE_UPDATE_COPY );
    }
}
