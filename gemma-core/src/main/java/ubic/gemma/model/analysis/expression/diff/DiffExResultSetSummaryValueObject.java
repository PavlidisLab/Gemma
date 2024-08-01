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
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Summary of a result set.
 *
 * @see DifferentialExpressionAnalysisValueObject
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Data
@EqualsAndHashCode(of = { "id" })
public class DiffExResultSetSummaryValueObject implements Serializable {

    private static final long serialVersionUID = 2063274043081170625L;

    private Long id;

    @JsonIgnore
    private Long analysisId;

    private Collection<Long> arrayDesignsUsed;

    private FactorValueValueObject baselineGroup;

    private Collection<ExperimentalFactorValueObject> experimentalFactors = new HashSet<>();

    @JsonIgnore
    private Collection<Long> factorIds;

    /**
     * Analyzed {@link ubic.gemma.model.expression.experiment.BioAssaySet} ID.
     * <p>
     * This is redundant because of {@link DifferentialExpressionAnalysisValueObject#getExperimentAnalyzedId()}, and always
     * displayed in that context in the RESTful API.
     */
    @JsonIgnore
    private Long bioAssaySetAnalyzedId;

    private Integer numberOfGenesAnalyzed;

    private Integer numberOfProbesAnalyzed;

    /**
     * This is used once in the frontend, but never filled, so please ignore.
     */
    @GemmaWebOnly
    private Double qValue;

    /**
     * Threshold applied to the hitlist.
     */
    private Double threshold;

    /**
     * Number of diffex probes in the {@link Direction#EITHER} hit list if available.
     */
    private Integer numberOfDiffExpressedProbes;

    /**
     * Number of diffex probes in the {@link Direction#UP} hit list if available.
     */
    private Integer upregulatedCount;

    /**
     * Number of diffex probes in the {@link Direction#DOWN} hit list if available.
     */
    private Integer downregulatedCount;

    public DiffExResultSetSummaryValueObject() {
        super();
    }

    public DiffExResultSetSummaryValueObject( ExpressionAnalysisResultSet resultSet ) {
        this.setId( resultSet.getId() );
        this.setFactorIds( EntityUtils.getIds( resultSet.getExperimentalFactors() ) );
        this.setNumberOfGenesAnalyzed( resultSet.getNumberOfGenesTested() );
        this.setNumberOfProbesAnalyzed( resultSet.getNumberOfProbesTested() );

        for ( ExperimentalFactor ef : resultSet.getExperimentalFactors() ) {
            this.getExperimentalFactors().add( new ExperimentalFactorValueObject( ef ) );
        }

        if ( resultSet.getBaselineGroup() != null ) {
            this.setBaselineGroup( new FactorValueValueObject( resultSet.getBaselineGroup() ) );
        }

        // extract statistics for the default threshold (if available)
        for ( HitListSize hitList : resultSet.getHitListSizes() ) {
            if ( hitList.getThresholdQvalue().equals( DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD ) ) {
                this.setThreshold( hitList.getThresholdQvalue() );
                if ( hitList.getDirection().equals( Direction.UP ) ) {
                    this.setUpregulatedCount( hitList.getNumberOfProbes() );
                } else if ( hitList.getDirection().equals( Direction.DOWN ) ) {
                    this.setDownregulatedCount( hitList.getNumberOfProbes() );
                } else if ( hitList.getDirection().equals( Direction.EITHER ) ) {
                    this.setNumberOfDiffExpressedProbes( hitList.getNumberOfProbes() );
                }
            }
        }
    }

    /**
     * @deprecated use {@link #getNumberOfUpregulatedProbes()} instead.
     */
    @Deprecated
    public Integer getUpregulatedCount() {
        return upregulatedCount;
    }

    public Integer getNumberOfUpregulatedProbes() {
        return upregulatedCount;
    }

    /**
     * @deprecated use {@link #getNumberOfDownregulatedProbes()} instead.
     */
    @Deprecated
    public Integer getDownregulatedCount() {
        return downregulatedCount;
    }

    public Integer getNumberOfDownregulatedProbes() {
        return downregulatedCount;
    }

    /**
     * Alias for {@link #getId()} kept for backward-compatibility in the Gemma Web frontend.
     */
    @GemmaWebOnly
    public Long getResultSetId() {
        return id;
    }
}
