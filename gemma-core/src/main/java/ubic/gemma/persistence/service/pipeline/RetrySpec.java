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
package ubic.gemma.persistence.service.pipeline;

import org.springframework.lang.Nullable;

import java.util.List;

/**
 * How to run a mop-up (§3.2). {@code retryFailed(batchId, new RetrySpec())} — the zero-arg default —
 * is "retry every transient failure, leave permanents for human eyes."
 *
 * <p>Plain public-field POJO so it round-trips from the REST body with no annotations.</p>
 */
public class RetrySpec {

    /** Skip PERMANENT/UNKNOWN failures (default). When false, retry any eligible failed job. */
    public boolean onlyRetryable = true;

    /** Restrict to these job ids; {@code null} = all eligible current-attempt failures in the batch. */
    @Nullable
    public List<Long> jobIds;

    /** Params override applied to the retried attempts (batch-level default, D7); {@code null} inherits. */
    @Nullable
    public String paramsOverrideJson;

    public RetrySpec() {
    }
}
