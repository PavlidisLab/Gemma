/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.description;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.ExternalDatabase</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.ExternalDatabase
 */
public abstract class ExternalDatabaseDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.description.ExternalDatabaseDao {

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.common.description.ExternalDatabase ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    
    public Collection<? extends ExternalDatabase> load( Collection<Long> ids ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "from ExternalDatabaseImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#create(int transform,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */
    public Object create( final int transform,
            final ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.create - 'externalDatabase' can not be null" );
        }
        this.getHibernateTemplate().save( externalDatabase );
        return this.transformEntity( transform, externalDatabase );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#create(java.util.Collection)
     */
    
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#create(ubic.gemma.model.common.description.ExternalDatabase)
     */
    public ExternalDatabase create( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        return ( ubic.gemma.model.common.description.ExternalDatabase ) this.create( TRANSFORM_NONE, externalDatabase );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByLocalDbInstallName(int, java.lang.String)
     */
    
    public java.util.Collection findByLocalDbInstallName( final int transform, final java.lang.String localInstallDBName ) {
        return this
                .findByLocalDbInstallName(
                        transform,
                        "from ExternalDatabaseImpl externalDatabase where externalDatabase.localInstallDbName=:localInstallDBName ",
                        localInstallDBName );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByLocalDbInstallName(int, java.lang.String,
     *      java.lang.String)
     */
    
    public java.util.Collection findByLocalDbInstallName( final int transform, final java.lang.String queryString,
            final java.lang.String localInstallDBName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( localInstallDBName );
        argNames.add( "localInstallDBName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByLocalDbInstallName(java.lang.String)
     */
    public java.util.Collection findByLocalDbInstallName( java.lang.String localInstallDBName ) {
        return this.findByLocalDbInstallName( TRANSFORM_NONE, localInstallDBName );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByLocalDbInstallName(java.lang.String,
     *      java.lang.String)
     */
    
    public java.util.Collection findByLocalDbInstallName( final java.lang.String queryString,
            final java.lang.String localInstallDBName ) {
        return this.findByLocalDbInstallName( TRANSFORM_NONE, queryString, localInstallDBName );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByName(int, java.lang.String)
     */
    
    public Object findByName( final int transform, final java.lang.String name ) {
        return this.findByName( transform, "from ExternalDatabaseImpl e where e.name=:name", name );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByName(int, java.lang.String, java.lang.String)
     */
    
    public Object findByName( final int transform, final java.lang.String queryString, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.ExternalDatabase"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.description.ExternalDatabase ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByName(java.lang.String)
     */
    public ubic.gemma.model.common.description.ExternalDatabase findByName( java.lang.String name ) {
        return ( ubic.gemma.model.common.description.ExternalDatabase ) this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByName(java.lang.String, java.lang.String)
     */
    
    public ubic.gemma.model.common.description.ExternalDatabase findByName( final java.lang.String queryString,
            final java.lang.String name ) {
        return ( ubic.gemma.model.common.description.ExternalDatabase ) this.findByName( TRANSFORM_NONE, queryString,
                name );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.ExternalDatabaseImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.ExternalDatabase ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#load(java.lang.Long)
     */

    public ExternalDatabase load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.ExternalDatabase ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#loadAll()
     */

    
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.ExternalDatabaseImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.ExternalDatabase entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#remove(ubic.gemma.model.common.description.ExternalDatabase)
     */
    public void remove( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.remove - 'externalDatabase' can not be null" );
        }
        this.getHibernateTemplate().delete( externalDatabase );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.ExternalDatabase ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#update(ubic.gemma.model.common.description.ExternalDatabase)
     */
    public void update( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.update - 'externalDatabase' can not be null" );
        }
        this.getHibernateTemplate().update( externalDatabase );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.ExternalDatabase)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.description.ExternalDatabaseDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.ExternalDatabase)
     */

    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.description.ExternalDatabaseDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.description.ExternalDatabaseDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.common.description.ExternalDatabase entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}