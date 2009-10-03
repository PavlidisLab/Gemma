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
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * A service for caching gene coexpression results
 * </p>
 */
public interface Gene2GeneCoexpressionService {

    /**
     * 
     */
    public ubic.gemma.model.association.coexpression.Gene2GeneCoexpression create(
            ubic.gemma.model.association.coexpression.Gene2GeneCoexpression gene2gene );

    /**
     * 
     */
    public java.util.Collection<Gene2GeneCoexpression> create(
            java.util.Collection<Gene2GeneCoexpression> gene2geneCoexpressions );

    /**
     * 
     */
    public void delete( ubic.gemma.model.association.coexpression.Gene2GeneCoexpression toDelete );

    /**
     * 
     */
    public java.util.Collection<Gene2GeneCoexpression> findCoexpressionRelationships(
            ubic.gemma.model.genome.Gene gene, int stringency, int maxResults, GeneCoexpressionAnalysis sourceAnalysis );

    /**
     * <p>
     * Returns a map of genes to coexpression results.
     * </p>
     */
    public java.util.Map findCoexpressionRelationships( java.util.Collection<Gene> genes, int stringency,
            int maxResults, GeneCoexpressionAnalysis sourceAnalysis );

    /**
     * <p>
     * Return coexpression relationships among the given genes, in a map of query gene to coexpression objects.
     * </p>
     */
    public java.util.Map findInterCoexpressionRelationship( java.util.Collection<Gene> genes, int stringency,
            GeneCoexpressionAnalysis sourceAnalysis );

}
