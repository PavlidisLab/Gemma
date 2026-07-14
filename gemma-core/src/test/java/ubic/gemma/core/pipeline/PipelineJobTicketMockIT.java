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
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PipelineJob → Ticket edge (§1.2 #1 / task 9): a PERMANENT/UNKNOWN job failure auto-opens a
 * {@code PIPELINE_FAILED} ticket targeting the EE; TRANSIENT failures don't (they're retry-eligible);
 * repeat failures for the same EE append to the one ticket rather than spamming.
 */
@ActiveProfiles("scheduler-mock")
class PipelineJobTicketMockIT extends BaseSpringContextTest5 {

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private MockSchedulerControl control;

    @Autowired
    private TicketService ticketService;

    @BeforeEach
    void resetMock() {
        control.reset();
    }

    @Test
    void permanentFailure_opensOneTicketWithDetail_dedupedAcrossBatches() {
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), permanentFailure() );

        // First failed batch → opens the ticket.
        pipelineJobBatchService.submit( "rnaseq-quant", Collections.singletonList( ee ), submitter, null, "ticket IT #1" );
        control.advance( 2000 );

        List<Ticket> open = ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() );
        assertThat( open ).hasSize( 1 );
        Ticket ticket = open.get( 0 );
        assertThat( ticket.getType() ).isEqualTo( TicketType.PIPELINE_FAILED );

        TicketValueObject vo = ticketService.loadValueObject( ticket.getId(), true );
        assertThat( vo.getEvents() )
                .anySatisfy( e -> assertThat( e.getPayload() ).contains( "PERMANENT" ) );

        // Second failed batch over the SAME EE → dedup: appends, does NOT open a second ticket.
        pipelineJobBatchService.submit( "rnaseq-quant", Collections.singletonList( ee ), submitter, null, "ticket IT #2" );
        control.advance( 2000 );
        assertThat( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) )
                .hasSize( 1 );
    }

    @Test
    void transientFailure_opensNoTicket() {
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), transientFailure() );

        pipelineJobBatchService.submit( "rnaseq-quant", Collections.singletonList( ee ), submitter, null, "transient IT" );
        control.advance( 2000 );

        assertThat( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) )
                .as( "transient failures are retry-eligible — no ticket" )
                .isEmpty();
    }

    private static Scenario permanentFailure() {
        return failure( Scenario.FailureClass.PERMANENT, "PERMANENT", "no raw data" );
    }

    private static Scenario transientFailure() {
        return failure( Scenario.FailureClass.TRANSIENT, "TRANSIENT", "SRA throttle" );
    }

    private static Scenario failure( Scenario.FailureClass fc, String fcName, String message ) {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.FAIL;
        s.failureClass = fc;
        s.transport = Scenario.Transport.PUSH;
        Scenario.Stage a = new Scenario.Stage();
        a.afterMs = 0;
        a.kind = "stage";
        a.payloadJson = "{}";
        Scenario.Stage b = new Scenario.Stage();
        b.afterMs = 1000;
        b.kind = "error";
        b.payloadJson = "{\"failureClass\":\"" + fcName + "\",\"message\":\"" + message + "\"}";
        s.stages.add( a );
        s.stages.add( b );
        return s;
    }
}
