package ubic.gemma.rest;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ubic.gemma.core.security.audit.payload.ProcessedVectorComputationPayload;
import ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.expression.experiment.GeeqValueObject;

import org.springframework.lang.Nullable;
import java.util.Date;
import java.util.List;

/**
 * Snapshot of every preprocessing/analysis pipeline step for a single experiment.
 * <p>
 * Returned by {@code GET /datasets/{dataset}/pipeline-status}. The duplicated curation/permissions/quality
 * fields ({@code troubled}, {@code curationNote}, {@code isPublic}, {@code geeq}) are included as a convenience
 * so that callers driving a pre-public checklist can render everything from a single round trip.
 * <p>
 * Wire shape uses {@code snake_case} field names per the curation-UI alignment (see
 * {@code PIPELINESTATUS_WIRE_AUDIT.md}). Legacy {@code camelCase} keys are accepted on read via
 * {@link JsonAlias} for backwards compatibility with any pre-alignment consumers.
 */
@Getter
@Setter
public class PipelineStatusValueObject {

    @JsonProperty("datasetId")
    @JsonAlias({ "experimentId" })
    private Long experimentId;

    private List<PipelineStepValueObject> steps;

    @JsonProperty("hasBatchInformation")
    @JsonAlias({ "hasBatchInformation" })
    private boolean hasBatchInformation;

    @JsonProperty("hasDea")
    @JsonAlias({ "hasDifferentialExpressionAnalysis" })
    private boolean hasDifferentialExpressionAnalysis;

    @JsonProperty("hasCoexpressionAnalysis")
    @JsonAlias({ "hasCoexpressionAnalysis" })
    private boolean hasCoexpressionAnalysis;

    @JsonProperty("isTroubled")
    @JsonAlias({ "troubled" })
    private boolean troubled;

    @JsonProperty("troubleDetails")
    @JsonAlias({ "troubleDetails" })
    private String troubleDetails;

    /**
     * The curator flag that predates the agent workflow: a human marked this dataset as wanting
     * a look. 🛑 NOT the agent workflow's signal — that is {@link #triageVerdict}. The two are
     * deliberately separate and neither is derived from the other.
     */
    @JsonProperty("needsAttention")
    @JsonAlias({ "needsAttention" })
    private boolean needsAttention;

    /**
     * The effective triage ruling across this dataset's annotation sets — {@code fine},
     * {@code wont_fix}, {@code might_fix} or {@code must_fix} — or {@code null} when nothing has
     * been triaged. Newest ruling wins, the same rule a single annotation set already uses.
     * <p>
     * This is the agent workflow's answer to "does a human need to look at this", and it carries
     * severity and a judge rather than collapsing to a boolean.
     */
    @Nullable
    @Schema(allowableValues = { "fine", "wont_fix", "might_fix", "must_fix" })
    @JsonProperty("triageVerdict")
    @JsonAlias({ "triageVerdict" })
    private String triageVerdict;

    /**
     * Whether {@link #triageVerdict} came from an {@code agent} or a {@code curator}; {@code null} when
     * nothing has been triaged. A curator's ruling and an agent's are not worth the same, and a
     * caller cannot tell them apart from the verdict alone.
     */
    @Nullable
    @Schema(allowableValues = { "agent", "curator" })
    @JsonProperty("triageJudgeKind")
    @JsonAlias({ "triageJudgeKind" })
    private String triageJudgeKind;

    /**
     * Admin-only; remains {@code null} for non-administrators.
     */
    @Nullable
    @JsonProperty("curationNote")
    @JsonAlias({ "curationNote" })
    private String curationNote;

    private boolean isPublic;

    /**
     * What changed here most recently, as a short label plus its date and performer — the thing
     * {@code curationDetails.lastUpdated} leaves out.
     * <p>
     * Derived from the dataset's latest typed audit event; a dataset that has only ever been loaded
     * falls back to its creation row ("Added to Gemma"). {@code null} only for a dataset with no
     * usable audit row at all.
     */
    @Nullable
    @JsonProperty("lastUpdate")
    private DatasetUpdateSummaryValueObject lastUpdate;

    @Nullable
    private GeeqValueObject geeq;

    @JsonProperty("isPublic")
    public boolean getIsPublic() {
        return isPublic;
    }

    @JsonProperty("isPublic")
    @JsonAlias({ "isPublic" })
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    @Getter
    @Setter
    public static class PipelineStepValueObject {

        /** The step has completed successfully. */
        public static final String STATUS_OK = "ok";
        /** The most recent attempt failed. */
        public static final String STATUS_FAILED = "failed";
        /** Applicable to this experiment, but never attempted. */
        public static final String STATUS_NOT_RUN = "notRun";
        /** Not applicable to this experiment -- so "never run" is not a gap. */
        public static final String STATUS_NOT_APPLICABLE = "notApplicable";
        /**
         * Ran successfully, and the experimental design has changed since -- so the result is still there but
         * no longer describes the design it was computed from.
         * <p>
         * 🛑 Distinct from the two states that already exist elsewhere and are NOT this. {@code incomplete}
         * (the curation store's step state) means started and unfinished, a curator owes it something.
         * {@code needsAttention} is the curator's own flag, mirrored at the top level of this VO. This is
         * neither: nobody has to do anything for the state to be true, and it is about the derived artifact
         * rather than about the curation.
         * <p>
         * Every step but {@code batchInfo} can report it. A design change that invalidates an analysis DELETES
         * it, after which the step reads {@code notRun}; this covers the case where the analysis survived the
         * change.
         * <p>
         * {@code GET /datasets/staleSteps} is the corpus-wide read: which datasets carry one of these, and
         * which steps.
         */
        public static final String STATUS_STALE = "stale";

        /**
         * Which pipeline step. The nine emitted by
         * {@code DatasetsWebService.PIPELINE_STEPS}.
         */
        @Schema(allowableValues = { "batchInfo", "preprocess", "batchCorrection", "pca", "sampleCorrelation",
                "meanVariance", "dea", "coexpression", "missingValue" })
        private String step;
        /**
         * One of {@link #STATUS_OK}, {@link #STATUS_FAILED}, {@link #STATUS_NOT_RUN},
         * {@link #STATUS_NOT_APPLICABLE}, {@link #STATUS_STALE}.
         * <p>
         * Kept a {@code String} rather than promoted to an enum, so that adding a value later is not a
         * deserialization break for a consumer holding an older copy of the vocabulary. The
         * {@code allowableValues} below is what pins it: before this, the deployed OpenAPI spec said only
         * {@code "type": "string"}, which is how a vocabulary drifts with nobody noticing -- and it had
         * already drifted, since the curation UI carries a six-value union of which two
         * ({@code in_progress}, {@code needs_attention}) no producer here emits.
         * <p>
         * Wire key is {@code status} per curation-UI alignment; legacy {@code state} accepted on read.
         */
        @Schema(allowableValues = { "ok", "failed", "notRun", "notApplicable", "stale" })
        @JsonProperty("status")
        @JsonAlias({ "state" })
        private String state;
        @Nullable
        @JsonProperty("lastRun")
        @JsonAlias({ "lastRun" })
        private Date lastRun;
        /**
         * Simple class name of the latest audit event ({@code BatchInformationFetchingEvent},
         * {@code FailedPCAAnalysisEvent}, etc.). {@code null} when no event has been recorded.
         */
        @Nullable
        @JsonProperty("eventType")
        @JsonAlias({ "eventType" })
        private String eventType;
        /**
         * Note attached to the latest audit event, when present. Most useful for failed steps where
         * the failure reason is captured here.
         * <p>
         * Wire key is {@code details} per curation-UI alignment; legacy {@code message} accepted on read.
         */
        @Nullable
        @JsonProperty("details")
        @JsonAlias({ "message" })
        private String message;
        /**
         * How many design elements and samples each filter removed while the step ran, and the filter settings
         * that produced those numbers. Only the {@code sampleCorrelation} step carries it.
         * <p>
         * 🛑 {@code null} means NOT RECORDED, never "nothing was filtered". Filtering is not part of
         * preprocessing and nothing about it was stored before this field existed, so every dataset whose
         * correlation matrix predates it reads null until something recomputes the matrix.
         * <p>
         * 🛑 These counts describe the filter the correlation matrix was built under, which is not the one the
         * "filtered" data download uses -- that path builds its own configuration. {@code config} is served
         * alongside for exactly that reason; the counts are not interpretable without it.
         */
        @Nullable
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        private SampleCorrelationAnalysisPayload filterAttrition;
        /**
         * What the processed-vector creation did to the data: which raw quantitation type it started from, which
         * one it produced, how many cells were masked for missing values and for outliers, and whether the result
         * was quantile-normalized. Only the {@code preprocess} step carries it.
         * <p>
         * 🛑 This is as close as Gemma comes to a "normalization method", and it is not one -- there is no stored
         * algorithm name. {@code quantileNormalized} is a single recorded fact; everything else about how the
         * values are scaled is described by the preferred quantitation type's flags, served by
         * {@code GET /datasets/{id}/quantitationTypes}.
         * <p>
         * 🛑 {@code null} means not recorded. The payload has been written since the Phase C audit migration, so
         * this is populated for anything preprocessed since -- but not for older runs.
         */
        @Nullable
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        private ProcessedVectorComputationPayload processedVectors;

        public PipelineStepValueObject() {
        }

        public PipelineStepValueObject( String step, String state, @Nullable Date lastRun,
                @Nullable String eventType, @Nullable String message ) {
            this( step, state, lastRun, eventType, message, null, null );
        }

        public PipelineStepValueObject( String step, String state, @Nullable Date lastRun,
                @Nullable String eventType, @Nullable String message,
                @Nullable SampleCorrelationAnalysisPayload filterAttrition,
                @Nullable ProcessedVectorComputationPayload processedVectors ) {
            this.step = step;
            this.state = state;
            this.lastRun = lastRun;
            this.eventType = eventType;
            this.message = message;
            this.filterAttrition = filterAttrition;
            this.processedVectors = processedVectors;
        }
    }
}
