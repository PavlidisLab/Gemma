/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.diff;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.util.ModelUtils;
import ubic.gemma.model.analysis.AnalysisValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Summary of a differential expression analysis
 *
 * @author paul
 */
@SuppressWarnings("unused") // Used in frontend
@Getter
@Setter
public class DifferentialExpressionAnalysisValueObject extends AnalysisValueObject<DifferentialExpressionAnalysis>
        implements Serializable {

    public static final double DEFAULT_THRESHOLD = 0.05; // should be one of the values stored in the HitListSizes
    private static final long serialVersionUID = 622877438067070041L;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<Long, Collection<FactorValueValueObject>> factorValuesUsed = new HashMap<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<DiffExResultSetSummaryValueObject> resultSets = new HashSet<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Collection<Long> arrayDesignsUsed;
    private Long experimentAnalyzedId;

    // for subsets
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long sourceExperimentId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ExperimentalFactorValueObject subsetFactor;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long subsetFactorId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This property is mutually exclusive with `subsetFactorValueId`.")
    private FactorValueValueObject subsetFactorValue;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This property is mutually exclusive with `subsetFactorValue`.")
    private Long subsetFactorValueId;

    public DifferentialExpressionAnalysisValueObject() {
        super();
    }

    /**
     * Does not populate the resultSets.
     *
     * @param analysis the analysis to read the values from
     */
    public DifferentialExpressionAnalysisValueObject( DifferentialExpressionAnalysis analysis ) {
        super( analysis );
        this.experimentAnalyzedId = analysis.getExperimentAnalyzed().getId();
        // experimentAnalyzed is eagerly fetched
        if ( analysis.getExperimentAnalyzed() instanceof ExpressionExperimentSubSet ) {
            // sourceExperiment is eagerly fetched too
            this.sourceExperimentId = ( ( ExpressionExperimentSubSet ) analysis.getExperimentAnalyzed() ).getSourceExperiment().getId();
        }
        // this is only populated for subsets, but it's safer to always check
        if ( analysis.getSubsetFactorValue() != null ) {
            if ( ModelUtils.isInitialized( analysis.getSubsetFactorValue() ) ) {
                this.subsetFactorValue = new FactorValueValueObject( analysis.getSubsetFactorValue(), false );
                if ( ModelUtils.isInitialized( analysis.getSubsetFactorValue().getExperimentalFactor() ) ) {
                    this.subsetFactor = new ExperimentalFactorValueObject(
                            analysis.getSubsetFactorValue().getExperimentalFactor() );
                }
                // TODO: make this mutually exclusive with subsetFactor
                this.subsetFactorId = analysis.getSubsetFactorValue().getExperimentalFactor().getId();
            } else {
                this.subsetFactorValueId = analysis.getSubsetFactorValue().getId();
            }
        }
        // fill in the factorValuesUsed separately, needs access to details of the subset.
    }

    /**
     * @deprecated use {@link #getExperimentAnalyzedId()} instead
     */
    @Deprecated
    @Schema(description = "This is deprecated, use experimentAnalyzedId instead.", deprecated = true)
    public Long getBioAssaySetId() {
        return experimentAnalyzedId;
    }

    /**
     * @deprecated use {@link #getSourceExperimentId()} instead
     */
    @Deprecated
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This is deprecated, use sourceExperimentId instead.", deprecated = true)
    public Long getSourceExperiment() {
        return sourceExperimentId;
    }

    /**
     * @deprecated This was renamed for clarity.
     * @see #getFactorValuesUsedByExperimentalFactorId()
     */
    @Deprecated
    public Map<Long, Collection<FactorValueValueObject>> getFactorValuesUsed() {
        return factorValuesUsed;
    }

    /**
     * Produce a mapping of {@link ubic.gemma.model.expression.experiment.ExperimentalFactor} IDs to
     * {@link ubic.gemma.model.expression.experiment.FactorValue} VOs used in this analysis.
     * <p>
     * If this is a NOT a subset analysis, then this won't be important (so it may not be populated), but for subset
     * analyses (subsetFactor != null), only the factor values present in the subset are relevant.
     * <p>
     * This can be null in certain cases if set to NULL via {@link #setFactorValuesUsed(Map)} so that it does not appear
     * in the JSON serialization, but you can assume it is non-null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<Long, Collection<FactorValueValueObject>> getFactorValuesUsedByExperimentalFactorId() {
        return factorValuesUsed;
    }

    @JsonProperty("isSubset")
    @Schema(description = "Indicate if this analysis is a subset of another experiment. if this is set, additional fields relevant to the subset will be populated.")
    public boolean isSubset() {
        return this.sourceExperimentId != null;
    }

    @Override
    public String toString() {
        return "DiffExAnalysisVO [id=" + id + ", experimentAnalyzedId=" + experimentAnalyzedId + "]";
    }
}
