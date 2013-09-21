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

/**
 * Represents an analysis that combines the results of other analyses of differential expression.
 */
public abstract class GeneDifferentialExpressionMetaAnalysis extends
        ubic.gemma.model.analysis.expression.ExpressionAnalysis implements gemma.gsec.model.Securable {

    /**
     * Constructs new instances of {@link GeneDifferentialExpressionMetaAnalysis}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link GeneDifferentialExpressionMetaAnalysis}.
         */
        public static GeneDifferentialExpressionMetaAnalysis newInstance() {
            return new GeneDifferentialExpressionMetaAnalysisImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5896210853804341350L;
    private Integer numGenesAnalyzed;

    private Double qvalueThresholdForStorage;

    private Collection<ExpressionAnalysisResultSet> resultSetsIncluded = new java.util.HashSet<>();

    private Collection<GeneDifferentialExpressionMetaAnalysisResult> results = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public GeneDifferentialExpressionMetaAnalysis() {
    }

    /**
     * <p>
     * How many genes were included in the meta-analysis. This does not mean that all genes were analyzed in all the
     * experiments.
     * </p>
     */
    public Integer getNumGenesAnalyzed() {
        return this.numGenesAnalyzed;
    }

    /**
     * <p>
     * The threshold, if any, used to determine which of the metaAnalysis results are persisted to the system.
     * </p>
     */
    public Double getQvalueThresholdForStorage() {
        return this.qvalueThresholdForStorage;
    }

    /**
     * 
     */
    public Collection<GeneDifferentialExpressionMetaAnalysisResult> getResults() {
        return this.results;
    }

    /**
     * 
     */
    public Collection<ExpressionAnalysisResultSet> getResultSetsIncluded() {
        return this.resultSetsIncluded;
    }

    public void setNumGenesAnalyzed( Integer numGenesAnalyzed ) {
        this.numGenesAnalyzed = numGenesAnalyzed;
    }

    public void setQvalueThresholdForStorage( Double qvalueThresholdForStorage ) {
        this.qvalueThresholdForStorage = qvalueThresholdForStorage;
    }

    public void setResults( Collection<GeneDifferentialExpressionMetaAnalysisResult> results ) {
        this.results = results;
    }

    public void setResultSetsIncluded( Collection<ExpressionAnalysisResultSet> resultSetsIncluded ) {
        this.resultSetsIncluded = resultSetsIncluded;
    }

}