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

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;

/**
 * Spring Service base class for <code>ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService</code>,
 * provides access to all services and entities referenced by this service.
 * 
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService
 */
public abstract class Gene2GeneCoexpressionServiceBase implements
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService {

    @Autowired
    Gene2GeneCoexpressionDao gene2GeneCoexpressionDao;

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(java.util.Collection)
     */
    @Override
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
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
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
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#delete(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
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
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(java.util.Collection,
     *      int, int)
     */
    @Override
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
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(ubic.gemma.model.genome.Gene,
     *      int, int)
     */
    @Override
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
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findInterCoexpressionRelationship(java.util.Collection,
     *      int)
     */
    @Override
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
     * @return the gene2GeneCoexpressionDao
     */
    public Gene2GeneCoexpressionDao getGene2GeneCoexpressionDao() {
        return gene2GeneCoexpressionDao;
    }

    /**
     * @param gene2GeneCoexpressionDao the gene2GeneCoexpressionDao to set
     */
    public void setGene2GeneCoexpressionDao( Gene2GeneCoexpressionDao gene2GeneCoexpressionDao ) {
        this.gene2GeneCoexpressionDao = gene2GeneCoexpressionDao;
    }

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection handleCreate( java.util.Collection gene2geneCoexpressions )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)}
     */
    protected abstract ubic.gemma.model.association.coexpression.Gene2GeneCoexpression handleCreate(
            ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2gene ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)}
     */
    protected abstract void handleDelete( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression toDelete )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findCoexpressionRelationships(java.util.Collection, int, int)}
     */
    protected abstract java.util.Map handleFindCoexpressionRelationships( java.util.Collection genes, int stringency,
            int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findCoexpressionRelationships(ubic.gemma.model.genome.Gene, int, int)}
     */
    protected abstract java.util.Collection handleFindCoexpressionRelationships( ubic.gemma.model.genome.Gene gene,
            int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findInterCoexpressionRelationship(java.util.Collection, int)}
     */
    protected abstract java.util.Map handleFindInterCoexpressionRelationship( java.util.Collection genes,
            int stringency, GeneCoexpressionAnalysis sourceAnalysis ) throws java.lang.Exception;

}