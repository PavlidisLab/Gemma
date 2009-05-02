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

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression</code>.
 * </p>
 * 
 * @version $Id$
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 */
public abstract class Probe2ProbeCoexpressionDaoBase extends ubic.gemma.model.association.RelationshipDaoImpl implements
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao {

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#load(int, java.lang.Long)
     */
    @Override
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression ) entity );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.association.Relationship load( java.lang.Long id ) {
        return ( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#update(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    public void update( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression probe2ProbeCoexpression ) {
        if ( probe2ProbeCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Probe2ProbeCoexpression.update - 'probe2ProbeCoexpression' can not be null" );
        }
        this.getHibernateTemplate().update( probe2ProbeCoexpression );
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#remove(ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    public void remove( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression probe2ProbeCoexpression ) {
        if ( probe2ProbeCoexpression == null ) {
            throw new IllegalArgumentException(
                    "Probe2ProbeCoexpression.remove - 'probe2ProbeCoexpression' can not be null" );
        }
        this.getHibernateTemplate().delete( probe2ProbeCoexpression );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.remove - 'id' can not be null" );
        }
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression entity = ( ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getVectorsForLinks(ubic.gemma.model.genome.Gene,
     *      java.util.Collection)
     */
    public java.util.Collection getVectorsForLinks( final ubic.gemma.model.genome.Gene gene,
            final java.util.Collection<ExpressionExperiment> ees ) {
        try {
            return this.handleGetVectorsForLinks( gene, ees );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getVectorsForLinks(ubic.gemma.model.genome.Gene gene, java.util.Collection ees)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #getVectorsForLinks(ubic.gemma.model.genome.Gene, java.util.Collection)}
     */
    protected abstract java.util.Collection handleGetVectorsForLinks( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<ExpressionExperiment> ees ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public void deleteLinks( final ubic.gemma.model.expression.experiment.ExpressionExperiment ee ) {
        try {
            this.handleDeleteLinks( ee );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment ee)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #deleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract void handleDeleteLinks( ubic.gemma.model.expression.experiment.ExpressionExperiment ee )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public java.lang.Integer countLinks(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleCountLinks( expressionExperiment );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #countLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)}
     */
    protected abstract java.lang.Integer handleCountLinks(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getVectorsForLinks(java.util.Collection,
     *      java.util.Collection)
     */
    public java.util.Map getVectorsForLinks( final java.util.Collection genes, final java.util.Collection ees ) {
        try {
            return this.handleGetVectorsForLinks( genes, ees );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getVectorsForLinks(java.util.Collection genes, java.util.Collection ees)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #getVectorsForLinks(java.util.Collection, java.util.Collection)}
     */
    protected abstract java.util.Map handleGetVectorsForLinks( java.util.Collection<Gene> genes, java.util.Collection<ExpressionExperiment> ees )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.lang.String, boolean)
     */
    public java.util.Collection getProbeCoExpression(
            final ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            final java.lang.String taxonCommonName, final boolean useWorkingTable ) {
        try {
            return this.handleGetProbeCoExpression( expressionExperiment, taxonCommonName, useWorkingTable );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment, java.lang.String taxonCommonName, boolean useWorkingTable)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for
     * {@link #getProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment, java.lang.String, boolean)}
     */
    protected abstract java.util.Collection handleGetProbeCoExpression(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment,
            java.lang.String taxonCommonName, boolean useWorkingTable ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#prepareForShuffling(java.util.Collection,
     *      java.lang.String, boolean)
     */
    public void prepareForShuffling( final java.util.Collection<ExpressionExperiment> ees,
            final java.lang.String taxon, final boolean filterNonSpecific ) {
        try {
            this.handlePrepareForShuffling( ees, taxon, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.prepareForShuffling(java.util.Collection ees, java.lang.String taxon, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #prepareForShuffling(java.util.Collection, java.lang.String, boolean)}
     */
    protected abstract void handlePrepareForShuffling( java.util.Collection<ExpressionExperiment> ees,
            java.lang.String taxon, boolean filterNonSpecific ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, boolean)
     */
    public java.util.Collection<BioAssaySet> getExpressionExperimentsLinkTestedIn(
            final ubic.gemma.model.genome.Gene gene, final java.util.Collection<BioAssaySet> expressionExperiments,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsLinkTestedIn( gene, expressionExperiments, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene gene, java.util.Collection expressionExperiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, java.util.Collection, boolean)}
     */
    protected abstract java.util.Collection<BioAssaySet> handleGetExpressionExperimentsLinkTestedIn(
            ubic.gemma.model.genome.Gene gene, java.util.Collection<BioAssaySet> expressionExperiments,
            boolean filterNonSpecific ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, java.util.Collection, boolean)
     */
    public java.util.Map getExpressionExperimentsLinkTestedIn( final ubic.gemma.model.genome.Gene geneA,
            final java.util.Collection<Long> genesB, final java.util.Collection<BioAssaySet> expressionExperiments,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsLinkTestedIn( geneA, genesB, expressionExperiments,
                    filterNonSpecific );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene geneA, java.util.Collection genesB, java.util.Collection expressionExperiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene, java.util.Collection, java.util.Collection, boolean)}
     */
    protected abstract java.util.Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsLinkTestedIn(
            ubic.gemma.model.genome.Gene geneA, java.util.Collection<Long> genesB,
            java.util.Collection<BioAssaySet> expressionExperiments, boolean filterNonSpecific )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#create(java.util.List)
     */
    public java.util.List create( final java.util.List links ) {
        try {
            return this.handleCreate( links );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.create(java.util.List links)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #create(java.util.List)}
     */
    protected abstract java.util.List handleCreate( java.util.List links ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet,
     *      boolean)
     */
    public java.util.Collection getGenesTestedBy(
            final ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment,
            final boolean filterNonSpecific ) {
        try {
            return this.handleGetGenesTestedBy( expressionExperiment, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for
     * {@link #getGenesTestedBy(ubic.gemma.model.expression.experiment.BioAssaySet, boolean)}
     */
    protected abstract java.util.Collection handleGetGenesTestedBy(
            ubic.gemma.model.expression.experiment.BioAssaySet expressionExperiment, boolean filterNonSpecific )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getExpressionExperimentsTestedIn(java.util.Collection,
     *      java.util.Collection, boolean)
     */
    public java.util.Map getExpressionExperimentsTestedIn( final java.util.Collection geneIds,
            final java.util.Collection experiments, final boolean filterNonSpecific ) {
        try {
            return this.handleGetExpressionExperimentsTestedIn( geneIds, experiments, filterNonSpecific );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao.getExpressionExperimentsTestedIn(java.util.Collection geneIds, java.util.Collection experiments, boolean filterNonSpecific)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for
     * {@link #getExpressionExperimentsTestedIn(java.util.Collection, java.util.Collection, boolean)}
     */
    protected abstract java.util.Map handleGetExpressionExperimentsTestedIn( java.util.Collection<Long> geneIds,
            java.util.Collection<BioAssaySet> experiments, boolean filterNonSpecific ) throws java.lang.Exception;

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression entity ) {
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

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)} method. This
     * method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression)
     */
    @Override
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

}