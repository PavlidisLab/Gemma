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
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-unit test of {@link ScriptedMockScheduler} — no Spring, deterministic virtual
 * clock, no {@code Thread.sleep}. Covers the three things the smoke-toy mock could not
 * do: fail with a class, be driven deterministically, and exercise both transports.
 */
class ScriptedMockSchedulerTest {

    private PipelineJobBatchService service;
    private ScriptedMockScheduler scheduler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = mock( PipelineJobBatchService.class );
        ObjectProvider<PipelineJobBatchService> provider = mock( ObjectProvider.class );
        when( provider.getObject() ).thenReturn( service );
        Environment env = mock( Environment.class );
        scheduler = new ScriptedMockScheduler( provider, env );
    }

    @Test
    void push_advanceFiresStagesInOrderOnlyWhenDue() {
        scheduler.setScenario( 1L, push( Scenario.Outcome.SUCCEED,
                stage( 0, "stage", "{}" ), stage( 1000, "completed", "{}" ) ) );
        SchedulerHandle h = scheduler.submit( req( 100L, 1L ) );

        // Nothing due before the first advance beyond t=0 fires only the 0ms stage.
        scheduler.advance( 0 );
        verify( service ).recordEvent( eq( 100L ), eq( "stage" ), any() );
        verify( service, never() ).recordEvent( eq( 100L ), eq( "completed" ), any() );

        // The terminal stage fires once the clock passes its afterMs.
        scheduler.advance( 1000 );
        InOrder inOrder = inOrder( service );
        inOrder.verify( service ).recordEvent( eq( 100L ), eq( "stage" ), any() );
        inOrder.verify( service ).recordEvent( eq( 100L ), eq( "completed" ), any() );
        assertThat( h.getId() ).isNotBlank();
    }

    @Test
    void push_failEventCarriesFailureClassInPayload() {
        scheduler.setScenario( 2L, push( Scenario.Outcome.FAIL,
                stage( 0, "error", "{\"failureClass\":\"TRANSIENT\",\"message\":\"throttle\"}" ) ) );
        scheduler.submit( req( 200L, 2L ) );
        scheduler.advance( 0 );
        verify( service ).recordEvent( eq( 200L ), eq( "error" ), contains( "TRANSIENT" ) );
    }

    @Test
    void poll_reportsStateFromVirtualClockWithoutPushing() {
        scheduler.setScenario( 3L, poll( Scenario.Outcome.STALL, stage( 1000, "stage", "{}" ) ) );
        SchedulerHandle h = scheduler.submit( req( 300L, 3L ) );

        // Before the stage is due: QUEUED.
        assertThat( scheduler.poll( h ).getState() ).isEqualTo( JobState.QUEUED );

        // A POLL scenario is surfaced through poll(), never pushed.
        scheduler.advance( 1000 );
        verify( service, never() ).recordEvent( any(), any(), any() );
        assertThat( scheduler.poll( h ).getState() ).isEqualTo( JobState.RUNNING );
    }

    @Test
    void poll_unknownHandleIsNull() {
        assertThat( scheduler.poll( new SchedulerHandle( ubic.gemma.model.pipeline.SchedulerKind.MOCK, "nope" ) ) )
                .isNull();
    }

    @Test
    void cancel_thenAdvanceEmitsKilled() {
        scheduler.setScenario( 4L, push( Scenario.Outcome.SUCCEED,
                stage( 0, "stage", "{}" ), stage( 5000, "completed", "{}" ) ) );
        SchedulerHandle h = scheduler.submit( req( 400L, 4L ) );
        scheduler.cancel( h );
        scheduler.advance( 100 );
        verify( service ).recordEvent( eq( 400L ), eq( "killed" ), isNull() );
        verify( service, never() ).recordEvent( eq( 400L ), eq( "completed" ), any() );
    }

    @Test
    void defaultScenario_usedWhenNoneRegistered() {
        scheduler.submit( req( 500L, 999L ) );
        scheduler.advance( 5000 );
        verify( service ).recordEvent( eq( 500L ), eq( "completed" ), any() );
    }

    @Test
    void reset_clearsJobsScenariosAndClock() {
        scheduler.setScenario( 6L, push( Scenario.Outcome.SUCCEED, stage( 0, "completed", "{}" ) ) );
        SchedulerHandle h = scheduler.submit( req( 600L, 6L ) );
        scheduler.reset();
        assertThat( scheduler.listScenarios() ).isEmpty();
        assertThat( scheduler.poll( h ) ).isNull();
    }

    @Test
    void readLog_servesScenarioLinesWithAdvancingCursor() {
        Scenario s = push( Scenario.Outcome.SUCCEED, stage( 0, "completed", "{}" ) );
        s.logLines.add( "line one" );
        s.logLines.add( "line two" );
        scheduler.setScenario( 7L, s );
        SchedulerHandle h = scheduler.submit( req( 700L, 7L ) );

        assertThat( scheduler.supportsLog() ).isTrue();
        LogChunk chunk = scheduler.readLog( h, 0, 64 * 1024 );
        assertThat( chunk.getText() ).contains( "line one" ).contains( "line two" );
        assertThat( chunk.isEof() ).isTrue();
        assertThat( chunk.getNextOffset() ).isGreaterThan( 0 );

        // A read from the end is empty + eof.
        LogChunk tail = scheduler.readLog( h, chunk.getNextOffset(), 64 * 1024 );
        assertThat( tail.getText() ).isEmpty();
        assertThat( tail.isEof() ).isTrue();
    }

    @Test
    void readArtifact_servesCannedPayload() {
        scheduler.setScenario( 8L, push( Scenario.Outcome.SUCCEED, stage( 0, "completed", "{}" ) ) );
        SchedulerHandle h = scheduler.submit( req( 800L, 8L ) );
        assertThat( scheduler.supportsArtifacts() ).isTrue();
        Artifact a = scheduler.readArtifact( h, "web_summary.html" );
        assertThat( a.getContent() ).isNotEmpty();
        assertThat( a.getContentType() ).isEqualTo( "text/html" );
        assertThat( a.getName() ).isEqualTo( "web_summary.html" );
    }

    @Test
    void afterPropertiesSet_throwsUnderProductionProfile() {
        Environment prod = mock( Environment.class );
        when( prod.acceptsProfiles( any( Profiles.class ) ) ).thenReturn( true );
        @SuppressWarnings("unchecked")
        ObjectProvider<PipelineJobBatchService> provider = mock( ObjectProvider.class );
        ScriptedMockScheduler s = new ScriptedMockScheduler( provider, prod );
        assertThatThrownBy( s::afterPropertiesSet ).isInstanceOf( IllegalStateException.class );
    }

    // -----------------------------------------------------------------------
    // builders
    // -----------------------------------------------------------------------

    private static SubmitRequest req( Long gemmaJobId, Long eeId ) {
        return new SubmitRequest( gemmaJobId, "test-pipeline", eeId, null );
    }

    private static Scenario push( Scenario.Outcome outcome, Scenario.Stage... stages ) {
        return scenario( outcome, Scenario.Transport.PUSH, stages );
    }

    private static Scenario poll( Scenario.Outcome outcome, Scenario.Stage... stages ) {
        return scenario( outcome, Scenario.Transport.POLL, stages );
    }

    private static Scenario scenario( Scenario.Outcome outcome, Scenario.Transport transport, Scenario.Stage... stages ) {
        Scenario s = new Scenario();
        s.outcome = outcome;
        s.transport = transport;
        for ( Scenario.Stage st : stages ) {
            s.stages.add( st );
        }
        return s;
    }

    private static Scenario.Stage stage( long afterMs, String kind, String payloadJson ) {
        Scenario.Stage st = new Scenario.Stage();
        st.afterMs = afterMs;
        st.kind = kind;
        st.payloadJson = payloadJson;
        return st;
    }
}
