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

import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A group of results for an ExpressionExperiment.
 */
public abstract class ExpressionAnalysisResultSet extends FactorAssociatedAnalysisResultSet {

    /**
     * Constructs new instances of {@link ExpressionAnalysisResultSet}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ExpressionAnalysisResultSet}.
         */
        public static ExpressionAnalysisResultSet newInstance() {
            return new ExpressionAnalysisResultSetImpl();
        }

        /**
         * Constructs a new instance of {@link ExpressionAnalysisResultSet}, taking all possible properties (except the
         * identifier(s))as arguments.
         */
        public static ExpressionAnalysisResultSet newInstance( Collection<ExperimentalFactor> experimentalFactors,
                Integer numberOfProbesTested, java.lang.Integer numberOfGenesTested, Double qvalueThresholdForStorage,
                FactorValue baselineGroup, Collection<DifferentialExpressionAnalysisResult> results,
                DifferentialExpressionAnalysis analysis, PvalueDistribution pvalueDistribution,
                Collection<HitListSize> hitListSizes ) {
            final ExpressionAnalysisResultSet entity = new ExpressionAnalysisResultSetImpl();
            entity.setExperimentalFactors( experimentalFactors );
            entity.setNumberOfProbesTested( numberOfProbesTested );
            entity.setNumberOfGenesTested( numberOfGenesTested );
            entity.setQvalueThresholdForStorage( qvalueThresholdForStorage );
            entity.setBaselineGroup( baselineGroup );
            entity.setResults( results );
            entity.setAnalysis( analysis );
            entity.setPvalueDistribution( pvalueDistribution );
            entity.setHitListSizes( hitListSizes );
            return entity;
        }
    }

    private Integer numberOfProbesTested;

    private Integer numberOfGenesTested;

    private Double qvalueThresholdForStorage;

    private FactorValue baselineGroup;

    private Collection<DifferentialExpressionAnalysisResult> results = new java.util.HashSet<>();

    private DifferentialExpressionAnalysis analysis;

    private PvalueDistribution pvalueDistribution;

    private Collection<HitListSize> hitListSizes = new java.util.HashSet<HitListSize>();

    /**
     * 
     */
    public DifferentialExpressionAnalysis getAnalysis() {
        return this.analysis;
    }

    /**
     * The group considered baseline when computing scores and 'upRegulated'. This might be a control group if it could
     * be recognized. For continuous factors, this would be null. For interaction terms we do not compute this so it
     * will also be null.
     */
    public FactorValue getBaselineGroup() {
        return this.baselineGroup;
    }

    /**
     * 
     */
    public Collection<HitListSize> getHitListSizes() {
        return this.hitListSizes;
    }

    /**
     * How many genes were tested in the result set. This is determined based on the annotations at one point in time,
     * so might slightly differ if the platform annotations have been updated since.
     */
    public Integer getNumberOfGenesTested() {
        return this.numberOfGenesTested;
    }

    /**
     * How many probes were tested in this result set.
     */
    public Integer getNumberOfProbesTested() {
        return this.numberOfProbesTested;
    }

    /**
     * 
     */
    public PvalueDistribution getPvalueDistribution() {
        return this.pvalueDistribution;
    }

    /**
     * <p>
     * What false discovery rate threshold was used for storing data when the analysis was initially run. If null or
     * 1.0, implies all results were stored.
     * </p>
     */
    public Double getQvalueThresholdForStorage() {
        return this.qvalueThresholdForStorage;
    }

    /**
     * 
     */
    @Override
    public Collection<DifferentialExpressionAnalysisResult> getResults() {
        return this.results;
    }

    public void setAnalysis( DifferentialExpressionAnalysis analysis ) {
        this.analysis = analysis;
    }

    public void setBaselineGroup( FactorValue baselineGroup ) {
        this.baselineGroup = baselineGroup;
    }

    public void setHitListSizes( Collection<HitListSize> hitListSizes ) {
        this.hitListSizes = hitListSizes;
    }

    public void setNumberOfGenesTested( Integer numberOfGenesTested ) {
        this.numberOfGenesTested = numberOfGenesTested;
    }

    public void setNumberOfProbesTested( Integer numberOfProbesTested ) {
        this.numberOfProbesTested = numberOfProbesTested;
    }

    public void setPvalueDistribution( PvalueDistribution pvalueDistribution ) {
        this.pvalueDistribution = pvalueDistribution;
    }

    public void setQvalueThresholdForStorage( Double qvalueThresholdForStorage ) {
        this.qvalueThresholdForStorage = qvalueThresholdForStorage;
    }

    public void setResults( Collection<DifferentialExpressionAnalysisResult> results ) {
        this.results = results;
    }

}