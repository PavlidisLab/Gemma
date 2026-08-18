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
import org.springframework.lang.Nullable;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * List of BioAssays analyzed
     */
    private Collection<BioAssayValueObject> bioAssaysAnalyzed;

    private Integer numberOfGenesAnalyzed;

    private Integer numberOfProbesAnalyzed;

    /**
     * This is used once in the frontend, but never filled, so please ignore.
     */
    @WithheldFromApi(value = Reason.UNTRIAGED,
            comment = "check whether it duplicates the exposed corrected p-value shape")
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
        populateBase( resultSet, null );
        // extract statistics for the default threshold (if available)
        for ( HitListSize hitList : resultSet.getHitListSizes() ) {
            applyHitListSize( hitList );
        }
    }

    /**
     * Variant constructor that uses a pre-aggregated counts snapshot (cached) for the hit-list
     * fields instead of walking {@link ExpressionAnalysisResultSet#getHitListSizes()}.
     * <p>
     * Used by the {@code findByExperimentIds} enrichment path so warm requests skip the
     * collection initialization on the {@code hitListSizes} association.
     */
    public DiffExResultSetSummaryValueObject( ExpressionAnalysisResultSet resultSet,
            ubic.gemma.model.analysis.expression.diff.ResultSetCountsValueObject counts ) {
        this( resultSet, counts, null );
    }

    /**
     * Variant constructor that also accepts a {@link Prefetch} snapshot of the result-set's
     * {@code experimentalFactors} + {@code baselineGroup} associations gathered up-front by
     * a batched HQL fetch in the caller. When {@code prefetch} is non-null, the VO is
     * populated without touching the lazy associations on {@code resultSet}, eliminating
     * three round trips per RS in the {@code findByExperimentIds} hot path.
     * <p>
     * Passing {@code null} for {@code prefetch} preserves the original lazy-load behaviour.
     */
    public DiffExResultSetSummaryValueObject( ExpressionAnalysisResultSet resultSet,
            @Nullable ubic.gemma.model.analysis.expression.diff.ResultSetCountsValueObject counts,
            @Nullable Prefetch prefetch ) {
        populateBase( resultSet, prefetch );
        if ( counts != null ) {
            this.setThreshold( counts.getThreshold() );
            this.setNumberOfDiffExpressedProbes( counts.getNumberOfDiffExpressedProbes() );
            this.setUpregulatedCount( counts.getUpregulatedCount() );
            this.setDownregulatedCount( counts.getDownregulatedCount() );
        }
    }

    private void populateBase( ExpressionAnalysisResultSet resultSet, @Nullable Prefetch prefetch ) {
        this.setId( resultSet.getId() );
        this.setNumberOfGenesAnalyzed( resultSet.getNumberOfGenesTested() );
        this.setNumberOfProbesAnalyzed( resultSet.getNumberOfProbesTested() );

        Set<ExperimentalFactor> efs = prefetch != null
                ? prefetch.getExperimentalFactors()
                : resultSet.getExperimentalFactors();
        this.setFactorIds( IdentifiableUtils.getIds( efs ) );
        for ( ExperimentalFactor ef : efs ) {
            this.getExperimentalFactors().add( new ExperimentalFactorValueObject( ef ) );
        }

        FactorValue baseline = prefetch != null
                ? prefetch.getBaselineGroup()
                : resultSet.getBaselineGroup();
        if ( baseline != null ) {
            this.setBaselineGroup( new FactorValueValueObject( baseline ) );
        }
    }

    /**
     * Pre-aggregated {@code experimentalFactors} + {@code baselineGroup} for a single
     * {@link ExpressionAnalysisResultSet}, sourced from a batched join-fetch query so the
     * VO ctor avoids three sequential lazy initializations.
     *
     * @see DiffExResultSetSummaryValueObject#DiffExResultSetSummaryValueObject(ExpressionAnalysisResultSet,
     *      ubic.gemma.model.analysis.expression.diff.ResultSetCountsValueObject, Prefetch)
     */
    public static final class Prefetch implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Set<ExperimentalFactor> experimentalFactors;
        @Nullable
        private final FactorValue baselineGroup;

        public Prefetch( Set<ExperimentalFactor> experimentalFactors, @Nullable FactorValue baselineGroup ) {
            this.experimentalFactors = experimentalFactors != null
                    ? experimentalFactors
                    : Collections.emptySet();
            this.baselineGroup = baselineGroup;
        }

        public Set<ExperimentalFactor> getExperimentalFactors() {
            return experimentalFactors;
        }

        @Nullable
        public FactorValue getBaselineGroup() {
            return baselineGroup;
        }
    }

    private void applyHitListSize( HitListSize hitList ) {
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
    @WithheldFromApi(value = Reason.UNTRIAGED,
            comment = "a result-set id clients cannot see is suspicious; check whether it duplicates id")
    public Long getResultSetId() {
        return id;
    }
}
