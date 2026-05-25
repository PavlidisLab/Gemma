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

/**
 * Payload handed to {@link PipelineScheduler#submit}. Scheduler-agnostic shape;
 * each impl translates to its own wire format.
 *
 * <p>{@link #gemmaJobId} is the Gemma-side {@code PipelineJob.id}. Each impl
 * must round-trip this value so push callbacks from the pipeline can locate
 * the originating row without a scheduler-side lookup (the callback URL
 * embeds the Gemma job id explicitly).</p>
 */
@Value
public class SubmitRequest {
    Long gemmaJobId;
    String pipeline;
    Long experimentId;
    @Nullable
    String paramsJson;
}
