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

import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Summary of a result set.
 *
 * @author paul
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

    public DiffExResultSetSummaryValueObject() {
        super();
    }

    public Long getBioAssaySetAnalyzedId() {
        return bioAssaySetAnalyzedId;
    }

    public void setBioAssaySetAnalyzedId( Long id ) {
        this.bioAssaySetAnalyzedId = id;
    }

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
        if ( !Objects.equals( resultSetId, other.resultSetId ) ) {
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

    /**
     * @param analysisId the analysisId to set
     */
    public void setAnalysisId( Long analysisId ) {
        this.analysisId = analysisId;
    }

    public Collection<Long> getArrayDesignsUsed() {
        return arrayDesignsUsed;
    }

    public void setArrayDesignsUsed( Collection<Long> arrayDesignsUsed ) {
        this.arrayDesignsUsed = arrayDesignsUsed;
    }

    public FactorValueValueObject getBaselineGroup() {
        return baselineGroup;
    }

    public void setBaselineGroup( FactorValueValueObject baselineGroup ) {
        this.baselineGroup = baselineGroup;
    }

    public Integer getDownregulatedCount() {
        return downregulatedCount;
    }

    public void setDownregulatedCount( Integer downregulatedCount ) {
        this.downregulatedCount = downregulatedCount;
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

    /**
     * @param factorIds the factorIds to set
     */
    public void setFactorIds( Collection<Long> factorIds ) {
        this.factorIds = factorIds;
    }

    public Integer getNumberOfDiffExpressedProbes() {
        return numberOfDiffExpressedProbes;
    }

    public void setNumberOfDiffExpressedProbes( Integer numberOfDiffExpressedProbes ) {
        this.numberOfDiffExpressedProbes = numberOfDiffExpressedProbes;
    }

    public Integer getNumberOfGenesAnalyzed() {
        return numberOfGenesAnalyzed;
    }

    public void setNumberOfGenesAnalyzed( Integer numberOfGenesAnalyzed ) {
        this.numberOfGenesAnalyzed = numberOfGenesAnalyzed;
    }

    public Integer getNumberOfProbesAnalyzed() {
        return numberOfProbesAnalyzed;
    }

    public void setNumberOfProbesAnalyzed( Integer numberOfProbesAnalyzed ) {
        this.numberOfProbesAnalyzed = numberOfProbesAnalyzed;
    }

    public Double getQValue() {
        return qValue;
    }

    public void setQValue( Double value ) {
        qValue = value;
    }

    public Long getResultSetId() {
        return resultSetId;
    }

    public void setResultSetId( Long resultSetId ) {
        this.resultSetId = resultSetId;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold( Double threshold ) {
        this.threshold = threshold;
    }

    public Integer getUpregulatedCount() {
        return upregulatedCount;
    }

    public void setUpregulatedCount( Integer upregulatedCount ) {
        this.upregulatedCount = upregulatedCount;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( int ) ( resultSetId ^ ( resultSetId >>> 32 ) );
        return result;
    }

    public void setExperimentalFactorsByValueObject( Collection<ExperimentalFactorValueObject> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

}
