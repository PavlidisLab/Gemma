/*
 * The Gemma project
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
package ubic.gemma.analysis.diff;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzer extends AbstractAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getSignificantGenes(java.util.Collection)
     */
    @Override
    public Collection<Gene> getSignificantGenes( Collection<ExpressionExperimentSubSet> expressionExperimentSubsets ) {
        // TODO Auto-generated method stub
        return null;
    }

}
