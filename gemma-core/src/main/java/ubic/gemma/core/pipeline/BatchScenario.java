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

/**
 * A batch-shaped scenario: deterministically fail every {@link #failEveryNth}-th job
 * in a submission with {@link #fail}, succeed the rest with {@link #succeed}. This is
 * the {@code PARTIAL_BATCH} contract fixture — distinct from a single-job
 * {@link Scenario} because it describes a rule across N jobs, not one job's timeline.
 *
 * <p>Realized against a real batch by walking the submission's experiments in order
 * and calling {@code setScenario(eeId, ...)} — fail on ordinal {@code i}, {@code i %
 * failEveryNth == failEveryNth - 1}, giving exactly ⌊N/failEveryNth⌋ failures for any
 * (non-contiguous) experiment ids. Plain public-field POJO for the same Jackson reason
 * as {@link Scenario}.</p>
 */
public class BatchScenario {

    /** Fail every N-th job (3 ⇒ ⌊N/3⌋ failures). */
    public int failEveryNth = 3;

    /** Applied to the failing jobs. */
    public Scenario fail = new Scenario();

    /** Applied to the rest. */
    public Scenario succeed = new Scenario();

    /**
     * Which scenario applies to the job at submission ordinal {@code index} (0-based).
     */
    public Scenario scenarioForOrdinal( int index ) {
        return ( index % failEveryNth == failEveryNth - 1 ) ? fail : succeed;
    }
}
