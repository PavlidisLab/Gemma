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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.analysis.expression.coexpression.QueryGeneCoexpression;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public interface ProbeLinkCoexpressionAnalyzer {

    /**
     * @param genes
     * @param ees Collection of ExpressionExperiments that will be considered.
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @param limit The maximum number of results that will be fully populated. Set to 0 to fill all (batch mode)
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.analysis.expression.coexpression.QueryGeneCoexpression
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    public abstract Map<Gene, QueryGeneCoexpression> linkAnalysis( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, int stringency, boolean interGenesOnly, int limit );

    /**
     * @param gene
     * @param ees Collection of ExpressionExperiments that will be considered.
     * @param inputStringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1
     *        will be silently used.
     * @param limit The maximum number of results that will be fully populated. Set to 0 to fill all (batch mode)
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.analysis.expression.coexpression.QueryGeneCoexpression
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    public abstract QueryGeneCoexpression linkAnalysis( Gene gene, Collection<? extends BioAssaySet> ees,
            int inputStringency, int limit );

}