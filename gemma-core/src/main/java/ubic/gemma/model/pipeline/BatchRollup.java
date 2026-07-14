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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Derived disposition of a {@link PipelineJobBatch}, computed over its <em>current</em> attempts
 * (§3.2). Never persisted — {@code PIPELINE_JOB_BATCH.state} stays the curator's explicit lifecycle
 * flag; this VO is the cheap repaint source the UI reads.
 *
 * <p>{@link #needsAttention} drives the "this batch isn't finished" badge. A retry in flight
 * supersedes the failure it replaces, so {@code failed} drops automatically while the retry runs —
 * no separate "in flight" clause.</p>
 *
 * <p>Wire shape is snake_case for the curation UI (UIB) — see §1.3.</p>
 */
public class BatchRollup {

    @JsonProperty("total")
    public int total;
    @JsonProperty("pending")
    public int pending;
    @JsonProperty("queued")
    public int queued;
    @JsonProperty("running")
    public int running;
    @JsonProperty("done")
    public int done;
    @JsonProperty("failed")
    public int failed;
    @JsonProperty("cancelled")
    public int cancelled;

    /** FAILED current attempts with {@link FailureClass#TRANSIENT} — auto-eligible for mop-up. */
    @JsonProperty("failed_retryable")
    public int failedRetryable;
    /** FAILED current attempts that are not transient (PERMANENT / UNKNOWN). */
    @JsonProperty("failed_permanent")
    public int failedPermanent;

    /** {@code state == OPEN && failed > 0} — the batch has current failures a curator should act on. */
    @JsonProperty("needs_attention")
    public boolean needsAttention;
    /** Every current attempt is in a terminal state. */
    @JsonProperty("terminal")
    public boolean terminal;
}
