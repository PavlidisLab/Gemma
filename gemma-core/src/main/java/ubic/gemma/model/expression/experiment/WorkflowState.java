/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package ubic.gemma.model.expression.experiment;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Eight-state workflow lifecycle for experiments (and forthcoming
 * {@code SkeletonInvestigation}s).
 *
 * <p>Transition table is lifted verbatim from {@code HANDOFF_WORKFLOW_STATE_STORAGE.md}
 * §"State-machine reference" (which itself mirrors the UI canonical spec in
 * {@code gemma-curation-ui/apps/curation/WORKFLOW_MANAGEMENT.md}):</p>
 *
 * <pre>
 * Discovery   -&gt; Candidate, Skeleton
 * Candidate   -&gt; Skeleton, Discovery
 * Skeleton    -&gt; Loaded, Candidate
 * Loaded      -&gt; Curate
 * Curate      -&gt; Process, Audit
 * Process     -&gt; Audit, Curate
 * Audit       -&gt; Curate, Public
 * Public      -&gt; Curate
 * </pre>
 *
 * <p>An idempotent re-assert ({@code target == current}) is permitted but
 * recorded by the service layer as a no-op (no audit event emitted); the
 * machine itself only enumerates strict transitions.</p>
 *
 * <p>String form of the enum is stored verbatim in
 * {@code INVESTIGATION.WORKFLOW_STATE} (VARCHAR(32)). Renaming a constant
 * is therefore a schema migration.</p>
 */
public enum WorkflowState {
    Discovery,
    Candidate,
    Skeleton,
    Loaded,
    Curate,
    Process,
    Audit,
    Public;

    private static final Map<WorkflowState, Set<WorkflowState>> TRANSITIONS;

    static {
        EnumMap<WorkflowState, Set<WorkflowState>> t = new EnumMap<>( WorkflowState.class );
        t.put( Discovery, EnumSet.of( Candidate, Skeleton ) );
        t.put( Candidate, EnumSet.of( Skeleton, Discovery ) );
        t.put( Skeleton, EnumSet.of( Loaded, Candidate ) );
        t.put( Loaded, EnumSet.of( Curate ) );
        t.put( Curate, EnumSet.of( Process, Audit ) );
        t.put( Process, EnumSet.of( Audit, Curate ) );
        t.put( Audit, EnumSet.of( Curate, Public ) );
        t.put( Public, EnumSet.of( Curate ) );
        TRANSITIONS = Collections.unmodifiableMap( t );
    }

    /**
     * @return the set of states this state can advance to (strict, excludes
     *         self). Never null.
     */
    public Set<WorkflowState> allowedNextStates() {
        return TRANSITIONS.get( this );
    }

    /**
     * @return {@code true} if {@code target} is a strict allowed successor of
     *         {@code this}. A self-transition returns {@code false}; the
     *         service layer treats idempotent re-assert as a no-op rather
     *         than as a transition.
     */
    public boolean canTransitionTo( WorkflowState target ) {
        if ( target == null ) {
            return false;
        }
        return TRANSITIONS.get( this ).contains( target );
    }
}
