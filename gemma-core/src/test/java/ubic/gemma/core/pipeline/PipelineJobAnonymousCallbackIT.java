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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.BatchRollup;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A real scheduler reports through {@code /internal/pipeline}, which authenticates with the callback
 * token, not as a Gemma user — so {@link PipelineJobBatchService#recordEvent} runs with an ANONYMOUS
 * security context. The other pipeline ITs deliver events as the test admin (the scripted mock calls
 * the service in-process), so none of them cover this. A terminal failure must still land the job
 * FAILED and open the {@code PIPELINE_FAILED} ticket; the ticket step swallows its own errors, so an
 * authorization failure there would otherwise pass silently as "no ticket".
 */
@ActiveProfiles("scheduler-mock")
class PipelineJobAnonymousCallbackIT extends BaseSpringContextTest5 {

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private MockSchedulerControl control;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UserManager userManager;

    @BeforeEach
    void resetMock() {
        control.reset();
    }

    @AfterEach
    void restoreAdmin() {
        runAsAdmin();
    }

    @Test
    void anonymousCallback_recordsTerminalFailureAndOpensTicket() {
        // As AdminPipelineWebService.submitBatch does: the submitter is the logged-in user (the test admin).
        User submitter = userManager.getCurrentUser();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), silent() );

        PipelineJobBatch batch = pipelineJobBatchService.submit(
                "sc-annotation", Collections.singletonList( ee ), submitter, null, "anonymous callback IT" );
        Long jobId = batch.getJobs().iterator().next().getId();

        runAsAnonymous();
        pipelineJobBatchService.recordEvent( jobId, "stage", "{\"stage\":\"LOAD_CTA\"}" );
        pipelineJobBatchService.recordEvent( jobId, "error",
                "{\"failureClass\":\"PERMANENT\",\"message\":\"no raw data\"}" );

        runAsAdmin();
        BatchRollup rollup = pipelineJobBatchService.computeRollup( batch.getId() );
        assertThat( rollup.failed ).isEqualTo( 1 );
        assertThat( rollup.failedPermanent ).isEqualTo( 1 );

        List<Ticket> open = ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() );
        assertThat( open )
                .as( "the failure ticket must open even though the callback carried no user" )
                .extracting( Ticket::getType )
                .containsExactly( TicketType.PIPELINE_FAILED );
    }

    /** POLL + STALL: the mock emits nothing by itself, so every event comes from the test's callback. */
    private static Scenario silent() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.STALL;
        s.transport = Scenario.Transport.POLL;
        return s;
    }
}
