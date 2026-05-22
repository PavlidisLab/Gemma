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
 * Lightweight projection used by {@code GET /workflow/queue}. One row per
 * dataset currently sitting in the queried state.
 *
 * <p>{@code currentAssignee} / {@code ticketCountOpen} are forward-compatible
 * with the Ticket integration that {@code AUDIT_AS_WORKFLOW_RECCE.md}
 * Phase B-3 will deliver; on this first cut they are populated via the
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService}
 * open-ticket lookup (count of OPEN/IN_PROGRESS tickets targeting the
 * dataset, and the assignee of the single such ticket if exactly one).
 *
 * <p>{@code datasetType} is a string rather than an enum so a forthcoming
 * {@code PreboardedExperiment} subclass can populate {@code "preboarded_experiment"}
 * without an enum-extension migration. // TODO(preboarded-integration): wire
 * the PreboardedExperiment path through the queue query once the subclass
 * lands.
 */
public class WorkflowQueueEntry {

    private final Long datasetId;
    private final String datasetType;
    @Nullable
    private final String accession;
    @Nullable
    private final Date enteredCurrentStateAt;
    @Nullable
    private String currentAssignee;
    private int ticketCountOpen;

    public WorkflowQueueEntry( Long datasetId, String datasetType, @Nullable String accession,
            @Nullable Date enteredCurrentStateAt ) {
        this.datasetId = datasetId;
        this.datasetType = datasetType;
        this.accession = accession;
        this.enteredCurrentStateAt = enteredCurrentStateAt;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public String getDatasetType() {
        return datasetType;
    }

    @Nullable
    public String getAccession() {
        return accession;
    }

    @Nullable
    public Date getEnteredCurrentStateAt() {
        return enteredCurrentStateAt;
    }

    @Nullable
    public String getCurrentAssignee() {
        return currentAssignee;
    }

    public void setCurrentAssignee( @Nullable String currentAssignee ) {
        this.currentAssignee = currentAssignee;
    }

    public int getTicketCountOpen() {
        return ticketCountOpen;
    }

    public void setTicketCountOpen( int ticketCountOpen ) {
        this.ticketCountOpen = ticketCountOpen;
    }

    /**
     * Helper for tests / queue assembly; never the persisted state.
     */
    public WorkflowState peekState() {
        return null; // not carried on the entry; queue() narrows by state already
    }
}
