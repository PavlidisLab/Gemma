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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Outcome returned by the {@code PUT /datasets/{id}/design} apply path.
 * <p>
 * Carries:
 * <ul>
 *     <li>{@link #applied} — {@code true} when at least one factor / factor value / biomaterial
 *         assignment / design-metadata mutation was written; {@code false} for the idempotent
 *         no-op branch (the proposed design already matched the current design).</li>
 *     <li>{@link #design} — the freshly-rebuilt design value object after the apply (or the
 *         unchanged current design in the no-op branch).</li>
 *     <li>{@link #preflightAtApply} — the {@link DesignPreflightReport} computed at apply-time,
 *         the authoritative gate. Returned even on success so callers have a record of what
 *         was checked.</li>
 * </ul>
 * <p>
 * The no-op branch suppresses the {@code DesignChangeEvent} audit row so that repeated PUTs
 * of an already-applied design produce one event, not many.
 *
 * @author paul
 */
@Getter
@Setter
public class DesignApplyOutcome implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "True when at least one factor / factor value / biomaterial assignment / design-metadata mutation was written. False for the idempotent no-op branch.")
    private boolean applied;

    @Schema(description = "The freshly-rebuilt design value object after the apply (unchanged current design in the no-op branch).")
    private ExperimentalDesignValueObject design;

    @Schema(description = "The DesignPreflightReport computed at apply-time. The authoritative gate; returned even on success so callers have a record of what was checked.")
    private DesignPreflightReport preflightAtApply;

    public DesignApplyOutcome() {
    }

    public DesignApplyOutcome( boolean applied, ExperimentalDesignValueObject design, DesignPreflightReport preflightAtApply ) {
        this.applied = applied;
        this.design = design;
        this.preflightAtApply = preflightAtApply;
    }
}
