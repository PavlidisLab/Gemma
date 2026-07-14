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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

/**
 * Tops up throttled batches (§3.4 #1). Runs every N seconds; for each dispatchable batch (OPEN, not
 * held, with pending jobs) it launches {@code PENDING} jobs up to the batch's {@code maxConcurrent}
 * budget as in-flight jobs reach terminal. The throttle decision itself lives in
 * {@link PipelineJobBatchService#dispatchPending()}; this is just the timer.
 *
 * <p>Profile-gated on {@link EnvironmentProfiles#SCHEDULER} (like {@link JobReconciler}) so it only
 * fires on the node that owns scheduled background work — not in every CLI/REST/test context.</p>
 */
@Component
@Profile(EnvironmentProfiles.SCHEDULER)
@Slf4j
public class PipelineJobDispatcher {

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    /**
     * Every 30s by default. Override with {@code gemma.pipeline.dispatcher.intervalMs}.
     */
    @Scheduled(fixedDelayString = "${gemma.pipeline.dispatcher.intervalMs:30000}")
    public void tick() {
        int dispatched = pipelineJobBatchService.dispatchPending();
        if ( dispatched > 0 ) {
            log.debug( "PipelineJobDispatcher: dispatched {} pending jobs", dispatched );
        }
    }
}
