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
package ubic.gemma.model.association.coexpression;

import java.util.Collection;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.genome.Gene;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.coexpression.Gene2GeneCoexpression</code>.
 * 
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpression
 */
public abstract class Gene2GeneCoexpressionDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao {

    @Autowired
    private Gene2GeneCoexpressionCache gene2GeneCoexpressionCache;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.util.Collection)
     */
    @Override
    public Collection<? extends Gene2GeneCoexpression> create(
            final Collection<? extends Gene2GeneCoexpression> entities ) {

        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( Session session ) throws HibernateException {
                int numDone = 0;

                for ( Gene2GeneCoexpression object : entities ) {
                    session.save( object );

                    if ( ++numDone % 1000 == 0 ) {
                        session.flush();
                        session.clear();
                    }
                }
                return null;
            }
        } );

        return entities;
    }

    @Override
    public Collection<? extends Gene2GeneCoexpression> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from Gene2GeneCoexpressionImpl where id in (:ids)",
                "ids", ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    @Override
    public Gene2GeneCoexpression create( Gene2GeneCoexpression entity ) {
        this.getHibernateTemplate().save( entity );
        return entity;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#findCoexpressionRelationships(java.util.Collection,
     *      int, int)
     */
    @Override
    public java.util.Map<Long, Collection<Gene2GeneCoexpression>> findCoexpressionRelationships(
            final java.util.Collection<Gene> genes, final int stringency, final int maxResults,
            GeneCoexpressionAnalysis sourceAnalysis ) {
        return this.handleFindCoexpressionRelationships( genes, stringency, maxResults, sourceAnalysis );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#findCoexpressionRelationships(ubic.gemma.model.genome.Gene,
     *      int, int)
     */
    @Override
    public java.util.Collection<Gene2GeneCoexpression> findCoexpressionRelationships(
            final ubic.gemma.model.genome.Gene gene, final int stringency, final int maxResults,
            GeneCoexpressionAnalysis sourceAnalysis ) {
        return this.handleFindCoexpressionRelationships( gene, stringency, maxResults, sourceAnalysis );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#findInterCoexpressionRelationships(java.util.Collection,
     *      int)
     */
    @Override
    public java.util.Map<Long, Collection<Gene2GeneCoexpression>> findInterCoexpressionRelationships(
            final java.util.Collection<Gene> genes, final int stringency, GeneCoexpressionAnalysis sourceAnalysis ) {
        return this.handleFindInterCoexpressionRelationships( genes, stringency, sourceAnalysis );
    }

    /**
     * @return the gene2GeneCoexpressionCache
     */
    public Gene2GeneCoexpressionCache getGene2GeneCoexpressionCache() {
        return gene2GeneCoexpressionCache;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#load(int, java.lang.Long)
     */
    @Override
    public Gene2GeneCoexpression load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GeneCoexpression.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.association.coexpression.Gene2GeneCoexpression.class, id );
        return ( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression ) entity;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends Gene2GeneCoexpression> loadAll() {
        return this.getHibernateTemplate().loadAll(
                ubic.gemma.model.association.coexpression.Gene2GeneCoexpression.class );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GeneCoexpression.remove - 'id' can not be null" );
        }
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpression entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends Gene2GeneCoexpression> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GeneCoexpression.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#remove(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
    public void remove( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2GeneCoexpression ) {
        if ( gene2GeneCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Gene2GeneCoexpression.remove - 'gene2GeneCoexpression' can not be null" );
        }
        this.getHibernateTemplate().delete( gene2GeneCoexpression );
    }

    /**
     * @param gene2GeneCoexpressionCache the gene2GeneCoexpressionCache to set
     */
    public void setGene2GeneCoexpressionCache( Gene2GeneCoexpressionCache gene2GeneCoexpressionCache ) {
        this.gene2GeneCoexpressionCache = gene2GeneCoexpressionCache;
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends Gene2GeneCoexpression> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GeneCoexpression.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Gene2GeneCoexpression> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao#update(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
    public void update( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2GeneCoexpression ) {
        if ( gene2GeneCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Gene2GeneCoexpression.update - 'gene2GeneCoexpression' can not be null" );
        }
        this.getHibernateTemplate().update( gene2GeneCoexpression );
    }

    /**
     * Performs the core logic for {@link #findCoexpressionRelationships(java.util.Collection, int, int)}
     */
    protected abstract java.util.Map<Long, Collection<Gene2GeneCoexpression>> handleFindCoexpressionRelationships(
            java.util.Collection<Gene> genes, int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis );

    /**
     * Performs the core logic for {@link #findCoexpressionRelationships(ubic.gemma.model.genome.Gene, int, int)}
     */
    protected abstract java.util.Collection<Gene2GeneCoexpression> handleFindCoexpressionRelationships(
            ubic.gemma.model.genome.Gene gene, int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis );

    /**
     * Performs the core logic for {@link #findCoexpressionRelationships(java.util.Collection, int, int)}
     */
    protected abstract java.util.Map<Long, Collection<Gene2GeneCoexpression>> handleFindInterCoexpressionRelationships(
            java.util.Collection<Gene> genes, int stringency, GeneCoexpressionAnalysis sourceAnalysis );

}