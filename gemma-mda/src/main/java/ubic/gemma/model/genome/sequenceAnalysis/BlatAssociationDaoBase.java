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
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation
 */
public abstract class BlatAssociationDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao {

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends BlatAssociation> create(
            final java.util.Collection<? extends BlatAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatAssociation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlatAssociation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#create(int transform,
     *      ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    public BlatAssociation create( final ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) {
        if ( blatAssociation == null ) {
            throw new IllegalArgumentException( "BlatAssociation.create - 'blatAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( blatAssociation );
        return blatAssociation;
    }

    @Override
    public Collection<? extends BlatAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from BlatAssociationImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#load(int, java.lang.Long)
     */
    @Override
    public BlatAssociation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlatAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationImpl.class, id );
        return ( ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation ) entity;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#loadAll(int)
     */
    @Override
    public java.util.Collection<BlatAssociation> loadAll() {
        final java.util.Collection<? extends BlatAssociation> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationImpl.class );

        return ( Collection<BlatAssociation> ) results;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BlatAssociation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends BlatAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#remove(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    public void remove( ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) {
        if ( blatAssociation == null ) {
            throw new IllegalArgumentException( "BlatAssociation.remove - 'blatAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( blatAssociation );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final java.util.Collection<BlatAssociation> blatAssociations ) {
        try {
            this.handleThaw( blatAssociations );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao.thaw(java.util.Collection blatAssociations)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#thaw(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    public void thaw( final ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) {
        try {
            this.handleThaw( blatAssociation );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao.thaw(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends BlatAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BlatAssociation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends BlatAssociation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao#update(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    public void update( ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) {
        if ( blatAssociation == null ) {
            throw new IllegalArgumentException( "BlatAssociation.update - 'blatAssociation' can not be null" );
        }
        this.getHibernateTemplate().update( blatAssociation );
    }

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<BlatAssociation> blatAssociations )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)}
     */
    protected abstract void handleThaw( ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation )
            throws java.lang.Exception;

}