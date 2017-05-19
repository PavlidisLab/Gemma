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
package ubic.gemma.persistence.service.genome;

import ubic.gemma.model.genome.Chromosome;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.Chromosome</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.Chromosome
 */
public abstract class ChromosomeDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ChromosomeDao {

    /**
     * @see ChromosomeDao#create(int, Collection)
     */
    @Override
    public Collection<? extends Chromosome> create( final Collection<? extends Chromosome> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Chromosome.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Chromosome> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ChromosomeDao#create(int transform, ubic.gemma.model.genome.Chromosome)
     */
    @Override
    public Chromosome create( final ubic.gemma.model.genome.Chromosome chromosome ) {
        if ( chromosome == null ) {
            throw new IllegalArgumentException( "Chromosome.create - 'chromosome' can not be null" );
        }
        this.getHibernateTemplate().save( chromosome );
        return chromosome;
    }

    /**
     * @see ChromosomeDao#load(int, java.lang.Long)
     */
    @Override
    public Chromosome load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Chromosome.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.ChromosomeImpl.class, id );
        return ( Chromosome ) entity;
    }

    /**
     * @see ChromosomeDao#loadAll(int)
     */
    @Override
    public Collection<? extends Chromosome> loadAll() {
        return this.getHibernateTemplate().loadAll( ubic.gemma.model.genome.ChromosomeImpl.class );
    }

    @Override
    public Collection<Chromosome> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ChromosomeImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ChromosomeDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Chromosome.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.Chromosome entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ChromosomeDao#remove(Collection)
     */
    @Override
    public void remove( Collection<? extends Chromosome> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Chromosome.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ChromosomeDao#remove(ubic.gemma.model.genome.Chromosome)
     */
    @Override
    public void remove( ubic.gemma.model.genome.Chromosome chromosome ) {
        if ( chromosome == null ) {
            throw new IllegalArgumentException( "Chromosome.remove - 'chromosome' can not be null" );
        }
        this.getHibernateTemplate().delete( chromosome );
    }

    /**
     * @see ChromosomeDao#update(Collection)
     */
    @Override
    public void update( final Collection<? extends Chromosome> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Chromosome.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Chromosome> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ChromosomeDao#update(ubic.gemma.model.genome.Chromosome)
     */
    @Override
    public void update( ubic.gemma.model.genome.Chromosome chromosome ) {
        if ( chromosome == null ) {
            throw new IllegalArgumentException( "Chromosome.update - 'chromosome' can not be null" );
        }
        this.getHibernateTemplate().update( chromosome );
    }

}