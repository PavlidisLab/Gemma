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
package ubic.gemma.persistence.service.common.description;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.ExternalDatabase</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.ExternalDatabase
 */
public abstract class ExternalDatabaseDaoBase extends HibernateDaoSupport implements ExternalDatabaseDao {

    /**
     * @see ExternalDatabaseDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends ExternalDatabase> create(
            final java.util.Collection<? extends ExternalDatabase> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExternalDatabase> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public Collection<? extends ExternalDatabase> load( Collection<Long> ids ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "from ExternalDatabaseImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ExternalDatabaseDao#create(int transform,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public ExternalDatabase create( final ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.create - 'externalDatabase' can not be null" );
        }
        this.getHibernateTemplate().save( externalDatabase );
        return externalDatabase;
    }

    /**
     * @see ExternalDatabaseDao#findByLocalDbInstallName(int, java.lang.String)
     */

    @Override
    public java.util.Collection<ExternalDatabase> findByLocalDbInstallName( final java.lang.String localInstallDBName ) {
        return this
                .findByLocalDbInstallName(
                        "from ExternalDatabaseImpl externalDatabase where externalDatabase.localInstallDbName=:localInstallDBName ",
                        localInstallDBName );
    }

    /**
     * @see ExternalDatabaseDao#findByLocalDbInstallName(int, java.lang.String,
     *      java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<ExternalDatabase> findByLocalDbInstallName( final java.lang.String queryString,
            final java.lang.String localInstallDBName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( localInstallDBName );
        argNames.add( "localInstallDBName" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<ExternalDatabase> ) results;
    }

    /**
     * @see ExternalDatabaseDao#findByName(int, java.lang.String)
     */

    @Override
    public ExternalDatabase findByName( final java.lang.String name ) {
        return this.findByName( "from ExternalDatabaseImpl e where e.name=:name", name );
    }

    /**
     * @see ExternalDatabaseDao#findByName(int, java.lang.String, java.lang.String)
     */

    public ExternalDatabase findByName( final java.lang.String queryString, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.Set<? extends ExternalDatabase> results = new java.util.LinkedHashSet<ExternalDatabase>( this
                .getHibernateTemplate().findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ),
                        args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.ExternalDatabase"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( ExternalDatabase ) result;
    }

    /**
     * @see ExternalDatabaseDao#load(int, java.lang.Long)
     */

    @Override
    public ExternalDatabase load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.ExternalDatabaseImpl.class, id );
        return ( ubic.gemma.model.common.description.ExternalDatabase ) entity;
    }

    /**
     * @see ExternalDatabaseDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends ExternalDatabase> loadAll() {
        final java.util.Collection<? extends ExternalDatabase> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.ExternalDatabaseImpl.class );
        return results;
    }

    /**
     * @see ExternalDatabaseDao#remove(java.lang.Long)
     */

    @Override
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

    @Override
    public void remove( java.util.Collection<? extends ExternalDatabase> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ExternalDatabaseDao#remove(ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public void remove( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.remove - 'externalDatabase' can not be null" );
        }
        this.getHibernateTemplate().delete( externalDatabase );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends ExternalDatabase> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends ExternalDatabase> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ExternalDatabaseDao#update(ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public void update( ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.update - 'externalDatabase' can not be null" );
        }
        this.getHibernateTemplate().update( externalDatabase );
    }

}