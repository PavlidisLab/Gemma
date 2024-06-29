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

import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;

import ubic.gemma.core.lang.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * An analysis of changes in expression levels across experimental conditions
 */
public class DifferentialExpressionAnalysis extends SingleExperimentAnalysis {

    private static final long serialVersionUID = -7855180617739271699L;

    private Set<ExpressionAnalysisResultSet> resultSets = new HashSet<>();

    @Nullable
    private FactorValue subsetFactorValue;

    /**
     * Groups of results produced by this differential expression analysis.
     * <p>
     * For example, in a two-way ANOVA, the model has 2 or 3 parameters. The statistical significance tests for each of
     * the effects in the model are stored as separate {@link ExpressionAnalysisResultSet} objects. Thus, a two-way ANOVA
     * with interactions will have three result sets: the two main effects and the interaction effect.
     */
    public Set<ExpressionAnalysisResultSet> getResultSets() {
        return this.resultSets;
    }

    public void setResultSets( Set<ExpressionAnalysisResultSet> resultSets ) {
        this.resultSets = resultSets;
    }

    /**
     * If non-null, a factor value applicable to the {@link ExpressionExperimentSubSet} being analyzed.
     */
    @Nullable
    public FactorValue getSubsetFactorValue() {
        return this.subsetFactorValue;
    }

    public void setSubsetFactorValue( @Nullable FactorValue subsetFactorValue ) {
        this.subsetFactorValue = subsetFactorValue;
    }

    public static final class Factory {

        public static DifferentialExpressionAnalysis newInstance() {
            return new DifferentialExpressionAnalysis();
        }

    }

}