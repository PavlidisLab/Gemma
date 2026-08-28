/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Report returned by {@code POST /datasets/{id}/designPreflight}. Predicts what would happen if the
 * accompanying proposed {@link ExperimentalDesignValueObject} were PUT to {@code /datasets/{id}/design}.
 * <p>
 * Contract:
 * <ul>
 *     <li>{@link #blockers} non-empty &rArr; the corresponding PUT would return 4xx. Caller must fix the payload.</li>
 *     <li>{@link #blockers} empty AND {@link #requiresForce()} false
 *         &rArr; PUT would succeed without {@code ?force=true}.</li>
 *     <li>{@link #blockers} empty AND {@link #requiresForce()} true
 *         &rArr; PUT needs {@code ?force=true} (admin) to consent to the consequences.</li>
 * </ul>
 *
 * @author ogan
 */
@Getter
@Setter
public class DesignPreflightReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Hard validation errors. A non-empty list means the PUT would be rejected; fix the payload and re-run.
     */
    private List<Blocker> blockers = new ArrayList<>();

    private Summary summary = new Summary();

    private List<EntityRef> factorsToDelete = new ArrayList<>();
    private List<EntityRef> factorValuesToDelete = new ArrayList<>();

    /**
     * Kept factors whose name, description or category the proposal rewrites.
     * <p>
     * Nothing is created or deleted by such an edit, so every counter above it stays at zero and the report
     * used to describe a real change as {@code unchanged}. The apply path has always performed these — see
     * {@code isNoOpDesignApply}, which consults the same comparison — so the gap was in what the report could
     * say, not in what a PUT would do.
     */
    private List<EntityRef> factorsToUpdate = new ArrayList<>();

    /**
     * Kept factor values the proposal edits in place: a statement re-termed, evidence attached, the baseline
     * flag flipped, a measurement retimed, the deprecated free-text value rewritten.
     * <p>
     * cab hit the gap on GSE49354.1 (2026-08-27): re-terming one factor value's subject URI preflighted as
     * {@code {created: 0, updated: 0, deleted: 0, unchanged: 1}}, which reads as "nothing to do" for an edit
     * that a PUT would in fact apply.
     */
    private List<EntityRef> factorValuesToUpdate = new ArrayList<>();
    private List<AnalysisRef> differentialExpressionAnalysesToDelete = new ArrayList<>();

    /**
     * Subsets whose definitional factor-value anchors would be deleted. These are not blockers (subsets
     * carry no FK to FactorValue), but their semantics drift after the change, so they require the same
     * explicit consent as the analysis cascade — see {@link #requiresForce()}.
     */
    private List<SubsetRef> subsetsWithStaleAnchor = new ArrayList<>();

    /**
     * Whether applying this change needs explicit consent ({@code ?force=true}, admin) rather than proceeding
     * silently. True when the change would delete differential-expression analyses, or would leave a subset
     * defined by factor values that no longer exist.
     * <p>
     * Subsets are included deliberately. A cascade-deleted analysis is <em>gone</em>, and gone announces
     * itself — somebody re-runs it. An orphaned subset is still there, still named, still listed, and now
     * anchored on factor values that were deleted out from under it: it reads as valid. A false prompt costs a
     * curator one {@code force=true}; a false silence costs a subset that nobody notices is wrong until it
     * produces a wrong answer.
     * <p>
     * Consent is per-request, not per-consequence: a caller that forces past an analysis cascade also forces
     * past a stale anchor. Callers should therefore surface {@link #differentialExpressionAnalysesToDelete}
     * and {@link #subsetsWithStaleAnchor} separately so the curator sees which they are agreeing to.
     */
    @JsonProperty("requiresForce")
    @Schema(description = "True when applying this change needs ?force=true (admin) to consent — it would delete differential-expression analyses, or leave a subset anchored on factor values that no longer exist. Computed server-side so a client never has to re-derive the rule.")
    public boolean requiresForce() {
        return !differentialExpressionAnalysesToDelete.isEmpty() || !subsetsWithStaleAnchor.isEmpty();
    }

    /**
     * One reason the proposed payload cannot be applied. Each blocker is self-describing.
     */
    @Data
    public static class Blocker implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "Stable identifier for this blocker kind, e.g. UNKNOWN_FACTOR_VALUE_ID, FACTOR_TYPE_CHANGE_WITH_VALUES, ORPHAN_STATEMENT, ASSIGNMENT_REFERENCES_DELETED_FV.")
        private String type;

        private String message;

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long factorId;

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long factorValueId;

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long bioMaterialId;

        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Long statementId;

        public Blocker() {
        }

        public Blocker( String type, String message ) {
            this.type = type;
            this.message = message;
        }
    }

    @Data
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;

        private int factorsToDelete;
        private int factorValuesToDelete;
        private int factorsToCreate;
        private int factorValuesToCreate;
        /** @see DesignPreflightReport#getFactorsToUpdate() */
        private int factorsToUpdate;
        /** @see DesignPreflightReport#getFactorValuesToUpdate() */
        private int factorValuesToUpdate;
        private int differentialExpressionAnalysesToDelete;
        private int subsetsWithStaleAnchor;
        private int biomaterialsWithChangedAssignments;
    }

    @Data
    public static class EntityRef implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Long id;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String name;
    }

    @Data
    public static class AnalysisRef implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Long id;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String name;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Set when the analysis is scoped to a subset factor value (subset-level analysis). Null for whole-experiment analyses.")
        private final Long subsetFactorValueId;
    }

    @Data
    public static class SubsetRef implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Long id;
        @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String name;
        @Schema(description = "FactorValue IDs that this subset currently relies on (transitively, via its biomaterials) that would be deleted by the proposed change.")
        private final List<Long> lostFactorValueIds;
    }
}