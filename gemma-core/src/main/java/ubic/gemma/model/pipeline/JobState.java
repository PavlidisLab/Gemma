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
package ubic.gemma.model.pipeline;

/**
 * Lifecycle of one {@link PipelineJob}.
 *
 * <p>State transitions:</p>
 * <pre>
 *   PENDING ──submit──▶ QUEUED ──start──▶ RUNNING ──┬──▶ DONE
 *                                                   ├──▶ FAILED
 *                                                   └──▶ CANCELLING ──ack──▶ CANCELLED
 * </pre>
 *
 * <p>{@code CANCELLING} is the intermediate state between curator-clicks-cancel and
 * scheduler-acknowledges-kill. Until the next push event (or poll fallback) confirms,
 * the job stays {@code CANCELLING}.</p>
 *
 * <p>{@code DONE}, {@code FAILED}, and {@code CANCELLED} are terminal.</p>
 */
public enum JobState {
    PENDING,
    QUEUED,
    RUNNING,
    DONE,
    FAILED,
    CANCELLING,
    CANCELLED;

    public boolean isTerminal() {
        return this == DONE || this == FAILED || this == CANCELLED;
    }
}
