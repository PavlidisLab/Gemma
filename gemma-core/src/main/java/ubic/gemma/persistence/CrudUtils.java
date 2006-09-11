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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.CascadingAction;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import ubic.gemma.util.ReflectionUtil;

/**
 * Convenience methods needed to perform CRUD operations on entities.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean name="crudUtils"
 * @spring.property name="sessionFactory" ref="sessionFactory"
 */
public class CrudUtils implements InitializingBean, ApplicationContextAware {

    private static Set<String> crudMethods;

    static Log log = LogFactory.getLog( CrudUtils.class.getName() );

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
    }

    private Map metaData;

    private SessionFactory sessionFactory;

    private ApplicationContext applicationContext;

    private Map collectionMetaData;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        metaData = sessionFactory.getAllClassMetadata();
        collectionMetaData = sessionFactory.getAllCollectionMetadata();
    }

    /**
     * @param role
     * @return
     */
    public CollectionPersister getCollectionPersister( String role ) {
        return ( CollectionPersister ) collectionMetaData.get( role );
    }

    /**
     * Get a predicate that can be used to run 'create' on entities that have the same class as the passed-in entity.
     * The predicate returns true if 'create' was actually called (the object is transient), false otherwise.
     * 
     * @param entity
     * @return predicate if successful, null if not.
     */
    public Predicate getCreatePredicate() {
        return new Predicate() {
            public boolean evaluate( Object entity ) {
                try {
                    Object service = getService( entity );
                    final Method m = getCreateMethod( service, entity );

                    if ( org.apache.commons.beanutils.BeanUtils.getSimpleProperty( entity, "id" ) == null ) {
                        log.debug( "Invoking " + m.getName() + " on " + service.getClass().getName()
                                + " with argument " + entity );
                        entity = m.invoke( service, new Object[] { entity } );
                        return true; // wasn't already persistent
                    }
                    return false; // doesn't need persisting.
                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }
            }
        };
    }

    /**
     * Get a predicate that can be used to run 'delete' on entities that have the same class as the passed-in entity.
     * The predicate returns true if 'delete' was actually called, false otherwise.
     * 
     * @param entity
     * @return predicate if successful, null if not.
     */
    public Predicate getDeletePredicate() {
        return new Predicate() {
            public boolean evaluate( Object entity ) {
                try {
                    Object service = getService( entity );
                    final Method m = getDeleteMethod( service, entity );

                    if ( org.apache.commons.beanutils.BeanUtils.getSimpleProperty( entity, "id" ) == null ) {
                        return false;
                    }
                    log.debug( "Invoking " + m.getName() + " on " + service.getClass().getName() + " with argument "
                            + entity );
                    m.invoke( service, new Object[] { entity } );
                    return true;

                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }
            }
        };
    }

    /**
     * @param object
     * @return
     */
    public EntityPersister getEntityPersister( Object object ) {
        return ( ( EntityPersister ) metaData.get( object.getClass().getName() ) );
    }

    /**
     * Get a predicate that can be used to run 'findOrCreate' on entities that have the same class as the passed-in
     * entity. The predicate returns true if 'findOrCreate' was actually called, false otherwise. In particular, the
     * predicate will return false if the entity is already persistent.
     * 
     * @param entity
     * @return predicate if successful, null if not.
     */
    public Predicate getFindOrCreatePredicate() {
        return new Predicate() {
            public boolean evaluate( Object entity ) {
                try {
                    Object service = getService( entity );
                    final Method m = getFindOrCreateMethod( service, entity );

                    if ( org.apache.commons.beanutils.BeanUtils.getSimpleProperty( entity, "id" ) != null ) {
                        return false;
                    }
                    log.debug( "Invoking " + m.getName() + " on " + service.getClass().getName() + " with argument "
                            + entity );
                    entity = m.invoke( service, new Object[] { entity } );
                    return true;

                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }
            }
        };
    }

    /**
     * Given an entity, locate the corresponding service in the application context.
     * 
     * @param entity
     * @return
     */
    public Object getService( Object entity ) {
        Class clazz = entity.getClass();
        String serviceInterfaceSimplName = StringUtils.chomp( clazz.getSimpleName(), "Impl" ) + "Service";
        String serviceBeanName = StringUtils.uncapitalize( serviceInterfaceSimplName );

        Object bean;
        try {
            bean = applicationContext.getBean( serviceBeanName );
        } catch ( NoSuchBeanDefinitionException e ) {
            throw new IllegalArgumentException( "There is no service for " + entity.getClass().getName() );
        }

        if ( bean instanceof Advised ) {
            try {
                if ( log.isDebugEnabled() )
                    log.debug( "Found service for " + entity.getClass().getSimpleName() + ": "
                            + ( ( Advised ) bean ).getTargetSource().getTarget().getClass().getName() );
                return ( ( Advised ) bean ).getTargetSource().getTarget();
            } catch ( Exception e ) {
                log.warn( "Expected a proxy, got a " + bean.getClass().toString() );
                return bean;
            }
        }
        // TODO possibly create a cache of the service beans, keyed by the entity class.
        throw new RuntimeException( "Could not locate service for " + entity.getClass() );
    }

    /**
     * Get a predicate that can be used to run 'update' on entities that have the same class as the passed-in entity.
     * The predicate returns true if 'update' was actually called, false otherwise.
     * 
     * @param entity
     * @return predicate if successful, null if not.
     */
    public Predicate getUpdatePredicate() {
        return new Predicate() {
            public boolean evaluate( Object entity ) {
                try {
                    Object service = getService( entity );
                    final Method m = getUpdateMethod( service, entity );

                    if ( org.apache.commons.beanutils.BeanUtils.getSimpleProperty( entity, "id" ) == null ) {
                        throw new IllegalArgumentException( entity + " was not persistent, cannot update." );
                    }
                    log.debug( "Invoking " + m.getName() + " on " + service.getClass().getName() + " with argument "
                            + entity );
                    m.invoke( service, new Object[] { entity } );
                    return true;

                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }
            }
        };
    }

    /**
     * Determine if cascading an association is required.
     * 
     * @param cs
     * @return
     */
    public boolean needCascade( CascadeStyle cs ) {
        return cs.doCascade( CascadingAction.PERSIST ) || cs.doCascade( CascadingAction.SAVE_UPDATE )
                || cs.doCascade( CascadingAction.SAVE_UPDATE_COPY );
    }

    /**
     * Determine if cascading an association is required.
     * 
     * @param m
     * @param cs
     * @return
     */
    public boolean needCascade( Method m, CascadeStyle cs ) {

        if ( methodIsDelete( m ) ) {
            return cs.doCascade( CascadingAction.DELETE );
        }

        return needCascade( cs );
    }

    /**
     * Recursively execute predicate on a persistent entity and all its associated objects. Operations will be assumed
     * to cascade if the cascade style results in willCascade returning true.
     * <p>
     * FIXME - deal with cascades better.
     * 
     * @param entity
     * @param fromEntity the entity this entity is associated with. Used during recursion; when calling this method
     *        yourself, the value doesn't matter (you can just pass in null)
     * @param predicate to be evaluated on the object. It should return false to indicate the recursion should halt,
     *        true to indicate recursion should continue.
     * @param ignoreCascadeStyles if true, then any cascading behavior configured will be ignored, and operations will
     *        proceed recursively on all associations as long as the predicate returns true. Otherwise, if willCascade
     *        returns true for the cascade style of an encountered association, the recursion will be broken for that
     *        association. Set this to false if you are using this to perform CRUD operations, but set to true if you
     *        want to force the predicate to be run regardless of the persistence engine configuration.
     * @param c
     */
    public void processAssociations( Object entity, Object fromEntity, Predicate predicate, boolean ignoreCascadeStyles ) {
        EntityPersister persister = getEntityPersister( entity );

        if ( persister == null ) return;

        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();

        String[] propertyNames = persister.getPropertyNames();

        try {
            for ( int j = 0; j < propertyNames.length; j++ ) {

                if ( !ignoreCascadeStyles ) {
                    CascadeStyle cs = cascadeStyles[j];

                    /*
                     * If the cascade style of this association will result in the action being taken on the children
                     * anyway, we don't want to bother doing it here. Otherwise we conflict with the semantics of the
                     * cascade. This behavior makes sense if the action the predicate does is a CRUD action.
                     */
                    if ( !willCascade( cs ) ) {
                        if ( log.isDebugEnabled() )
                            log.debug( "Not processing association, it will cascade " + propertyNames[j] + ", Cascade="
                                    + cs );
                        continue;
                    }
                }

                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( entity.getClass(), propertyNames[j] );
                Object associatedObject = ReflectionUtil.getProperty( entity, descriptor );

                if ( associatedObject == null ) continue; // nothing to persist

                Class<?> propertyType = descriptor.getPropertyType();

                if ( log.isDebugEnabled() )
                    log.debug( "Processing " + propertyNames[j] + " (" + associatedObject + " - "
                            + propertyType.getName() + ")" );

                /*
                 * Run the predicate on the object, and recurse, but break the recursion if the predicate returns false.
                 */
                if ( Collection.class.isAssignableFrom( propertyType ) ) {
                    Collection associatedObjects = ( Collection ) ReflectionUtil.getProperty( entity, descriptor );
                    if ( associatedObjects.size() == 0 ) continue;
                    if ( log.isDebugEnabled() ) log.debug( propertyNames[j] + " is a non-empty collection" );
                    for ( Object object2 : associatedObjects ) {

                        boolean keepGoing = predicate.evaluate( object2 );
                        if ( keepGoing ) {
                            processAssociations( object2, entity, predicate, ignoreCascadeStyles );
                        }
                    }
                } else if ( getEntityPersister( associatedObject ) == null ) {
                    // Need to check that this isn't a 'String' or something (NB, Collections don't have
                    // entityPersisters either, but they do have CollectionPersisters).
                    if ( log.isDebugEnabled() )
                        log.debug( "Don't need to call for " + propertyNames[j] + " ("
                                + associatedObject.getClass().getName() + ")" );
                    continue;

                } else {
                    if ( log.isDebugEnabled() )
                        log.debug( propertyNames[j] + "(" + associatedObject.getClass().getName() + ") needs call " );

                    /*
                     * Only contine if the predicate returned true, and make sure we don't travel backwards up the
                     * association graph (in which case an infinite loop would result)
                     */
                    boolean keepGoing = fromEntity != associatedObject && predicate.evaluate( associatedObject );

                    if ( log.isDebugEnabled() && fromEntity == associatedObject ) {
                        log.debug( associatedObject + "is the same we came from, we will not travel back to it" );
                    }

                    if ( keepGoing ) processAssociations( associatedObject, entity, predicate, ignoreCascadeStyles );
                }
                // otherwise, we don't need to 'create' it.

            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * @param factory the factory to set
     */
    public void setSessionFactory( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @return true if the cascade style will result in a
     */
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

        Class[] argArray = null;
        try {
            Class entityInterface = Class.forName( argInterfaceName );
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
     * @param entity
     * @return
     */
    Object invokeCreate( Object entity ) {
        Object service = getService( entity );
        Method m = this.getCreateMethod( service, entity );
        try {
            return m.invoke( service, new Object[] { entity } );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param entity
     */
    void invokeDelete( Object entity ) {
        Object service = getService( entity );

        Method m = this.getDeleteMethod( service, entity );
        try {
            m.invoke( service, new Object[] { entity } );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param entity
     * @return
     */
    Object invokeFind( Object entity ) {
        Object service = getService( entity );
        Method m = this.getFindMethod( service, entity );

        try {
            return m.invoke( service, new Object[] { entity } );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param entity
     * @return
     */
    Object invokeFindOrCreate( Object entity ) {
        Object service = getService( entity );
        Method m = this.getFindOrCreateMethod( service, entity );

        try {
            return m.invoke( service, new Object[] { entity } );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param entity
     */
    void invokeUpdate( Object entity ) {
        Object service = getService( entity );
        Method m = this.getUpdateMethod( service, entity );

        try {
            m.invoke( service, new Object[] { entity } );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

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
        return m.getName().equals( "create" ) || m.getName().equals( "save" ) || m.getName().equals( "findOrCreate" );
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
        return m.getName().equals( "remove" ) || m.getName().equals( "delete" );
    }

    /**
     * Test whether a method loads objects. Patterns accepted include "read", "find", "load*" (latter for load or
     * loadAll)
     * 
     * @param m
     * @return
     */
    public static boolean methodIsLoad( Method m ) {
        return m.getName().equals( "read" ) || m.getName().startsWith( "find" ) || m.getName().startsWith( "load" );
    }

    /**
     * Test whether a method updates objects
     * 
     * @param m
     * @return
     */
    public static boolean methodIsUpdate( Method m ) {
        return m.getName().equals( "update" );
    }
}
