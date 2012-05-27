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
package ubic.gemma.model.common.measurement;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.measurement.Measurement</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.measurement.Measurement
 */
public abstract class MeasurementDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.common.measurement.MeasurementDao {

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Measurement.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.common.measurement.Measurement ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#create(int transform,
     *      ubic.gemma.model.common.measurement.Measurement)
     */
    public Object create( final int transform, final ubic.gemma.model.common.measurement.Measurement measurement ) {
        if ( measurement == null ) {
            throw new IllegalArgumentException( "Measurement.create - 'measurement' can not be null" );
        }
        this.getHibernateTemplate().save( measurement );
        return this.transformEntity( transform, measurement );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#create(java.util.Collection)
     */
    
    @Override
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#create(ubic.gemma.model.common.measurement.Measurement)
     */
    @Override
    public ubic.gemma.model.common.measurement.Measurement create(
            ubic.gemma.model.common.measurement.Measurement measurement ) {
        return ( ubic.gemma.model.common.measurement.Measurement ) this.create( TRANSFORM_NONE, measurement );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Measurement.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.measurement.MeasurementImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.measurement.Measurement ) entity );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.common.measurement.Measurement load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.measurement.Measurement ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#loadAll()
     */
    
    @Override
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#loadAll(int)
     */
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.measurement.MeasurementImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Measurement.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.measurement.Measurement entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Measurement.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#remove(ubic.gemma.model.common.measurement.Measurement)
     */
    @Override
    public void remove( ubic.gemma.model.common.measurement.Measurement measurement ) {
        if ( measurement == null ) {
            throw new IllegalArgumentException( "Measurement.remove - 'measurement' can not be null" );
        }
        this.getHibernateTemplate().delete( measurement );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Measurement.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.measurement.Measurement ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#update(ubic.gemma.model.common.measurement.Measurement)
     */
    @Override
    public void update( ubic.gemma.model.common.measurement.Measurement measurement ) {
        if ( measurement == null ) {
            throw new IllegalArgumentException( "Measurement.update - 'measurement' can not be null" );
        }
        this.getHibernateTemplate().update( measurement );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.measurement.Measurement)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.measurement.MeasurementDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.measurement.Measurement)
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
     * <code>ubic.gemma.model.common.measurement.MeasurementDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.measurement.MeasurementDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.measurement.Measurement entity ) {
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