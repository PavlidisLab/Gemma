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
 * <code>ubic.gemma.model.genome.gene.BibReferenceCandidateGene</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGene
 */
public abstract class BibReferenceCandidateGeneDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao {

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#create(int, java.util.Collection)
     */

    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibReferenceCandidateGene.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform,
                                    ( ubic.gemma.model.genome.gene.BibReferenceCandidateGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#create(int transform,
     *      ubic.gemma.model.genome.gene.BibReferenceCandidateGene)
     */
    public BibReferenceCandidateGene create( final int transform,
            final ubic.gemma.model.genome.gene.BibReferenceCandidateGene bibReferenceCandidateGene ) {
        if ( bibReferenceCandidateGene == null ) {
            throw new IllegalArgumentException(
                    "BibReferenceCandidateGene.create - 'bibReferenceCandidateGene' can not be null" );
        }
        this.getHibernateTemplate().save( bibReferenceCandidateGene );
        return ( BibReferenceCandidateGene ) this.transformEntity( transform, bibReferenceCandidateGene );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#create(java.util.Collection)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<BibReferenceCandidateGene> create(
            final java.util.Collection<BibReferenceCandidateGene> entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#create(ubic.gemma.model.genome.gene.BibReferenceCandidateGene)
     */
    public BibReferenceCandidateGene create(
            ubic.gemma.model.genome.gene.BibReferenceCandidateGene bibReferenceCandidateGene ) {
        return this.create( TRANSFORM_NONE, bibReferenceCandidateGene );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#load(int, java.lang.Long)
     */

    public BibReferenceCandidateGene load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BibReferenceCandidateGene.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.gene.BibReferenceCandidateGeneImpl.class, id );
        return ( BibReferenceCandidateGene ) transformEntity( transform,
                ( ubic.gemma.model.genome.gene.BibReferenceCandidateGene ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#load(java.lang.Long)
     */

    public BibReferenceCandidateGene load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#loadAll()
     */

    public java.util.Collection<BibReferenceCandidateGene> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#loadAll(int)
     */

    public java.util.Collection<BibReferenceCandidateGene> loadAll( final int transform ) {
        final java.util.Collection<BibReferenceCandidateGene> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.BibReferenceCandidateGeneImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BibReferenceCandidateGene.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.BibReferenceCandidateGene entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<BibReferenceCandidateGene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibReferenceCandidateGene.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#remove(ubic.gemma.model.genome.gene.BibReferenceCandidateGene)
     */
    public void remove( ubic.gemma.model.genome.gene.BibReferenceCandidateGene bibReferenceCandidateGene ) {
        if ( bibReferenceCandidateGene == null ) {
            throw new IllegalArgumentException(
                    "BibReferenceCandidateGene.remove - 'bibReferenceCandidateGene' can not be null" );
        }
        this.getHibernateTemplate().delete( bibReferenceCandidateGene );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<BibReferenceCandidateGene> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibReferenceCandidateGene.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.gene.BibReferenceCandidateGene ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao#update(ubic.gemma.model.genome.gene.BibReferenceCandidateGene)
     */
    public void update( ubic.gemma.model.genome.gene.BibReferenceCandidateGene bibReferenceCandidateGene ) {
        if ( bibReferenceCandidateGene == null ) {
            throw new IllegalArgumentException(
                    "BibReferenceCandidateGene.update - 'bibReferenceCandidateGene' can not be null" );
        }
        this.getHibernateTemplate().update( bibReferenceCandidateGene );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.gene.BibReferenceCandidateGene)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.gene.BibReferenceCandidateGene)
     */

    protected void transformEntities( final int transform,
            final java.util.Collection<BibReferenceCandidateGene> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.genome.gene.BibReferenceCandidateGeneDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.genome.gene.BibReferenceCandidateGene entity ) {
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