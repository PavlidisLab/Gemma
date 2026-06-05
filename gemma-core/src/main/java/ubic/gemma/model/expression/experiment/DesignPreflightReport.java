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
 *     <li>{@link #blockers} empty AND {@link #differentialExpressionAnalysesToDelete} empty
 *         &rArr; PUT would succeed without {@code ?force=true}.</li>
 *     <li>{@link #blockers} empty AND {@link #differentialExpressionAnalysesToDelete} non-empty
 *         &rArr; PUT needs {@code ?force=true} (admin) to consent to the cascade.</li>
 *     <li>{@link #subsetsWithStaleAnchor} is informational; never blocks, never forces.</li>
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
    private List<AnalysisRef> differentialExpressionAnalysesToDelete = new ArrayList<>();

    /**
     * Subsets whose definitional factor-value anchors would be deleted. These are not blockers (subsets
     * carry no FK to FactorValue), but their semantics drift after the change.
     */
    private List<SubsetRef> subsetsWithStaleAnchor = new ArrayList<>();

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