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
package ubic.gemma.model.genome;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.PhysicalMarker</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.PhysicalMarker
 */
public abstract class PhysicalMarkerDaoBase extends ubic.gemma.model.genome.ChromosomeFeatureDaoImpl<PhysicalMarker>
        implements ubic.gemma.model.genome.PhysicalMarkerDao {

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#create(int, java.util.Collection)
     */
    public java.util.Collection<PhysicalMarker> create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.PhysicalMarker ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#create(int transform, ubic.gemma.model.genome.PhysicalMarker)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.PhysicalMarker physicalMarker ) {
        if ( physicalMarker == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.create - 'physicalMarker' can not be null" );
        }
        this.getHibernateTemplate().save( physicalMarker );
        return this.transformEntity( transform, physicalMarker );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#create(ubic.gemma.model.genome.PhysicalMarker)
     */
    public PhysicalMarker create( ubic.gemma.model.genome.PhysicalMarker physicalMarker ) {
        return ( ubic.gemma.model.genome.PhysicalMarker ) this.create( TRANSFORM_NONE, physicalMarker );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByNcbiId(int, java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> findByNcbiId( final int transform, final java.lang.String ncbiId ) {
        return this.findByNcbiId( transform, "from GeneImpl g where g.ncbiId = :ncbiId", ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> findByNcbiId( final int transform, final java.lang.String queryString,
            final java.lang.String ncbiId ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( ncbiId );
        argNames.add( "ncbiId" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByNcbiId(java.lang.String)
     */

    public java.util.Collection<PhysicalMarker> findByNcbiId( java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByNcbiId(java.lang.String, java.lang.String)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> findByNcbiId( final java.lang.String queryString,
            final java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, queryString, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> findByPhysicalLocation( final int transform,
            final java.lang.String queryString, final ubic.gemma.model.genome.PhysicalLocation location ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( location );
        argNames.add( "location" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByPhysicalLocation(int,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> findByPhysicalLocation( final int transform,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this
                .findByPhysicalLocation(
                        transform,
                        "from ubic.gemma.model.genome.PhysicalMarker as physicalMarker where physicalMarker.location = :location",
                        location );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByPhysicalLocation(java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, queryString, location );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#findByPhysicalLocation(ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection<PhysicalMarker> findByPhysicalLocation(
            ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, location );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.PhysicalMarkerImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.PhysicalMarker ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#load(java.lang.Long)
     */

    public PhysicalMarker load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.PhysicalMarker ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#load(java.util.Collection)
     */
    public java.util.Collection<PhysicalMarker> load( final java.util.Collection ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#loadAll()
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<PhysicalMarker> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#loadAll(int)
     */

    public java.util.Collection<PhysicalMarker> loadAll( final int transform ) {
        final java.util.Collection<PhysicalMarker> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.PhysicalMarkerImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.PhysicalMarker entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<PhysicalMarker> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#remove(ubic.gemma.model.genome.PhysicalMarker)
     */
    public void remove( ubic.gemma.model.genome.PhysicalMarker physicalMarker ) {
        if ( physicalMarker == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.remove - 'physicalMarker' can not be null" );
        }
        this.getHibernateTemplate().delete( physicalMarker );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<PhysicalMarker> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.PhysicalMarker ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalMarkerDao#update(ubic.gemma.model.genome.PhysicalMarker)
     */
    public void update( ubic.gemma.model.genome.PhysicalMarker physicalMarker ) {
        if ( physicalMarker == null ) {
            throw new IllegalArgumentException( "PhysicalMarker.update - 'physicalMarker' can not be null" );
        }
        this.getHibernateTemplate().update( physicalMarker );
    }

    protected abstract Collection<PhysicalMarker> handleLoad( Collection<Long> ids );

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.PhysicalMarker)} method. This method does not instantiate a
     * new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.PhysicalMarkerDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.PhysicalMarker)
     */

    @Override
    protected void transformEntities( final int transform, final java.util.Collection<PhysicalMarker> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.genome.PhysicalMarkerDao</code>, please note that the {@link #TRANSFORM_NONE} constant
     * denotes no transformation, so the entity itself will be returned. If the integer argument value is unknown
     * {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.PhysicalMarkerDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.PhysicalMarker entity ) {
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