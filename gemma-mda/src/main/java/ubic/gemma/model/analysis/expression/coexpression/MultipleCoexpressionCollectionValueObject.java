/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import ubic.gemma.model.genome.Gene;

/**
 * @author luke The MultipleCoexpressionCollectionValueObject is used for storing the results of a multiple coexpression
 *         search.
 * @version $Id$
 */
public class MultipleCoexpressionCollectionValueObject {

    private int minimumCommonQueryGenes;
    private Collection<Gene> queryGenes;
    private MultipleCoexpressionTypeValueObject geneCoexpressionData;
    private double elapsedWallSeconds;

    public MultipleCoexpressionCollectionValueObject() {
        minimumCommonQueryGenes = 2;
        queryGenes = Collections.synchronizedCollection( new ArrayList<Gene>() );
        geneCoexpressionData = new MultipleCoexpressionTypeValueObject();
    }

    public void addCoexpressionCollection( CoexpressionCollectionValueObject coexpressionCollection ) {
        synchronized ( this ) {
            queryGenes.add( coexpressionCollection.getQueryGene() );
            geneCoexpressionData.addCoexpressionCollection( coexpressionCollection.getQueryGene(),
                    coexpressionCollection.getGeneCoexpression() );
        }
    }

    /**
     * @return those coexpressed genes that are common to multiple query genes
     */
    public Collection<CommonCoexpressionValueObject> getCommonCoexpressedGenes() {
        return this.geneCoexpressionData.getCommonCoexpressedGenes( minimumCommonQueryGenes );
    }

    /**
     * This gives the amount of time we had to wait for the queries (which can be less than the time per query because
     * of threading)
     * 
     * @return
     */
    public double getElapsedWallSeconds() {
        return elapsedWallSeconds;
    }

    /**
     * @return the MultipleCoexpressonTypeValueObject for standard genes
     */
    public MultipleCoexpressionTypeValueObject getGeneCoexpressionType() {
        return this.geneCoexpressionData;
    }

    /**
     * @return the minimum number of query genes with which a result gene must exhibit coexpression to be displayed
     */
    public int getMinimumCommonQueries() {
        return minimumCommonQueryGenes;
    }

    /**
     * @return the number of Genes
     */
    public int getNumGenes() {
        return this.geneCoexpressionData.getNumberOfGenes();
    }

    /**
     * @param gene the Gene of interest
     * @return a subset of the geneCommonCoexpressionValueObjects that exhibit coexpression with the specified Gene
     */
    public CommonCoexpressionValueObject getQueriesForGene( Gene gene ) {
        return this.geneCoexpressionData.getQueriesForGene( gene );
    }

    /**
     * @return the query genes
     */
    public Collection<Gene> getQueryGenes() {
        // return queries.keySet();
        return queryGenes;
    }

    /**
     * Set the amount of time we had to wait for the queries (which can be less than the time per query because
     * 
     * @param elapsedWallTime (in milliseconds)
     */
    public void setElapsedWallTimeElapsed( double elapsedWallMillisSeconds ) {
        this.elapsedWallSeconds = elapsedWallMillisSeconds / 1000.0;
    }

    /**
     * @param minimumCommonQueryGenes the minimum number of query genes with which a result gene must exhibit
     *        coexpression to be displayed
     */
    public void setMinimumCommonQueries( int minimumCommonQueryGenes ) {
        this.minimumCommonQueryGenes = minimumCommonQueryGenes;
    }
}
