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

import java.util.List;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.DatabaseEntry</code>.
 * 
 * @see ubic.gemma.model.common.description.DatabaseEntry
 * @version $Id$
 */
public abstract class DatabaseEntryDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.common.description.DatabaseEntryDao {

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.DatabaseEntryDao.countAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends DatabaseEntry> create(
            final java.util.Collection<? extends DatabaseEntry> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends DatabaseEntry> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#create(int transform,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public DatabaseEntry create( final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.create - 'databaseEntry' can not be null" );
        }
        this.getHibernateTemplate().save( databaseEntry );
        return databaseEntry;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#find(int, java.lang.String,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    public DatabaseEntry find( final java.lang.String queryString,
            final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( databaseEntry );
        argNames.add( "databaseEntry" );
        java.util.Set<? extends DatabaseEntry> results = new java.util.LinkedHashSet<DatabaseEntry>( this
                .getHibernateTemplate().findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ),
                        args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.DatabaseEntry"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( DatabaseEntry ) result;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#find(int,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */

    @Override
    public DatabaseEntry find( final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        return this.find( "from ubic.gemma.model.common.description.DatabaseEntryImpl"
                + " as databaseEntry where databaseEntry.databaseEntry = :databaseEntry", databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#findByAccession(int, java.lang.String,
     *      java.lang.String, ubic.gemma.model.common.description.ExternalDatabase)
     */

    public DatabaseEntry findByAccession( final java.lang.String queryString, final java.lang.String accession,
            final ubic.gemma.model.common.description.ExternalDatabase externalDb ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( accession );
        argNames.add( "accession" );
        args.add( externalDb );
        argNames.add( "externalDb" );
        List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.DatabaseEntry"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( DatabaseEntry ) result;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#findByAccession(int, java.lang.String,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */

    @Override
    public DatabaseEntry findByAccession( final java.lang.String accession,
            final ubic.gemma.model.common.description.ExternalDatabase externalDb ) {
        return this.findByAccession(
                "from DatabaseEntryImpl d where d.accession=:accession and d.externalDatabase=:externalDb", accession,
                externalDb );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#load(int, java.lang.Long)
     */
    @Override
    public DatabaseEntry load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( DatabaseEntryImpl.class, id );
        return ( DatabaseEntry ) entity;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends DatabaseEntry> loadAll() {
        final java.util.Collection<? extends DatabaseEntry> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.DatabaseEntryImpl.class );

        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.DatabaseEntry entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends DatabaseEntry> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#remove(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public void remove( ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.remove - 'databaseEntry' can not be null" );
        }
        this.getHibernateTemplate().delete( databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends DatabaseEntry> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends DatabaseEntry> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#update(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public void update( ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.update - 'databaseEntry' can not be null" );
        }
        this.getHibernateTemplate().update( databaseEntry );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

}