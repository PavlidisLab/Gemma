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

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an analysis that combines the results of other analyses of differential expression.
 */
public class GeneDifferentialExpressionMetaAnalysis extends ExpressionAnalysis implements Securable {

    private static final long serialVersionUID = -2588180973962410595L;
    private Integer numGenesAnalyzed;
    private Double qvalueThresholdForStorage;
    private Set<ExpressionAnalysisResultSet> resultSetsIncluded = new HashSet<>();
    private Set<GeneDifferentialExpressionMetaAnalysisResult> results = new HashSet<>();

    /**
     * @return How many genes were included in the meta-analysis. This does not mean that all genes were analyzed in all the
     * experiments.
     */
    public Integer getNumGenesAnalyzed() {
        return this.numGenesAnalyzed;
    }

    public void setNumGenesAnalyzed( Integer numGenesAnalyzed ) {
        this.numGenesAnalyzed = numGenesAnalyzed;
    }

    /**
     * @return The threshold, if any, used to determine which of the metaAnalysis results are persisted to the system.
     */
    public Double getQvalueThresholdForStorage() {
        return this.qvalueThresholdForStorage;
    }

    public void setQvalueThresholdForStorage( Double qvalueThresholdForStorage ) {
        this.qvalueThresholdForStorage = qvalueThresholdForStorage;
    }

    public Set<GeneDifferentialExpressionMetaAnalysisResult> getResults() {
        return this.results;
    }

    public void setResults( Set<GeneDifferentialExpressionMetaAnalysisResult> results ) {
        this.results = results;
    }

    public Set<ExpressionAnalysisResultSet> getResultSetsIncluded() {
        return this.resultSetsIncluded;
    }

    public void setResultSetsIncluded( Set<ExpressionAnalysisResultSet> resultSetsIncluded ) {
        this.resultSetsIncluded = resultSetsIncluded;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof GeneDifferentialExpressionMetaAnalysis ) )
            return false;
        GeneDifferentialExpressionMetaAnalysis that = ( GeneDifferentialExpressionMetaAnalysis ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {
        public static GeneDifferentialExpressionMetaAnalysis newInstance() {
            return new GeneDifferentialExpressionMetaAnalysis();
        }
    }

}