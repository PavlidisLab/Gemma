/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * An analysis of changes in expression levels across experimental conditions
 */
public abstract class DifferentialExpressionAnalysis extends SingleExperimentAnalysis {

    /**
     * 
     */
    private static final long serialVersionUID = -7855180617739271699L;

    /**
     * Constructs new instances of {@link DifferentialExpressionAnalysis}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link DifferentialExpressionAnalysis} .
         */
        public static DifferentialExpressionAnalysis newInstance() {
            return new DifferentialExpressionAnalysisImpl();
        }

    }

    private FactorValue subsetFactorValue;

    private Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<>();

    /**
     * Groups of results produced by this ExpressionAnalysis. For example, in a two-way ANOVA, the model has 2 or 3
     * parameters. The statistical significance tests for each of the effects in the model are stored as separate
     * ResultSet objects. Thus a two-way ANOVA with interactions will have three result sets: the two main effects and
     * the interaction effect.
     */
    public Collection<ExpressionAnalysisResultSet> getResultSets() {
        return this.resultSets;
    }

    /**
     * 
     */
    public FactorValue getSubsetFactorValue() {
        return this.subsetFactorValue;
    }

    public void setResultSets( Collection<ExpressionAnalysisResultSet> resultSets ) {
        this.resultSets = resultSets;
    }

    public void setSubsetFactorValue( FactorValue subsetFactorValue ) {
        this.subsetFactorValue = subsetFactorValue;
    }

}