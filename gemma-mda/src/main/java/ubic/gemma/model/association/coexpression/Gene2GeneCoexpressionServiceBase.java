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

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService
 */
public abstract class Gene2GeneCoexpressionServiceBase implements
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService {

    private ubic.gemma.model.association.coexpression.RatGeneCoExpressionDao ratGeneCoExpressionDao;

    /**
     * Sets the reference to <code>ratGeneCoExpression</code>'s DAO.
     */
    public void setRatGeneCoExpressionDao(
            ubic.gemma.model.association.coexpression.RatGeneCoExpressionDao ratGeneCoExpressionDao ) {
        this.ratGeneCoExpressionDao = ratGeneCoExpressionDao;
    }

    /**
     * Gets the reference to <code>ratGeneCoExpression</code>'s DAO.
     */
    protected ubic.gemma.model.association.coexpression.RatGeneCoExpressionDao getRatGeneCoExpressionDao() {
        return this.ratGeneCoExpressionDao;
    }

    private ubic.gemma.model.association.coexpression.OtherGeneCoExpressionDao otherGeneCoExpressionDao;

    /**
     * Sets the reference to <code>otherGeneCoExpression</code>'s DAO.
     */
    public void setOtherGeneCoExpressionDao(
            ubic.gemma.model.association.coexpression.OtherGeneCoExpressionDao otherGeneCoExpressionDao ) {
        this.otherGeneCoExpressionDao = otherGeneCoExpressionDao;
    }

    /**
     * Gets the reference to <code>otherGeneCoExpression</code>'s DAO.
     */
    protected ubic.gemma.model.association.coexpression.OtherGeneCoExpressionDao getOtherGeneCoExpressionDao() {
        return this.otherGeneCoExpressionDao;
    }

    private ubic.gemma.model.association.coexpression.HumanGeneCoExpressionDao humanGeneCoExpressionDao;

    /**
     * Sets the reference to <code>humanGeneCoExpression</code>'s DAO.
     */
    public void setHumanGeneCoExpressionDao(
            ubic.gemma.model.association.coexpression.HumanGeneCoExpressionDao humanGeneCoExpressionDao ) {
        this.humanGeneCoExpressionDao = humanGeneCoExpressionDao;
    }

    /**
     * Gets the reference to <code>humanGeneCoExpression</code>'s DAO.
     */
    protected ubic.gemma.model.association.coexpression.HumanGeneCoExpressionDao getHumanGeneCoExpressionDao() {
        return this.humanGeneCoExpressionDao;
    }

    private ubic.gemma.model.association.coexpression.MouseGeneCoExpressionDao mouseGeneCoExpressionDao;

    /**
     * Sets the reference to <code>mouseGeneCoExpression</code>'s DAO.
     */
    public void setMouseGeneCoExpressionDao(
            ubic.gemma.model.association.coexpression.MouseGeneCoExpressionDao mouseGeneCoExpressionDao ) {
        this.mouseGeneCoExpressionDao = mouseGeneCoExpressionDao;
    }

    /**
     * Gets the reference to <code>mouseGeneCoExpression</code>'s DAO.
     */
    protected ubic.gemma.model.association.coexpression.MouseGeneCoExpressionDao getMouseGeneCoExpressionDao() {
        return this.mouseGeneCoExpressionDao;
    }

    private ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao gene2GeneCoexpressionDao;

    /**
     * Sets the reference to <code>gene2GeneCoexpression</code>'s DAO.
     */
    public void setGene2GeneCoexpressionDao(
            ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao gene2GeneCoexpressionDao ) {
        this.gene2GeneCoexpressionDao = gene2GeneCoexpressionDao;
    }

    /**
     * Gets the reference to <code>gene2GeneCoexpression</code>'s DAO.
     */
    protected ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionDao getGene2GeneCoexpressionDao() {
        return this.gene2GeneCoexpressionDao;
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    public ubic.gemma.model.association.coexpression.Gene2GeneCoexpression create(
            final ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2gene ) {
        try {
            return this.handleCreate( gene2gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService.create(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2gene)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)}
     */
    protected abstract ubic.gemma.model.association.coexpression.Gene2GeneCoexpression handleCreate(
            ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2gene ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(java.util.Collection)
     */
    public java.util.Collection create( final java.util.Collection gene2geneCoexpressions ) {
        try {
            return this.handleCreate( gene2geneCoexpressions );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService.create(java.util.Collection gene2geneCoexpressions)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection handleCreate( java.util.Collection gene2geneCoexpressions )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#delete(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    public void delete( final ubic.gemma.model.association.coexpression.Gene2GeneCoexpression toDelete ) {
        try {
            this.handleDelete( toDelete );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService.delete(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression toDelete)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)}
     */
    protected abstract void handleDelete( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression toDelete )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(ubic.gemma.model.genome.Gene,
     *      int, int)
     */
    public java.util.Collection findCoexpressionRelationships( final ubic.gemma.model.genome.Gene gene,
            final int stringency, final int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {
        try {
            return this.handleFindCoexpressionRelationships( gene, stringency, maxResults, sourceAnalysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService.findCoexpressionRelationships(ubic.gemma.model.genome.Gene gene, int stringency, int maxResults)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #findCoexpressionRelationships(ubic.gemma.model.genome.Gene, int, int)}
     */
    protected abstract java.util.Collection handleFindCoexpressionRelationships( ubic.gemma.model.genome.Gene gene,
            int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(java.util.Collection,
     *      int, int)
     */
    public java.util.Map findCoexpressionRelationships( final java.util.Collection genes, final int stringency,
            final int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {
        try {
            return this.handleFindCoexpressionRelationships( genes, stringency, maxResults, sourceAnalysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService.findCoexpressionRelationships(java.util.Collection genes, int stringency, int maxResults)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #findCoexpressionRelationships(java.util.Collection, int, int)}
     */
    protected abstract java.util.Map handleFindCoexpressionRelationships( java.util.Collection genes, int stringency,
            int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findInterCoexpressionRelationship(java.util.Collection,
     *      int)
     */
    public java.util.Map findInterCoexpressionRelationship( final java.util.Collection genes, final int stringency,
            GeneCoexpressionAnalysis sourceAnalysis ) {
        try {
            return this.handleFindInterCoexpressionRelationship( genes, stringency, sourceAnalysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceException(
                    "Error performing 'ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService.findInterCoexpressionRelationship(java.util.Collection genes, int stringency)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #findInterCoexpressionRelationship(java.util.Collection, int)}
     */
    protected abstract java.util.Map handleFindInterCoexpressionRelationship( java.util.Collection genes,
            int stringency, GeneCoexpressionAnalysis sourceAnalysis ) throws java.lang.Exception;

}