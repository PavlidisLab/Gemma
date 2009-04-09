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
package ubic.gemma.model.genome.gene;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.CandidateGene</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.gene.CandidateGene
 */
public abstract class CandidateGeneDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.gene.CandidateGeneDao {

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#create(int, java.util.Collection)
     */
    public java.util.Collection<CandidateGene> create( final int transform,
            final java.util.Collection<CandidateGene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CandidateGene.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.gene.CandidateGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#create(int transform,
     *      ubic.gemma.model.genome.gene.CandidateGene)
     */
    public CandidateGene create( final int transform, final ubic.gemma.model.genome.gene.CandidateGene candidateGene ) {
        if ( candidateGene == null ) {
            throw new IllegalArgumentException( "CandidateGene.create - 'candidateGene' can not be null" );
        }
        this.getHibernateTemplate().save( candidateGene );
        return ( CandidateGene ) this.transformEntity( transform, candidateGene );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#create(ubic.gemma.model.genome.gene.CandidateGene)
     */
    public CandidateGene create( ubic.gemma.model.genome.gene.CandidateGene candidateGene ) {
        return this.create( TRANSFORM_NONE, candidateGene );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#load(int, java.lang.Long)
     */

    public CandidateGene load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CandidateGene.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate()
                .get( ubic.gemma.model.genome.gene.CandidateGeneImpl.class, id );
        return ( CandidateGene ) transformEntity( transform, entity );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#load(java.lang.Long)
     */

    public CandidateGene load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#loadAll()
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<CandidateGene> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#loadAll(int)
     */

    public java.util.Collection<CandidateGene> loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.CandidateGeneImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CandidateGene.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.CandidateGene entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<CandidateGene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CandidateGene.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#remove(ubic.gemma.model.genome.gene.CandidateGene)
     */
    public void remove( ubic.gemma.model.genome.gene.CandidateGene candidateGene ) {
        if ( candidateGene == null ) {
            throw new IllegalArgumentException( "CandidateGene.remove - 'candidateGene' can not be null" );
        }
        this.getHibernateTemplate().delete( candidateGene );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<CandidateGene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CandidateGene.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.gene.CandidateGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneDao#update(ubic.gemma.model.genome.gene.CandidateGene)
     */
    public void update( ubic.gemma.model.genome.gene.CandidateGene candidateGene ) {
        if ( candidateGene == null ) {
            throw new IllegalArgumentException( "CandidateGene.update - 'candidateGene' can not be null" );
        }
        this.getHibernateTemplate().update( candidateGene );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.gene.CandidateGene)} method. This method does not instantiate
     * a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.gene.CandidateGeneDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.gene.CandidateGene)
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
     * <code>ubic.gemma.model.genome.gene.CandidateGeneDao</code>, please note that the {@link #TRANSFORM_NONE} constant
     * denotes no transformation, so the entity itself will be returned. If the integer argument value is unknown
     * {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.gene.CandidateGeneDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final Object entity ) {
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