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

import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

/**
 * A group of results for an ExpressionExperiment.
 */
public class ExpressionAnalysisResultSet extends FactorAssociatedAnalysisResultSet<DifferentialExpressionAnalysisResult> implements SecuredChild {

    private static final long serialVersionUID = 7226901182513177574L;
    private Integer numberOfProbesTested;
    private Integer numberOfGenesTested;
    @Nullable
    private FactorValue baselineGroup;
    private Set<DifferentialExpressionAnalysisResult> results = new HashSet<>();
    private DifferentialExpressionAnalysis analysis;
    private PvalueDistribution pvalueDistribution;
    private Set<HitListSize> hitListSizes = new HashSet<>();

    public DifferentialExpressionAnalysis getAnalysis() {
        return this.analysis;
    }

    public void setAnalysis( DifferentialExpressionAnalysis analysis ) {
        this.analysis = analysis;
    }

    /**
     * @return The group considered baseline when computing scores and 'upRegulated'. This might be a control group if it could
     * be recognized. For continuous factors, this would be null. For interaction terms we do not compute this so it
     * will also be null.
     */
    @Nullable
    public FactorValue getBaselineGroup() {
        return this.baselineGroup;
    }

    public void setBaselineGroup( @Nullable FactorValue baselineGroup ) {
        this.baselineGroup = baselineGroup;
    }

    public Set<HitListSize> getHitListSizes() {
        return this.hitListSizes;
    }

    public void setHitListSizes( Set<HitListSize> hitListSizes ) {
        this.hitListSizes = hitListSizes;
    }

    /**
     * @return How many genes were tested in the result set. This is determined based on the annotations at one point in time,
     * so might slightly differ if the platform annotations have been updated since.
     */
    public Integer getNumberOfGenesTested() {
        return this.numberOfGenesTested;
    }

    public void setNumberOfGenesTested( Integer numberOfGenesTested ) {
        this.numberOfGenesTested = numberOfGenesTested;
    }

    /**
     * @return How many probes were tested in this result set.
     */
    public Integer getNumberOfProbesTested() {
        return this.numberOfProbesTested;
    }

    public void setNumberOfProbesTested( Integer numberOfProbesTested ) {
        this.numberOfProbesTested = numberOfProbesTested;
    }

    public PvalueDistribution getPvalueDistribution() {
        return this.pvalueDistribution;
    }

    public void setPvalueDistribution( PvalueDistribution pvalueDistribution ) {
        this.pvalueDistribution = pvalueDistribution;
    }

    public Set<DifferentialExpressionAnalysisResult> getResults() {
        return this.results;
    }

    public void setResults( Set<DifferentialExpressionAnalysisResult> results ) {
        this.results = results;
    }

    @Override
    @Transient
    public Securable getSecurityOwner() {
        return analysis;
    }

    @Override
    public String toString() {
        return "ExpressionAnalysisResultSet" + ( getId() != null ? " Id=" + getId() : "" );
    }

    public static final class Factory {

        public static ExpressionAnalysisResultSet newInstance() {
            return new ExpressionAnalysisResultSet();
        }

        public static ExpressionAnalysisResultSet newInstance( Set<ExperimentalFactor> experimentalFactors,
                Integer numberOfProbesTested, java.lang.Integer numberOfGenesTested, FactorValue baselineGroup,
                Set<DifferentialExpressionAnalysisResult> results, DifferentialExpressionAnalysis analysis,
                PvalueDistribution pvalueDistribution, Set<HitListSize> hitListSizes ) {
            final ExpressionAnalysisResultSet entity = new ExpressionAnalysisResultSet();
            entity.setExperimentalFactors( experimentalFactors );
            entity.setNumberOfProbesTested( numberOfProbesTested );
            entity.setNumberOfGenesTested( numberOfGenesTested );
            entity.setBaselineGroup( baselineGroup );
            entity.setResults( results );
            entity.setAnalysis( analysis );
            entity.setPvalueDistribution( pvalueDistribution );
            entity.setHitListSizes( hitListSizes );
            return entity;
        }
    }

}