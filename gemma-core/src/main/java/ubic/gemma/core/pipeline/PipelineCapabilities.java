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
package ubic.gemma.core.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.springframework.lang.Nullable;

/**
 * What the active pipeline scheduler can do (§3.4 #2). The curation UI feature-gates off this — it
 * hides the suspend / log / artifact controls when the active scheduler doesn't support them, rather
 * than showing buttons that 404 or 409.
 *
 * <p>Wire shape is snake_case for the UI (§1.3). {@link #kind} is {@code null} when no scheduler is
 * wired (no {@code scheduler-*} profile active).</p>
 */
@Value
public class PipelineCapabilities {

    /** Active scheduler kind (lowercase wire value: {@code mock} / {@code luigi} / {@code nextflow}), or null. */
    @Nullable
    @JsonProperty("kind")
    String kind;

    @JsonProperty("supports_suspend")
    boolean supportsSuspend;

    @JsonProperty("supports_log")
    boolean supportsLog;

    @JsonProperty("supports_artifacts")
    boolean supportsArtifacts;
}
