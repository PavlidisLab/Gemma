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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Collection;
import java.util.HashSet;

/**
 * Summary of a result set.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Data
@EqualsAndHashCode(of = { "id" })
public class DiffExResultSetSummaryValueObject implements java.io.Serializable {

    private static final long serialVersionUID = 2063274043081170625L;

    private Long id;

    @JsonIgnore
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

    /**
     * @deprecated use {@link #getResultSetId()} instead
     */
    @Deprecated
    public Long getResultSetId() {
        return id;
    }

    /**
     * @deprecated use {@link #setId} instead
     */
    @Deprecated
    public void setResultSetId( Long resultSetId ) {
        this.id = resultSetId;
    }
}
