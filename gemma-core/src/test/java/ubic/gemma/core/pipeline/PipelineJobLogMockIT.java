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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Log + artifact proxy through the service (§3.5 / task 5): the scripted mock serves a job's
 * {@code logLines} and a canned artifact; the service proxies them without persisting anything.
 */
@ActiveProfiles("scheduler-mock")
class PipelineJobLogMockIT extends BaseSpringContextTest5 {

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private MockSchedulerControl control;

    @BeforeEach
    void resetMock() {
        control.reset();
    }

    @Test
    void readJobLog_incrementalTail_andArtifact() {
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), withLog() );

        PipelineJobBatch batch = pipelineJobBatchService.submit(
                "test-pipeline", Collections.singletonList( ee ), submitter, null, "log IT" );
        Long jobId = batch.getJobs().iterator().next().getId();
        control.advance( 2000 );

        LogChunk chunk = pipelineJobBatchService.readJobLog( jobId, 0, 64 * 1024 );
        assertThat( chunk ).isNotNull();
        assertThat( chunk.getText() ).contains( "aligning" ).contains( "done" );
        assertThat( chunk.isEof() ).isTrue();

        // Incremental read from the cursor yields nothing more.
        LogChunk tail = pipelineJobBatchService.readJobLog( jobId, chunk.getNextOffset(), 64 * 1024 );
        assertThat( tail.getText() ).isEmpty();
        assertThat( tail.isEof() ).isTrue();

        Artifact artifact = pipelineJobBatchService.readJobArtifact( jobId, "web_summary.html" );
        assertThat( artifact ).isNotNull();
        assertThat( artifact.getContent() ).isNotEmpty();
        assertThat( artifact.getContentType() ).isEqualTo( "text/html" );
    }

    @Test
    void capabilities_reflectTheMockScheduler() {
        PipelineCapabilities caps = pipelineJobBatchService.capabilities();
        assertThat( caps.getKind() ).isEqualTo( "mock" );
        assertThat( caps.isSupportsLog() ).isTrue();
        assertThat( caps.isSupportsArtifacts() ).isTrue();
        assertThat( caps.isSupportsSuspend() ).isFalse();  // §3.4 #2 — stub until Slurm
    }

    private static Scenario withLog() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.SUCCEED;
        s.transport = Scenario.Transport.PUSH;
        Scenario.Stage a = new Scenario.Stage();
        a.afterMs = 0;
        a.kind = "stage";
        a.payloadJson = "{}";
        Scenario.Stage b = new Scenario.Stage();
        b.afterMs = 1000;
        b.kind = "completed";
        b.payloadJson = "{}";
        s.stages.add( a );
        s.stages.add( b );
        s.logLines.add( "aligning" );
        s.logLines.add( "done" );
        return s;
    }
}
