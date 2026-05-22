package ubic.gemma.rest;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.expression.experiment.GeeqValueObject;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

/**
 * Snapshot of every preprocessing/analysis pipeline step for a single experiment.
 * <p>
 * Returned by {@code GET /datasets/{dataset}/pipeline-status}. The duplicated curation/permissions/quality
 * fields ({@code troubled}, {@code curationNote}, {@code isPublic}, {@code geeq}) are included as a convenience
 * so that callers driving a pre-public checklist can render everything from a single round trip.
 */
@Getter
@Setter
public class PipelineStatusValueObject {

    private Long experimentId;
    private List<PipelineStepValueObject> steps;

    private boolean hasBatchInformation;
    private boolean hasDifferentialExpressionAnalysis;
    private boolean hasCoexpressionAnalysis;

    private boolean troubled;
    private String troubleDetails;
    private boolean needsAttention;
    /**
     * Admin-only; remains {@code null} for non-administrators.
     */
    @Nullable
    private String curationNote;

    private boolean isPublic;
    @Nullable
    private GeeqValueObject geeq;

    public boolean getIsPublic() {
        return isPublic;
    }

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
         */
        private String state;
        @Nullable
        private Date lastRun;
        /**
         * Simple class name of the latest audit event ({@code BatchInformationFetchingEvent},
         * {@code FailedPCAAnalysisEvent}, etc.). {@code null} when no event has been recorded.
         */
        @Nullable
        private String eventType;
        /**
         * Note attached to the latest audit event, when present. Most useful for failed steps where
         * the failure reason is captured here.
         */
        @Nullable
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