/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.util.Map;

import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.genome.Gene;

/**
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService
 */
@Service
public class Gene2GeneCoexpressionServiceImpl extends
        ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceBase {

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(java.util.Collection)
     */
    @Override
    protected Collection<Gene2GeneCoexpression> handleCreate( Collection<Gene2GeneCoexpression> gene2genes ) {

        if ( !this.validCollection( gene2genes ) ) return null;

        return ( Collection<Gene2GeneCoexpression> ) this.getGene2GeneCoexpressionDao().create( gene2genes );

    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#create(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
    protected ubic.gemma.model.association.coexpression.Gene2GeneCoexpression handleCreate(
            ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2gene ) {
        return this.getGene2GeneCoexpressionDao().create( gene2gene );
    }

    /**
     * Performs the core logic for {@link #delete(java.util.Collection)}
     */
    protected void handleDelete( Collection<Gene2GeneCoexpression> deletes ) {
        this.getGene2GeneCoexpressionDao().remove( deletes );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#delete(ubic.gemma.model.association.coexpression.Gene2GeneCoexpression)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression toDelete ) {
        this.getGene2GeneCoexpressionDao().remove( toDelete );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceBase#handleFindCoexpressionRelationships
     * (java.util.Collection, ubic.gemma.model.analysis.Analysis, int)
     */
    @Override
    protected Map<Gene, Collection<Gene2GeneCoexpression>> handleFindCoexpressionRelationships( Collection<Gene> genes,
            int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {
        return this.getGene2GeneCoexpressionDao().findCoexpressionRelationships( genes, stringency, maxResults,
                sourceAnalysis );
    }

    /**
     * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(ubic.gemma.model.genome.Gene,
     *      java.util.Collection)
     */
    @Override
    protected java.util.Collection<Gene2GeneCoexpression> handleFindCoexpressionRelationships(
            ubic.gemma.model.genome.Gene gene, int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis ) {
        return this.getGene2GeneCoexpressionDao().findCoexpressionRelationships( gene, stringency, maxResults,
                sourceAnalysis );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceBase#handleFindInterCoexpressionRelationship
     * (java.util.Collection, ubic.gemma.model.analysis.Analysis, int)
     */
    @Override
    protected Map<Gene, Collection<Gene2GeneCoexpression>> handleFindInterCoexpressionRelationship(
            Collection<Gene> genes, int stringency, GeneCoexpressionAnalysis sourceAnalysis ) {
        return this.getGene2GeneCoexpressionDao()
                .findInterCoexpressionRelationships( genes, stringency, sourceAnalysis );
    }

    private Boolean validCollection( java.util.Collection<Gene2GeneCoexpression> g2gExpressions )
            throws IllegalArgumentException {
        // sanity check.
        if ( ( g2gExpressions == null ) || ( g2gExpressions.size() == 0 ) ) return false;

        // Make sure that the collections passed in is all of the same Class

        Object last = g2gExpressions.iterator().next();

        for ( Object next : g2gExpressions ) {

            if ( last.getClass() != next.getClass() ) {
                throw new IllegalArgumentException(
                        "Given collection doesn't contain objects of uniform type. Contains an object of type "
                                + last.getClass() + " and another of type " + next.getClass() );
            }

            last = next;
        }

        return true;
    }

    @Override
    public Integer getNumberOfLinks( Gene gene, GeneCoexpressionAnalysis analysis ) {
        return this.getGene2GeneCoexpressionDao().getNumberOfLinks( gene, analysis );
    }

}