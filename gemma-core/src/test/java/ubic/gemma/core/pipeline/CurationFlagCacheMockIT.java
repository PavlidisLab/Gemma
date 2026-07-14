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
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 11 (ticket-derived cache): opening / resolving a ticket keeps the legacy
 * {@code CurationDetails.troubled}/{@code needsAttention} columns in sync, so every existing
 * column-backed read/filter (e.g. {@code loadTroubledIds}) reflects ticket state. Also verifies the
 * task-9 → task-11 compose: an auto-opened {@code PIPELINE_FAILED} ticket marks the EE needs-attention.
 */
@ActiveProfiles("scheduler-mock")
class CurationFlagCacheMockIT extends BaseSpringContextTest5 {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private MockSchedulerControl control;

    @BeforeEach
    void resetMock() {
        control.reset();
    }

    @Test
    void qualityReviewTicket_setsTroubledAndNeedsAttention_andClearsOnResolve() {
        Contact reporter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();

        Ticket ticket = ticketService.openTicket( reporter, TicketType.QUALITY_REVIEW, "review it",
                Collections.singleton( TicketTarget.Factory.newInstance(
                        TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) ) );

        ExpressionExperiment reloaded = expressionExperimentService.load( ee.getId() );
        assertThat( reloaded.getCurationDetails().getTroubled() ).isTrue();
        assertThat( reloaded.getCurationDetails().getNeedsAttention() ).isTrue();
        // the /datasets filter path reads the column — it now reflects the ticket
        assertThat( expressionExperimentService.loadTroubledIds() ).contains( ee.getId() );

        ticketService.transition( ticket, TicketState.RESOLVED, reporter, "fixed" );

        ExpressionExperiment afterResolve = expressionExperimentService.load( ee.getId() );
        assertThat( afterResolve.getCurationDetails().getTroubled() ).isFalse();
        assertThat( afterResolve.getCurationDetails().getNeedsAttention() ).isFalse();
        assertThat( expressionExperimentService.loadTroubledIds() ).doesNotContain( ee.getId() );
    }

    @Test
    void batchInfoTicket_setsNeedsAttentionButNotTroubled() {
        Contact reporter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();

        ticketService.openTicket( reporter, TicketType.BATCH_INFO_NEEDED, "batch info?",
                Collections.singleton( TicketTarget.Factory.newInstance(
                        TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) ) );

        ExpressionExperiment reloaded = expressionExperimentService.load( ee.getId() );
        assertThat( reloaded.getCurationDetails().getNeedsAttention() ).isTrue();
        assertThat( reloaded.getCurationDetails().getTroubled() ).isFalse();
    }

    @Test
    void pipelineFailure_marksNeedsAttentionViaAutoTicket() {
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), permanentFailure() );

        pipelineJobBatchService.submit( "rnaseq-quant", Collections.singletonList( ee ), submitter, null, "cache IT" );
        control.advance( 2000 );  // fail -> task-9 auto-opens a PIPELINE_FAILED ticket -> task-11 cache

        ExpressionExperiment reloaded = expressionExperimentService.load( ee.getId() );
        assertThat( reloaded.getCurationDetails().getNeedsAttention() ).isTrue();
        assertThat( reloaded.getCurationDetails().getTroubled() ).isFalse();  // PIPELINE_FAILED != troubled
    }

    private static Scenario permanentFailure() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.FAIL;
        s.failureClass = Scenario.FailureClass.PERMANENT;
        s.transport = Scenario.Transport.PUSH;
        Scenario.Stage a = new Scenario.Stage();
        a.afterMs = 0;
        a.kind = "stage";
        a.payloadJson = "{}";
        Scenario.Stage b = new Scenario.Stage();
        b.afterMs = 1000;
        b.kind = "error";
        b.payloadJson = "{\"failureClass\":\"PERMANENT\",\"message\":\"no raw data\"}";
        s.stages.add( a );
        s.stages.add( b );
        return s;
    }
}
