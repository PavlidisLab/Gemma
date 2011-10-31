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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.CytogeneticLocation</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.CytogeneticLocation
 */
public abstract class CytogeneticLocationDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.CytogeneticLocationDao {

    /**
     * @see ubic.gemma.model.genome.CytogeneticLocationDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( ( ubic.gemma.model.genome.CytogeneticLocation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.CytogeneticLocationDao#create(int transform,
     *      ubic.gemma.model.genome.CytogeneticLocation)
     */
    public CytogeneticLocation create( final ubic.gemma.model.genome.CytogeneticLocation cytogeneticLocation ) {
        if ( cytogeneticLocation == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.create - 'cytogeneticLocation' can not be null" );
        }
        this.getHibernateTemplate().save( cytogeneticLocation );
        return cytogeneticLocation;
    }

    /**
     * @see ubic.gemma.model.genome.CytogeneticLocationDao#load(int, java.lang.Long)
     */
    @Override
    public CytogeneticLocation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.CytogeneticLocationImpl.class,
                id );
        return ( ubic.gemma.model.genome.CytogeneticLocation ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.CytogeneticLocationDao#loadAll(int)
     */
    @Override
    public java.util.Collection loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.CytogeneticLocationImpl.class );

        return results;
    }

    /**
     * @see ubic.gemma.model.genome.CytogeneticLocationDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.CytogeneticLocation entity = ( ubic.gemma.model.genome.CytogeneticLocation ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeLocationDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.CytogeneticLocationDao#remove(ubic.gemma.model.genome.CytogeneticLocation)
     */
    public void remove( ubic.gemma.model.genome.CytogeneticLocation cytogeneticLocation ) {
        if ( cytogeneticLocation == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.remove - 'cytogeneticLocation' can not be null" );
        }
        this.getHibernateTemplate().delete( cytogeneticLocation );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeLocationDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.CytogeneticLocation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.CytogeneticLocationDao#update(ubic.gemma.model.genome.CytogeneticLocation)
     */
    public void update( ubic.gemma.model.genome.CytogeneticLocation cytogeneticLocation ) {
        if ( cytogeneticLocation == null ) {
            throw new IllegalArgumentException( "CytogeneticLocation.update - 'cytogeneticLocation' can not be null" );
        }
        this.getHibernateTemplate().update( cytogeneticLocation );
    }

}