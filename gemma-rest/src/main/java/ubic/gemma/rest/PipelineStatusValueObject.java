package ubic.gemma.rest;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("needsAttention")
    @JsonAlias({ "needsAttention" })
    private boolean needsAttention;

    /**
     * Admin-only; remains {@code null} for non-administrators.
     */
    @Nullable
    @JsonProperty("curationNote")
    @JsonAlias({ "curationNote" })
    private String curationNote;

    private boolean isPublic;

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

        /**
         * One of {@code batchInfo}, {@code preprocess}, {@code pca}, {@code dea}, {@code coexpression},
         * {@code missingValue}.
         */
        private String step;
        /**
         * One of {@code ok}, {@code failed}, {@code notRun}, {@code notApplicable}.
         * <p>
         * Wire key is {@code status} per curation-UI alignment; legacy {@code state} accepted on read.
         */
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

        public PipelineStepValueObject() {
        }

        public PipelineStepValueObject( String step, String state, @Nullable Date lastRun,
                @Nullable String eventType, @Nullable String message ) {
            this.step = step;
            this.state = state;
            this.lastRun = lastRun;
            this.eventType = eventType;
            this.message = message;
        }
    }
}
