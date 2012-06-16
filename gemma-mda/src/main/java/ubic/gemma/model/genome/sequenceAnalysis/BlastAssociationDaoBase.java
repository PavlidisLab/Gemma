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
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation</code>.
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation
 */
public abstract class BlastAssociationDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao {

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends BlastAssociation> create(
            final java.util.Collection<? extends BlastAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlastAssociation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlastAssociation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao#create(int transform,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation)
     */
    @Override
    public BlastAssociation create( final ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation blastAssociation ) {
        if ( blastAssociation == null ) {
            throw new IllegalArgumentException( "BlastAssociation.create - 'blastAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( blastAssociation );
        return blastAssociation;
    }

    @Override
    public Collection<? extends BlastAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "from BlastAssociationImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao#load(int, java.lang.Long)
     */
    @Override
    public BlastAssociation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlastAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationImpl.class, id );
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends BlastAssociation> loadAll() {
        final java.util.Collection<? extends BlastAssociation> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlastAssociation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends BlastAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlastAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao#remove(ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation)
     */
    @Override
    public void remove( ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation blastAssociation ) {
        if ( blastAssociation == null ) {
            throw new IllegalArgumentException( "BlastAssociation.remove - 'blastAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( blastAssociation );
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends BlastAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlastAssociation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlastAssociation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao#update(ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation)
     */
    @Override
    public void update( ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation blastAssociation ) {
        if ( blastAssociation == null ) {
            throw new IllegalArgumentException( "BlastAssociation.update - 'blastAssociation' can not be null" );
        }
        this.getHibernateTemplate().update( blastAssociation );
    }

}