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
import ubic.gemma.model.pipeline.SchedulerKind;

/**
 * Opaque pointer to a scheduler-side job. The tuple {@code (kind, id)} is the
 * scheduler-side primary key and is stored on {@code PipelineJob.schedulerKind}
 * + {@code PipelineJob.schedulerHandle}.
 */
@Value
public class SchedulerHandle {
    SchedulerKind kind;
    String id;
}
