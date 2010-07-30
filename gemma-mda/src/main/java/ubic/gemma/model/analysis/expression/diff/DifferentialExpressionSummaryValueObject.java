/* The Gemma project
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

package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

/**
 * Summary of a resultset.
 * 
 * @author paul
 * @version $Id$
 */
public class DifferentialExpressionSummaryValueObject implements java.io.Serializable {

    private static final long serialVersionUID = 2063274043081170625L;
    private Long analysisId;
    private FactorValueValueObject baselineGroup;
    private Integer downregulatedCount;

    private Collection<ExperimentalFactorValueObject> experimentalFactors;

    private Collection<Long> factorIds;

    private Integer numberOfDiffExpressedProbes;

    private Double qValue;
    private long resultSetId;

    private Double threshold;

    private Integer upregulatedCount;

    public DifferentialExpressionSummaryValueObject() {
        super();
    }

    public DifferentialExpressionSummaryValueObject( Collection<ExperimentalFactorValueObject> experimentalFactors,
            Double qValue, Double threshold, int numberOfDiffExpressedProbes, long resultSetId ) {
        this();
        this.experimentalFactors = experimentalFactors;
        this.qValue = qValue;
        this.threshold = threshold;
        this.numberOfDiffExpressedProbes = numberOfDiffExpressedProbes;
        this.resultSetId = resultSetId;

    }

    /**
     * @return the analysisId
     */
    public Long getAnalysisId() {
        return analysisId;
    }

    public FactorValueValueObject getBaselineGroup() {
        return baselineGroup;
    }

    public Integer getDownregulatedCount() {
        return downregulatedCount;
    }

    public Collection<ExperimentalFactorValueObject> getExperimentalFactors() {
        return experimentalFactors;
    }

    /**
     * @return the factorIds
     */
    public Collection<Long> getFactorIds() {
        return factorIds;
    }

    public Integer getNumberOfDiffExpressedProbes() {
        return numberOfDiffExpressedProbes;
    }

    public Double getQValue() {
        return qValue;
    }

    public long getResultSetId() {
        return resultSetId;
    }

    public Double getThreshold() {
        return threshold;
    }

    public Integer getUpregulatedCount() {
        return upregulatedCount;
    }

    /**
     * @param analysisId the analysisId to set
     */
    public void setAnalysisId( Long analysisId ) {
        this.analysisId = analysisId;
    }

    public void setBaselineGroup( FactorValueValueObject baselineGroup ) {
        this.baselineGroup = baselineGroup;
    }

    public void setDownregulatedCount( Integer downregulatedCount ) {
        this.downregulatedCount = downregulatedCount;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors ) {

        this.experimentalFactors = new HashSet<ExperimentalFactorValueObject>();
        ExperimentalFactorValueObject efvo = null;

        for ( ExperimentalFactor eF : experimentalFactors ) {
            efvo = new ExperimentalFactorValueObject( eF );
            this.experimentalFactors.add( efvo );
        }
    }

    public void setExperimentalFactorsByValueObject( Collection<ExperimentalFactorValueObject> experimentalFactors ) {

        this.experimentalFactors = experimentalFactors;
    }

    /**
     * @param factorIds the factorIds to set
     */
    public void setFactorIds( Collection<Long> factorIds ) {
        this.factorIds = factorIds;
    }

    public void setNumberOfDiffExpressedProbes( Integer numberOfDiffExpressedProbes ) {
        this.numberOfDiffExpressedProbes = numberOfDiffExpressedProbes;
    }

    public void setQValue( Double value ) {
        qValue = value;
    }

    public void setResultSetId( long resultSetId ) {
        this.resultSetId = resultSetId;
    }

    public void setThreshold( Double threshold ) {
        this.threshold = threshold;
    }

    public void setUpregulatedCount( Integer upregulatedCount ) {
        this.upregulatedCount = upregulatedCount;
    }

}
