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

import ubic.gemma.analysis.util.RCommander;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractAnalyzer extends RCommander {

    /**
     * Returns the significant genes for the implementing analyzer.
     * 
     * @param expressionExperiment The {@link ExpressionExperiment} of interest.
     * @param expressionExperimentSubsets If null, subsets are ignored.
     * @return Collection<Gene> Stored internally as a list of genes ordered by p-value.
     */
    public abstract Collection<Gene> getSignificantGenes( ExpressionExperiment expressionExperiment,
            Collection<ExpressionExperimentSubSet> expressionExperimentSubsets );
}
