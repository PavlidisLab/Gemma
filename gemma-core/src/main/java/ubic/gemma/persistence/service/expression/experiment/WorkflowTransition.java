/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.Date;

/**
 * Return value of
 * {@link WorkflowService#advance}. Carries
 * enough for the caller (and the REST layer) to round-trip a 200 response
 * without re-fetching the dataset.
 *
 * <p>{@code idempotent} is true when the caller advanced to the state the
 * dataset was already in; in that case {@code previousState == currentState}
 * and {@code auditEventId} is null (no event emitted).</p>
 */
public class WorkflowTransition {

    private final Long datasetId;
    private final WorkflowState previousState;
    private final WorkflowState currentState;
    @Nullable
    private final Date enteredCurrentStateAt;
    @Nullable
    private final Long auditEventId;

    public WorkflowTransition( Long datasetId, WorkflowState previousState, WorkflowState currentState,
            @Nullable Date enteredCurrentStateAt, @Nullable Long auditEventId ) {
        this.datasetId = datasetId;
        this.previousState = previousState;
        this.currentState = currentState;
        this.enteredCurrentStateAt = enteredCurrentStateAt;
        this.auditEventId = auditEventId;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public WorkflowState getPreviousState() {
        return previousState;
    }

    public WorkflowState getCurrentState() {
        return currentState;
    }

    @Nullable
    public Date getEnteredCurrentStateAt() {
        return enteredCurrentStateAt;
    }

    @Nullable
    public Long getAuditEventId() {
        return auditEventId;
    }

    public boolean isIdempotent() {
        return previousState == currentState;
    }
}
