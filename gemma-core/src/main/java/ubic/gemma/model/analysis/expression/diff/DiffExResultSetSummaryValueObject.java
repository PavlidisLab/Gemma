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

import org.hibernate.Hibernate;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Summary of a result set.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class DiffExResultSetSummaryValueObject implements java.io.Serializable {

    private static final long serialVersionUID = 2063274043081170625L;

    private Long id;

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

    @Deprecated
    private Long resultSetId;

    private Double threshold;

    private Integer upregulatedCount;

    private Long bioAssaySetAnalyzedId;

    public DiffExResultSetSummaryValueObject( ExpressionAnalysisResultSet resultSet ) {
        this.setId( resultSet.getId() );
        this.setResultSetId( resultSet.getId() );
        this.setFactorIds( EntityUtils.getIds( resultSet.getExperimentalFactors() ) );
        this.setNumberOfGenesAnalyzed( resultSet.getNumberOfGenesTested() );
        this.setNumberOfProbesAnalyzed( resultSet.getNumberOfProbesTested() );

        this.setThreshold( DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD );
        for ( ExperimentalFactor ef : resultSet.getExperimentalFactors() ) {
            this.getExperimentalFactors().add( new ExperimentalFactorValueObject( ef ) );
        }

        for ( HitListSize hitList : resultSet.getHitListSizes() ) {
            if ( hitList.getThresholdQvalue()
                    .equals( DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD ) ) {
                if ( hitList.getDirection().equals( Direction.UP ) ) {
                    this.setUpregulatedCount( hitList.getNumberOfProbes() );
                } else if ( hitList.getDirection().equals( Direction.DOWN ) ) {
                    this.setDownregulatedCount( hitList.getNumberOfProbes() );
                } else if ( hitList.getDirection().equals( Direction.EITHER ) ) {
                    this.setNumberOfDiffExpressedProbes( hitList.getNumberOfProbes() );
                }

            }
        }

        if ( resultSet.getBaselineGroup() != null ) {
            this.setBaselineGroup( new FactorValueValueObject( resultSet.getBaselineGroup() ) );
        }
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Long getBioAssaySetAnalyzedId() {
        return bioAssaySetAnalyzedId;
    }

    public void setBioAssaySetAnalyzedId( Long id ) {
        this.bioAssaySetAnalyzedId = id;
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

    /**
     * @deprecated use {@link #getId} instead
     */
    @Deprecated
    public Long getResultSetId() {
        return resultSetId;
    }

    /**
     * @deprecated use {@link #setId} instead
     */
    @Deprecated
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

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( this.getClass() != obj.getClass() ) {
            return false;
        }
        DiffExResultSetSummaryValueObject other = ( DiffExResultSetSummaryValueObject ) obj;
        return Objects.equals( resultSetId, other.resultSetId );
    }

    public void setExperimentalFactorsByValueObject( Collection<ExperimentalFactorValueObject> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

}
