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

import lombok.Value;
import org.springframework.lang.Nullable;
import ubic.gemma.model.pipeline.JobState;

/**
 * Scheduler-side state read via {@link PipelineScheduler#poll}. Used by the
 * reconciler to update Gemma's view when push events are missing. {@link #raw}
 * carries the scheduler's native status string for diagnostics.
 */
@Value
public class JobSnapshot {
    JobState state;
    @Nullable
    String raw;
    @Nullable
    String message;
}
