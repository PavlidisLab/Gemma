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
 * Why a {@link PipelineJob} failed — the "when appropriate" in the mop-up model (§3.2).
 * Drives whether a failed job is auto-eligible for retry.
 *
 * <p>Source of truth is the pipeline itself: it reports {@code failureClass} in the terminal
 * {@code error} event payload (D9). Gemma persists that verbatim; it does not yet second-guess
 * with an exit-code heuristic (unclassified → {@link #UNKNOWN}).</p>
 */
public enum FailureClass {
    /** SRA throttle, OOM, node died, scheduler lost the handle, transient network — retry-eligible. */
    TRANSIENT,
    /** Malformed input, no raw data, validation reject, unsupported chemistry — not retried without override. */
    PERMANENT,
    /** No signal from the pipeline / unclassified — surfaced for the curator to decide. */
    UNKNOWN
}
