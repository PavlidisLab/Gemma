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

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.DatabaseEntry</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.DatabaseEntry
 */
public abstract class DatabaseEntryDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.common.description.DatabaseEntryDao {

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#countAll()
     */
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
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.common.description.DatabaseEntry ) entityIterator
                                    .next() );
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
    public Object create( final int transform, final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.create - 'databaseEntry' can not be null" );
        }
        this.getHibernateTemplate().save( databaseEntry );
        return this.transformEntity( transform, databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#create(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.common.description.DatabaseEntry create(
            ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        return ( ubic.gemma.model.common.description.DatabaseEntry ) this.create( TRANSFORM_NONE, databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#find(int, java.lang.String,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( databaseEntry );
        argNames.add( "databaseEntry" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.common.description.DatabaseEntry"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.common.description.DatabaseEntry ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#find(int,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.common.description.DatabaseEntry as databaseEntry where databaseEntry.databaseEntry = :databaseEntry",
                        databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#find(java.lang.String,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.common.description.DatabaseEntry find( final java.lang.String queryString,
            final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        return ( ubic.gemma.model.common.description.DatabaseEntry ) this.find( TRANSFORM_NONE, queryString,
                databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#find(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.common.description.DatabaseEntry find(
            ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        return ( ubic.gemma.model.common.description.DatabaseEntry ) this.find( TRANSFORM_NONE, databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#findByAccession(int, java.lang.String,
     *      java.lang.String, ubic.gemma.model.common.description.ExternalDatabase)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findByAccession( final int transform, final java.lang.String queryString,
            final java.lang.String accession, final ubic.gemma.model.common.description.ExternalDatabase externalDb ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( accession );
        argNames.add( "accession" );
        args.add( externalDb );
        argNames.add( "externalDb" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.common.description.DatabaseEntry"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.common.description.DatabaseEntry ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#findByAccession(int, java.lang.String,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findByAccession( final int transform, final java.lang.String accession,
            final ubic.gemma.model.common.description.ExternalDatabase externalDb ) {
        return this.findByAccession( transform,
                "from DatabaseEntryImpl d where d.accession=:accession and d.externalDatabase=:externalDb", accession,
                externalDb );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#findByAccession(java.lang.String, java.lang.String,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.common.description.DatabaseEntry findByAccession( final java.lang.String queryString,
            final java.lang.String accession, final ubic.gemma.model.common.description.ExternalDatabase externalDb ) {
        return ( ubic.gemma.model.common.description.DatabaseEntry ) this.findByAccession( TRANSFORM_NONE, queryString,
                accession, externalDb );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#findByAccession(java.lang.String,
     *      ubic.gemma.model.common.description.ExternalDatabase)
     */
    public ubic.gemma.model.common.description.DatabaseEntry findByAccession( java.lang.String accession,
            ubic.gemma.model.common.description.ExternalDatabase externalDb ) {
        return ( ubic.gemma.model.common.description.DatabaseEntry ) this.findByAccession( TRANSFORM_NONE, accession,
                externalDb );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.DatabaseEntryImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.DatabaseEntry ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#load(java.lang.Long)
     */
    public ubic.gemma.model.common.description.DatabaseEntry load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.DatabaseEntry ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#loadAll()
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#loadAll(int)
     */
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.DatabaseEntryImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#remove(java.lang.Long)
     */
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
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#remove(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public void remove( ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.remove - 'databaseEntry' can not be null" );
        }
        this.getHibernateTemplate().delete( databaseEntry );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "DatabaseEntry.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.DatabaseEntry ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryDao#update(ubic.gemma.model.common.description.DatabaseEntry)
     */
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

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.DatabaseEntry)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.description.DatabaseEntryDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.DatabaseEntry)
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
     * <code>ubic.gemma.model.common.description.DatabaseEntryDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.description.DatabaseEntryDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.common.description.DatabaseEntry entity ) {
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