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
 * <code>ubic.gemma.model.genome.PhysicalLocation</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.PhysicalLocation
 */
public abstract class PhysicalLocationDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.PhysicalLocationDao {

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.PhysicalLocation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#create(int transform, ubic.gemma.model.genome.PhysicalLocation)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        if ( physicalLocation == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.create - 'physicalLocation' can not be null" );
        }
        this.getHibernateTemplate().save( physicalLocation );
        return physicalLocation;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#create(ubic.gemma.model.genome.PhysicalLocation)
     */
    public ubic.gemma.model.genome.PhysicalLocation create( ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        return ( ubic.gemma.model.genome.PhysicalLocation ) this.create( TRANSFORM_NONE, physicalLocation );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#load(int, java.lang.Long)
     */
    @Override
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.PhysicalLocationImpl.class, id );
        return ( ubic.gemma.model.genome.PhysicalLocation ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.genome.PhysicalLocation load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.PhysicalLocation ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#loadAll()
     */
    @Override
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#loadAll(int)
     */
    @Override
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.PhysicalLocationImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.PhysicalLocation entity = ( ubic.gemma.model.genome.PhysicalLocation ) this.load( id );
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
            throw new IllegalArgumentException( "PhysicalLocation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#remove(ubic.gemma.model.genome.PhysicalLocation)
     */
    public void remove( ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        if ( physicalLocation == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.remove - 'physicalLocation' can not be null" );
        }
        this.getHibernateTemplate().delete( physicalLocation );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeLocationDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.PhysicalLocation ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocationDao#update(ubic.gemma.model.genome.PhysicalLocation)
     */
    public void update( ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        if ( physicalLocation == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.update - 'physicalLocation' can not be null" );
        }
        this.getHibernateTemplate().update( physicalLocation );
    }

}