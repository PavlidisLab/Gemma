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

import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

/**
 * Summary of a resultset.
 * 
 * @author paul
 * @version $Id$
 */
public class DiffExResultSetSummaryValueObject implements java.io.Serializable {

    private static final long serialVersionUID = 2063274043081170625L;

    private Long analysisId;

    private Collection<Long> arrayDesignsUsed;

    private FactorValueValueObject baselineGroup;

    private Integer downregulatedCount;

    private Collection<ExperimentalFactorValueObject> experimentalFactors = new HashSet<>();

    private Collection<Long> factorIds;

    private Integer numberOfDiffExpressedProbes;

    private Integer numberOfGenesAnalyzed;

    private Integer numberOfProbesAnalyzed;

    private Double qValue;

    private Long resultSetId;

    private Double threshold;

    private Integer upregulatedCount;

    private Long bioAssaySetAnalyzedId;

    public Long getBioAssaySetAnalyzedId() {
        return bioAssaySetAnalyzedId;
    }

    public DiffExResultSetSummaryValueObject() {
        super();
    }

    // public DiffExResultSetSummaryValueObject( Collection<ExperimentalFactorValueObject> experimentalFactors,
    // Double qValue, Double threshold, int numberOfDiffExpressedProbes, long resultSetId ) {
    // this();
    // this.experimentalFactors = experimentalFactors;
    // this.qValue = qValue;
    // this.threshold = threshold;
    // this.numberOfDiffExpressedProbes = numberOfDiffExpressedProbes;
    // this.resultSetId = resultSetId;
    // }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        DiffExResultSetSummaryValueObject other = ( DiffExResultSetSummaryValueObject ) obj;
        if ( resultSetId != other.resultSetId ) {
            return false;
        }
        return true;
    }

    /**
     * @return the analysisId
     */
    public Long getAnalysisId() {
        return analysisId;
    }

    public Collection<Long> getArrayDesignsUsed() {
        return arrayDesignsUsed;
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

    public Integer getNumberOfGenesAnalyzed() {
        return numberOfGenesAnalyzed;
    }

    public Integer getNumberOfProbesAnalyzed() {
        return numberOfProbesAnalyzed;
    }

    public Double getqValue() {
        return qValue;
    }

    public Double getQValue() {
        return qValue;
    }

    public Long getResultSetId() {
        return resultSetId;
    }

    public Double getThreshold() {
        return threshold;
    }

    public Integer getUpregulatedCount() {
        return upregulatedCount;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( int ) ( resultSetId ^ ( resultSetId >>> 32 ) );
        return result;
    }

    /**
     * @param analysisId the analysisId to set
     */
    public void setAnalysisId( Long analysisId ) {
        this.analysisId = analysisId;
    }

    public void setArrayDesignsUsed( Collection<Long> arrayDesignsUsed ) {
        this.arrayDesignsUsed = arrayDesignsUsed;
    }

    public void setBaselineGroup( FactorValueValueObject baselineGroup ) {
        this.baselineGroup = baselineGroup;
    }

    public void setDownregulatedCount( Integer downregulatedCount ) {
        this.downregulatedCount = downregulatedCount;
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

    public void setNumberOfGenesAnalyzed( Integer numberOfGenesAnalyzed ) {
        this.numberOfGenesAnalyzed = numberOfGenesAnalyzed;
    }

    public void setNumberOfProbesAnalyzed( Integer numberOfProbesAnalyzed ) {
        this.numberOfProbesAnalyzed = numberOfProbesAnalyzed;
    }

    public void setqValue( Double qValue ) {
        this.qValue = qValue;
    }

    public void setQValue( Double value ) {
        qValue = value;
    }

    public void setResultSetId( Long resultSetId ) {
        this.resultSetId = resultSetId;
    }

    public void setThreshold( Double threshold ) {
        this.threshold = threshold;
    }

    public void setUpregulatedCount( Integer upregulatedCount ) {
        this.upregulatedCount = upregulatedCount;
    }

    /**
     * @param id
     */
    public void setBioAssaySetAnalyzedId( Long id ) {
        this.bioAssaySetAnalyzedId = id;
    }

}
