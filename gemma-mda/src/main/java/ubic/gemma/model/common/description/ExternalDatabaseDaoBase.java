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
    public java.util.Collection<? extends ExternalDatabase> create(
            final java.util.Collection<? extends ExternalDatabase> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.common.description.ExternalDatabase ) entityIterator.next() );
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
    public ExternalDatabase create( final ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        if ( externalDatabase == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.create - 'externalDatabase' can not be null" );
        }
        this.getHibernateTemplate().save( externalDatabase );
        return externalDatabase;
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByLocalDbInstallName(int, java.lang.String)
     */

    public java.util.Collection findByLocalDbInstallName( final java.lang.String localInstallDBName ) {
        return this.findByLocalDbInstallName(

        "from ExternalDatabaseImpl externalDatabase where externalDatabase.localInstallDbName=:localInstallDBName ",
                localInstallDBName );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByLocalDbInstallName(int, java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection findByLocalDbInstallName( final java.lang.String queryString,
            final java.lang.String localInstallDBName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( localInstallDBName );
        argNames.add( "localInstallDBName" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByName(int, java.lang.String)
     */

    public ExternalDatabase findByName( final java.lang.String name ) {
        return this.findByName( "from ExternalDatabaseImpl e where e.name=:name", name );
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#findByName(int, java.lang.String, java.lang.String)
     */

    public ExternalDatabase findByName( final java.lang.String queryString, final java.lang.String name ) {
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

        return ( ExternalDatabase ) result;
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#load(int, java.lang.Long)
     */

    public ExternalDatabase load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExternalDatabase.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.ExternalDatabaseImpl.class, id );
        return ( ubic.gemma.model.common.description.ExternalDatabase ) entity;
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseDao#loadAll(int)
     */

    public java.util.Collection loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.ExternalDatabaseImpl.class );
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

}