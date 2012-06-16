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

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.measurement.Measurement</code>.
 * 
 * @see ubic.gemma.model.common.measurement.Measurement
 */
public abstract class MeasurementDaoBase extends HibernateDaoSupport implements MeasurementDao {

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends Measurement> create(
            final java.util.Collection<? extends Measurement> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Measurement.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Measurement> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
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
    @Override
    public Measurement create( final ubic.gemma.model.common.measurement.Measurement measurement ) {
        if ( measurement == null ) {
            throw new IllegalArgumentException( "Measurement.create - 'measurement' can not be null" );
        }
        this.getHibernateTemplate().save( measurement );
        return measurement;
    }

    @Override
    public Collection<? extends Measurement> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from MeasurementImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#load(int, java.lang.Long)
     */
    @Override
    public Measurement load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Measurement.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.measurement.MeasurementImpl.class, id );
        return ( Measurement ) entity;
    }

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends Measurement> loadAll() {
        final java.util.Collection<? extends Measurement> results = this.getHibernateTemplate().loadAll(
                MeasurementImpl.class );
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
    public void remove( java.util.Collection<? extends Measurement> entities ) {
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
    public void update( final java.util.Collection<? extends Measurement> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Measurement.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Measurement> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
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

}