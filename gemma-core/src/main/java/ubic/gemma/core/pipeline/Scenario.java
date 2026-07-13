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

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A scripted outcome for one {@link ScriptedMockScheduler} job. Deterministic and
 * failure-capable — the piece the smoke-toy {@code MockPipelineScheduler} lacked.
 *
 * <p>Plain public-field POJO so Jackson round-trips it from the canonical scenario
 * fixtures (see {@code gemma-rest/src/test/resources/pipeline-scenarios/}) and from
 * the dev-only {@code POST /admin/pipeline/_mock/scenario} body with no annotations.</p>
 *
 * <p>One {@link #stages} list drives BOTH transports: for {@link Transport#PUSH} the
 * scheduler fires each due stage through the internal callback path
 * ({@code recordEvent}); for {@link Transport#POLL} the scheduler reports the
 * {@link JobState} of the latest due stage and the reconciler picks it up.</p>
 */
public class Scenario {

    /** Terminal disposition of the job. {@link #STALL} never reaches a terminal stage. */
    public enum Outcome {
        SUCCEED,
        FAIL,
        STALL
    }

    /** Which integration path this scenario exercises. */
    public enum Transport {
        POLL,
        PUSH
    }

    /**
     * Failure classification carried in the {@code error} event payload (schema-free —
     * no {@code FAILURE_CLASS} column until §3.2 / task 3). Mirrors the taxonomy in
     * {@code PIPELINE_COMPUTE_AND_JOB_MANAGEMENT.md} §3.2.
     */
    public enum FailureClass {
        TRANSIENT,
        PERMANENT,
        UNKNOWN
    }

    /** One scripted event, due once the virtual clock has advanced {@link #afterMs} past submit. */
    public static class Stage {
        /** Virtual milliseconds after the job's submit at which this stage becomes due. */
        public long afterMs;
        /** Event kind understood by {@code PipelineJobBatchService.recordEvent}: {@code stage|progress|completed|error|killed}. */
        public String kind;
        /** Opaque JSON payload; for an {@code error} stage carries {@code {"failureClass":...}}. */
        @Nullable
        public String payloadJson;
    }

    public Outcome outcome = Outcome.SUCCEED;

    /** Set when {@link #outcome} is {@link Outcome#FAIL}. */
    @Nullable
    public FailureClass failureClass;

    public Transport transport = Transport.PUSH;

    public List<Stage> stages = new ArrayList<>();

    /**
     * Log lines this scenario would serve via {@code readLog}. Carried for fixture
     * completeness + the shared UIB contract; NOT served until the {@code readLog}
     * SPI lands (task 5).
     */
    public List<String> logLines = new ArrayList<>();

    /**
     * Reserved hook for the fail→retry→green loop (task 3): once the same experiment
     * has been submitted at least this many times (1-based attempt), the job succeeds
     * regardless of the scripted terminal stage. Null = honour {@link #stages} verbatim.
     */
    @Nullable
    public Integer succeedOnAttempt;
}
