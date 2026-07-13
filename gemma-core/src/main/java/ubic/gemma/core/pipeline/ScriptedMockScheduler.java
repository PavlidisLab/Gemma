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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.SchedulerKind;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Programmable, deterministic, failure-capable mock scheduler. Selected by
 * {@code spring.profiles.active=scheduler-mock}. Replaces the smoke-toy
 * {@code MockPipelineScheduler}, which ran a fixed wall-clock lifecycle, always
 * succeeded, and was poll-only.
 *
 * <p>Behaviour is driven by a {@link Scenario} per experiment (with a fallback). The
 * clock is a <em>virtual</em> millisecond counter advanced only by {@link #advance};
 * there is no background timer — so tests assert without {@code Thread.sleep} and a
 * UIB dev can step the clock over HTTP.</p>
 *
 * <ul>
 *   <li>{@link Scenario.Transport#PUSH}: {@link #advance} fires each due stage through
 *       {@link PipelineJobBatchService#recordEvent} — the same write path the real
 *       internal callback uses.</li>
 *   <li>{@link Scenario.Transport#POLL}: {@link #poll} returns the {@link JobState} of
 *       the latest due stage; the reconciler observes drift and records it.</li>
 * </ul>
 *
 * <p>Reports kind {@code MOCK} so production code can never mistake a mock-dispatched
 * job for a real one. Wired for dev/test only — {@link #assertNotProduction} fails
 * fast if the {@code production} profile is somehow co-active.</p>
 */
@Component
@Profile("scheduler-mock")
@Primary
@Slf4j
public class ScriptedMockScheduler implements PipelineScheduler, MockSchedulerControl, InitializingBean {

    private static final SchedulerKind MOCK_KIND = SchedulerKind.MOCK;

    /** Built-in fallback when no scenario is registered: succeed after a couple of ticks. */
    private static final Scenario DEFAULT_SCENARIO = defaultSuccess();

    /**
     * Resolved lazily at {@link #advance}-time to break the bean cycle with
     * {@code PipelineJobBatchServiceImpl} (which injects a {@link PipelineScheduler}).
     * {@code getObject()} returns the transactional proxy, so each fired event runs in
     * its own {@code recordEvent} transaction.
     */
    private final ObjectProvider<PipelineJobBatchService> serviceProvider;

    private final Environment environment;

    /** Virtual clock, milliseconds. Only {@link #advance} (synchronized) mutates it. */
    private volatile long clock = 0L;

    private final ConcurrentHashMap<String, MockJob> jobs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Scenario> scenariosByEe = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> submitCounts = new ConcurrentHashMap<>();

    @Nullable
    private volatile Scenario fallbackScenario;

    @Autowired
    public ScriptedMockScheduler( ObjectProvider<PipelineJobBatchService> serviceProvider, Environment environment ) {
        this.serviceProvider = serviceProvider;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        assertNotProduction();
    }

    void assertNotProduction() {
        if ( environment != null && environment.acceptsProfiles( Profiles.of( EnvironmentProfiles.PRODUCTION ) ) ) {
            throw new IllegalStateException(
                    "scheduler-mock profile must not be active in production — it exposes dev-only "
                            + "/admin/pipeline/_mock control endpoints and a fake scheduler" );
        }
    }

    // -----------------------------------------------------------------------
    // PipelineScheduler
    // -----------------------------------------------------------------------

    @Override
    public SchedulerKind kind() {
        return MOCK_KIND;
    }

    @Override
    public SchedulerHandle submit( SubmitRequest req ) {
        Long eeId = req.getExperimentId();
        Scenario scenario = scenariosByEe.get( eeId );
        if ( scenario == null ) {
            scenario = fallbackScenario != null ? fallbackScenario : DEFAULT_SCENARIO;
        }
        int attempt = eeId != null ? submitCounts.merge( eeId, 1, Integer::sum ) : 1;
        String handle = UUID.randomUUID().toString();
        // submittedAtClock anchors this job's stage timeline to the current virtual clock.
        // Deliberately does NOT fire events here — submit() runs inside the service's own
        // @Transactional, before the job row is committed / visible to a fresh recordEvent tx.
        jobs.put( handle, new MockJob( req.getGemmaJobId(), eeId, scenario, clock, attempt ) );
        log.info( "ScriptedMockScheduler.submit gemmaJobId={} ee={} attempt={} -> handle={} ({})",
                req.getGemmaJobId(), eeId, attempt, handle, scenario.outcome );
        return new SchedulerHandle( MOCK_KIND, handle );
    }

    @Override
    public JobSnapshot poll( SchedulerHandle handle ) {
        MockJob j = jobs.get( handle.getId() );
        if ( j == null ) {
            return null;
        }
        if ( j.cancelRequested && !j.terminalFired ) {
            return new JobSnapshot( JobState.CANCELLED, "cancelled", null );
        }
        Scenario sc = effectiveScenario( j );
        long elapsed = clock - j.submittedAtClock;
        Scenario.Stage latest = null;
        for ( Scenario.Stage s : sc.stages ) {
            if ( s.afterMs <= elapsed ) {
                latest = s;
            } else {
                break;
            }
        }
        if ( latest == null ) {
            return new JobSnapshot( JobState.QUEUED, "queued", null );
        }
        return new JobSnapshot( kindToState( latest.kind ), latest.kind, latest.payloadJson );
    }

    @Override
    public void cancel( SchedulerHandle handle ) {
        MockJob j = jobs.get( handle.getId() );
        if ( j == null ) {
            log.warn( "ScriptedMockScheduler.cancel: unknown handle {}", handle.getId() );
            return;
        }
        j.cancelRequested = true;
        log.info( "ScriptedMockScheduler.cancel handle={}", handle.getId() );
    }

    // -----------------------------------------------------------------------
    // MockSchedulerControl
    // -----------------------------------------------------------------------

    @Override
    public synchronized void advance( long ms ) {
        if ( ms < 0 ) {
            throw new IllegalArgumentException( "advance(ms) requires ms >= 0, got " + ms );
        }
        clock += ms;
        for ( MockJob j : jobs.values() ) {
            if ( j.scenario.transport != Scenario.Transport.PUSH ) {
                // POLL scenarios are surfaced through poll(); nothing to push.
                continue;
            }
            fireDuePushStages( j );
        }
    }

    @Override
    public void setScenario( @Nullable Long experimentId, Scenario scenario ) {
        if ( scenario == null ) {
            throw new IllegalArgumentException( "scenario is required" );
        }
        if ( experimentId == null ) {
            fallbackScenario = scenario;
        } else {
            scenariosByEe.put( experimentId, scenario );
        }
    }

    @Override
    public Map<Long, Scenario> listScenarios() {
        return new HashMap<>( scenariosByEe );
    }

    @Override
    public synchronized void reset() {
        clock = 0L;
        jobs.clear();
        scenariosByEe.clear();
        submitCounts.clear();
        fallbackScenario = null;
    }

    // -----------------------------------------------------------------------
    // internals
    // -----------------------------------------------------------------------

    private void fireDuePushStages( MockJob j ) {
        if ( j.terminalFired ) {
            return;
        }
        // A cancel that arrived mid-run confirms as a killed event on the next advance.
        if ( j.cancelRequested ) {
            serviceProvider.getObject().recordEvent( j.gemmaJobId, "killed", null );
            j.terminalFired = true;
            return;
        }
        Scenario sc = effectiveScenario( j );
        List<Scenario.Stage> stages = sc.stages;
        long elapsed = clock - j.submittedAtClock;
        for ( int i = j.firedStageIndex + 1; i < stages.size(); i++ ) {
            Scenario.Stage s = stages.get( i );
            if ( s.afterMs > elapsed ) {
                break;
            }
            serviceProvider.getObject().recordEvent( j.gemmaJobId, s.kind, s.payloadJson );
            j.firedStageIndex = i;
            if ( isTerminalKind( s.kind ) ) {
                j.terminalFired = true;
                break;
            }
        }
    }

    /**
     * Honours the reserved {@link Scenario#succeedOnAttempt} hook so task 3's
     * fail→retry→green loop works once retry mints attempt N+1; a no-op for task 1.
     */
    private Scenario effectiveScenario( MockJob j ) {
        Scenario sc = j.scenario;
        if ( sc.succeedOnAttempt != null && j.attempt >= sc.succeedOnAttempt ) {
            return sc.transport == Scenario.Transport.POLL ? defaultSuccessPoll() : DEFAULT_SCENARIO;
        }
        return sc;
    }

    private static JobState kindToState( String kind ) {
        // Inverse of JobReconciler.mapStateToEventKind.
        switch ( kind ) {
            case "completed":
                return JobState.DONE;
            case "error":
                return JobState.FAILED;
            case "killed":
                return JobState.CANCELLED;
            case "stage":
            case "progress":
                return JobState.RUNNING;
            default:
                return JobState.QUEUED;
        }
    }

    private static boolean isTerminalKind( String kind ) {
        return "completed".equals( kind ) || "error".equals( kind ) || "killed".equals( kind );
    }

    private static Scenario defaultSuccess() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.SUCCEED;
        s.transport = Scenario.Transport.PUSH;
        s.stages.add( stage( 0, "stage", "{\"stage\":\"start\"}" ) );
        s.stages.add( stage( 1000, "progress", "{\"pct\":50}" ) );
        s.stages.add( stage( 2000, "completed", "{}" ) );
        return s;
    }

    private static Scenario defaultSuccessPoll() {
        Scenario s = defaultSuccess();
        s.transport = Scenario.Transport.POLL;
        return s;
    }

    private static Scenario.Stage stage( long afterMs, String kind, @Nullable String payloadJson ) {
        Scenario.Stage st = new Scenario.Stage();
        st.afterMs = afterMs;
        st.kind = kind;
        st.payloadJson = payloadJson;
        return st;
    }

    private static final class MockJob {
        final Long gemmaJobId;
        @Nullable
        final Long eeId;
        final Scenario scenario;
        final long submittedAtClock;
        final int attempt;
        int firedStageIndex = -1;
        volatile boolean cancelRequested = false;
        volatile boolean terminalFired = false;

        MockJob( Long gemmaJobId, @Nullable Long eeId, Scenario scenario, long submittedAtClock, int attempt ) {
            this.gemmaJobId = gemmaJobId;
            this.eeId = eeId;
            this.scenario = scenario;
            this.submittedAtClock = submittedAtClock;
            this.attempt = attempt;
        }
    }
}
