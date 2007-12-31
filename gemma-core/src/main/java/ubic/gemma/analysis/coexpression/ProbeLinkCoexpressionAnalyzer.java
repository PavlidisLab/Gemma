/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.coexpression;

import java.util.Collection;

import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * Perform probe-to-probe coexpression link analysis ("TMM-style").
 * 
 * @spring.bean id="probeLinkCoexpressionAnalyzer"
 * @spring.property name="geneService" ref="geneService"
 * @author paul
 * @version $Id$
 */
public class ProbeLinkCoexpressionAnalyzer {

    GeneService geneService;

    /**
     * @param gene
     * @param ees
     * @param stringency A positive non-zero integer. If a value less than or equal to zero is entered, the value 1 will
     *        be silently used.
     * @see ubic.gemma.model.genome.GeneDao.getCoexpressedGenes
     * @see ubic.gemma.model.coexpression.CoexpressionCollectionValueObject
     * @return Fully initialized CoexpressionCollectionValueObject.
     */
    public CoexpressionCollectionValueObject linkAnalysis( Gene gene, Collection<ExpressionExperiment> ees,
            int stringency ) {

        if ( stringency <= 0 ) stringency = 1;

        CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                .getCoexpressedGenes( gene, ees, stringency );

        return coexpressions;
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

}
