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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.BatchRollup;
import ubic.gemma.model.pipeline.FailureClass;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;
import ubic.gemma.persistence.service.pipeline.PipelineJobDao;
import ubic.gemma.persistence.service.pipeline.RetrySpec;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mop-up headline (§3.2 / §7 task 3): submit a batch, a third of the jobs fail transiently, the
 * curator retries, the retries succeed — all deterministic via the scripted mock. Proves the
 * attempt chain (a retry mints a NEW job; the failed one is frozen), the {@link BatchRollup}
 * disposition, and retry idempotency.
 *
 * <p>Fail EEs use a scenario with {@code succeedOnAttempt = 2} — the hook built in task 1 so the
 * mock returns FAIL on attempt 1 and SUCCEED on the retried attempt 2.</p>
 */
@ActiveProfiles("scheduler-mock")
class PipelineJobRetryMockIT extends BaseSpringContextTest5 {

    private static final int N = 6;
    private static final int FAIL_EVERY_NTH = 3;

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private PipelineJobDao pipelineJobDao;

    @Autowired
    private MockSchedulerControl control;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void resetMock() {
        control.reset();
        txTemplate = new TransactionTemplate( transactionManager );
    }

    @Test
    void failThenRetryThenGreen_preservesTheAttemptChain() {
        Contact submitter = getTestPersistentContact();
        List<ExpressionExperiment> ees = new ArrayList<>();
        for ( int i = 0; i < N; i++ ) {
            ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
            ees.add( ee );
            boolean shouldFail = i % FAIL_EVERY_NTH == FAIL_EVERY_NTH - 1;
            control.setScenario( ee.getId(), shouldFail ? failThenSucceed() : success() );
        }
        int expectedFails = N / FAIL_EVERY_NTH;

        PipelineJobBatch batch = pipelineJobBatchService.submit( "test-pipeline", ees, submitter, null, "retry IT" );
        Long batchId = batch.getId();

        // --- first pass: a third fail transiently ---
        control.advance( 2000 );
        BatchRollup r1 = pipelineJobBatchService.computeRollup( batchId );
        assertThat( r1.total ).isEqualTo( N );
        assertThat( r1.done ).isEqualTo( N - expectedFails );
        assertThat( r1.failed ).isEqualTo( expectedFails );
        assertThat( r1.failedRetryable ).isEqualTo( expectedFails );   // classified TRANSIENT
        assertThat( r1.failedPermanent ).isZero();
        // All current attempts have stopped (4 DONE + 2 FAILED) so terminal is true; needsAttention
        // is the orthogonal "there are failures to act on" flag. Both true = "stopped, but mop up".
        assertThat( r1.needsAttention ).isTrue();
        assertThat( r1.terminal ).isTrue();

        // --- mop up: retry every transient failure ---
        BatchRollup afterRetry = pipelineJobBatchService.retryFailed( batchId, new RetrySpec() );
        // the retried attempts are now the current ones and back in flight, so the batch is no
        // longer terminal and the failures have been superseded
        assertThat( afterRetry.running + afterRetry.queued + afterRetry.pending ).isEqualTo( expectedFails );
        assertThat( afterRetry.terminal ).isFalse();
        assertThat( afterRetry.failed ).isZero();

        // --- second pass: the retried attempts succeed ---
        control.advance( 2000 );
        BatchRollup r2 = pipelineJobBatchService.computeRollup( batchId );
        assertThat( r2.total ).isEqualTo( N );
        assertThat( r2.done ).isEqualTo( N );
        assertThat( r2.failed ).isZero();
        assertThat( r2.needsAttention ).isFalse();
        assertThat( r2.terminal ).isTrue();

        // --- the attempt chain is intact: retries are NEW rows; failures are frozen ---
        // Direct DAO reads need an active session, so run them inside a transaction.
        txTemplate.executeWithoutResult( status -> {
            List<PipelineJob> all = pipelineJobDao.findByBatch( batchId );
            assertThat( all ).hasSize( N + expectedFails );          // originals + one retry each

            List<PipelineJob> superseded = all.stream().filter( j -> j.getSupersededBy() != null ).toList();
            assertThat( superseded ).hasSize( expectedFails );
            assertThat( superseded ).allSatisfy( j -> {
                assertThat( j.getState() ).isEqualTo( JobState.FAILED );  // frozen, never flipped back
                assertThat( j.getAttempt() ).isEqualTo( 1 );
                assertThat( j.getFailureClass() ).isEqualTo( FailureClass.TRANSIENT );
            } );

            List<PipelineJob> current = all.stream().filter( j -> j.getSupersededBy() == null ).toList();
            assertThat( current ).hasSize( N );
            assertThat( current ).allSatisfy( j -> assertThat( j.getState() ).isEqualTo( JobState.DONE ) );
            assertThat( current.stream().filter( j -> j.getAttempt() == 2 ).toList() ).hasSize( expectedFails );
            assertThat( current.stream().filter( j -> j.getRetryOf() != null ).toList() ).hasSize( expectedFails );
        } );

        // batch auto-closed now that every current attempt is DONE with no failures
        assertThat( pipelineJobBatchService.get( batchId ).getState() )
                .isEqualTo( PipelineJobBatch.BatchState.CLOSED );

        // --- idempotency: nothing left to retry, no new rows minted ---
        pipelineJobBatchService.retryFailed( batchId, new RetrySpec() );
        txTemplate.executeWithoutResult( status ->
                assertThat( pipelineJobDao.findByBatch( batchId ) ).hasSize( N + expectedFails ) );
    }

    private static Scenario success() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.SUCCEED;
        s.transport = Scenario.Transport.PUSH;
        s.stages.add( stage( 0, "stage", "{\"stage\":\"align\"}" ) );
        s.stages.add( stage( 1000, "completed", "{}" ) );
        return s;
    }

    /** FAIL (transient) on attempt 1; SUCCEED on the retried attempt 2 (task-1 succeedOnAttempt hook). */
    private static Scenario failThenSucceed() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.FAIL;
        s.failureClass = Scenario.FailureClass.TRANSIENT;
        s.transport = Scenario.Transport.PUSH;
        s.succeedOnAttempt = 2;
        s.stages.add( stage( 0, "stage", "{\"stage\":\"align\"}" ) );
        s.stages.add( stage( 1000, "error", "{\"failureClass\":\"TRANSIENT\",\"message\":\"synthetic SRA throttle\"}" ) );
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
