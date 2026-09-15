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

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ubic.gemma.model.pipeline.SchedulerKind;

/**
 * Nextflow-backed scheduler. Selected by {@code spring.profiles.active=scheduler-nextflow}.
 *
 * <p>STUB — wire-shape TBD pending the Nextflow Tower / REST surface decision.</p>
 */
@Component
@Profile("scheduler-nextflow")
@Primary
public class NextflowPipelineScheduler implements PipelineScheduler {

    @Override
    public SchedulerKind kind() {
        return SchedulerKind.NEXTFLOW;
    }

    @Override
    public SchedulerHandle submit( SubmitRequest req ) throws PipelineSchedulerException {
        throw new PipelineSchedulerException( "Nextflow scheduler not yet implemented" );
    }

    @Override
    public JobSnapshot poll( SchedulerHandle handle ) throws PipelineSchedulerException {
        throw new PipelineSchedulerException( "Nextflow scheduler not yet implemented" );
    }

    @Override
    public void cancel( SchedulerHandle handle ) throws PipelineSchedulerException {
        throw new PipelineSchedulerException( "Nextflow scheduler not yet implemented" );
    }
}
