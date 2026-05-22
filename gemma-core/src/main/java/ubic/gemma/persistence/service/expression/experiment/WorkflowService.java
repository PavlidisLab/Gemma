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
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.util.Slice;

import java.util.Date;
import java.util.List;

/**
 * Service surface for the 8-state workflow lifecycle
 * (HANDOFF_WORKFLOW_STATE_STORAGE.md).
 *
 * <p>The state itself lives as a first-class column on {@code INVESTIGATION}
 * (see {@link Investigation#getWorkflowState()}); the history is derived
 * from the {@code AUDIT_EVENT} stream filtered on
 * {@link ubic.gemma.model.common.auditAndSecurity.eventType.WorkflowStateChangedEvent}.</p>
 *
 * <p>Implementations are responsible for:</p>
 * <ul>
 *   <li>validating transitions against the
 *       {@link WorkflowState#canTransitionTo(WorkflowState)} machine,</li>
 *   <li>persisting {@code workflowState} + {@code workflowStateEnteredAt}
 *       on {@link #advance(Investigation, WorkflowState, String, Long)},</li>
 *   <li>emitting exactly one
 *       {@code WorkflowStateChangedEvent} per real transition (idempotent
 *       no-ops emit nothing),</li>
 *   <li>answering queue-style queries for the curator worklist
 *       ({@link #queue(WorkflowState, String, String, Date, int, int)}).</li>
 * </ul>
 */
public interface WorkflowService {

    /**
     * @return the current workflow state of {@code investigation}. Never
     *         null (legacy rows default to {@link WorkflowState#Loaded}).
     */
    WorkflowState getCurrentState( Investigation investigation );

    /**
     * Retrieve the full chronological history of workflow transitions for a
     * dataset, derived from the AUDIT_EVENT stream filtered to
     * {@code WorkflowStateChangedEvent} rows.
     *
     * @param investigation the dataset whose workflow history to fetch.
     * @return zero-or-more events, oldest first. Empty list for a dataset
     *         that has never been transitioned via the workflow service
     *         (the backfilled 'Loaded' default is NOT a transition and
     *         does not appear in history).
     */
    List<AuditEvent> getHistory( Investigation investigation );

    /**
     * Advance the given dataset to {@code targetState}.
     *
     * <p>Idempotent on {@code targetState == current}: returns a
     * {@link WorkflowTransition} with {@code previousState == currentState}
     * and a {@code null} audit-event id; no row mutation, no audit event.</p>
     *
     * <p>The first argument is the {@link Investigation} (Auditable) target
     * because {@code @AuditedConditional} requires the auditable to be on
     * the argument list (the aspect locates it positionally). REST callers
     * resolve the dataset id to an {@link Investigation} before invoking.</p>
     *
     * @param dataset     the dataset to advance.
     * @param targetState the desired next state. Must satisfy
     *                    {@code current.canTransitionTo(targetState)} OR
     *                    equal {@code current} (idempotent no-op).
     * @param reason      optional human-readable rationale; recorded on the
     *                    audit event note.
     * @param ticketId    optional ticket id (forward-compatible with the
     *                    Ticket integration); the service currently records
     *                    the value on the audit note but does not interpret
     *                    it.
     * @return the transition record; never null.
     * @throws DisallowedWorkflowTransitionException if the transition is
     *                                               not permitted by the
     *                                               state machine.
     */
    WorkflowTransition advance( Investigation dataset, WorkflowState targetState,
            @Nullable String reason, @Nullable Long ticketId );

    /**
     * Return datasets currently in {@code state}, oldest entry first
     * (so the curator's "what's been waiting longest" view is the natural
     * read).
     *
     * @param state        required; restrict to this workflow state.
     * @param datasetType  optional; "expression_experiment" or
     *                     "preboarding_experiment". Currently only the
     *                     former is implemented.
     * @param assignee     optional; restrict to datasets that have an OPEN
     *                     ticket assigned to this user login.
     * @param since        optional; restrict to datasets whose
     *                     {@code workflowStateEnteredAt} is on or after
     *                     this instant.
     * @param offset       pagination offset.
     * @param limit        pagination page size.
     */
    Slice<WorkflowQueueEntry> queue( WorkflowState state,
            @Nullable String datasetType,
            @Nullable String assignee,
            @Nullable Date since,
            int offset, int limit );
}
