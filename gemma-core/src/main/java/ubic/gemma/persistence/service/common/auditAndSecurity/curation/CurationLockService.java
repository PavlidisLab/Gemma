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
 */
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationLock;
import ubic.gemma.model.analysis.Investigation;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Advisory, steal-able claims on a dataset's curation.
 *
 * <p>🛑 <b>Advisory.</b> The correctness guarantee for concurrent curation
 * writes is the optimistic-concurrency token — the curation commit checks
 * {@code baseline.lastModified} and returns 409 when the dataset moved. This
 * lock exists so that 409 rarely fires and so a curator can see who else is
 * working. Nothing here may become the thing that makes concurrent writes
 * safe.</p>
 *
 * <p>🛑 <b>No audit events.</b> Any audit event sets
 * {@code curationDetails.lastUpdated}, which is that same token, so taking a
 * lock would 409 every draft in flight on the dataset. The row carries
 * {@code stolenFrom} / {@code stolenAt} and is its own record.</p>
 *
 * <p>As with the rest of the curation surface, the holder identity is passed
 * in rather than read from the security context: curation reaches Gemma
 * through the agent, so the principal is normally not the person holding the
 * lock.</p>
 */
public interface CurationLockService {

    /** Default lease length. Refreshed by curator activity. */
    int DEFAULT_TTL_MINUTES = 30;

    /**
     * Take or refresh the lock.
     *
     * @param steal permit taking a lock another curator currently holds.
     *              Always available to callers by design — there is no unlock
     *              ceremony to forget, and a steal destroys nothing, since the
     *              displaced curator's DRAFT is a separate row.
     * @return the granted lock
     * @throws CurationLockedException if the lock is held by someone else and
     *                                 {@code steal} is false
     */
    CurationLock acquire( Investigation ee, String lockedBy, boolean steal, int ttlMinutes );

    /**
     * As {@link #acquire(Investigation, String, boolean, int)}, recording WHAT is taking the lock as well as
     * who it is for.
     * <p>
     * {@code runId} / {@code agentName} are null for a person, which is how a reader tells a curator from a
     * job. See {@link CurationLock#getRunId()} for why this is stored rather than derived.
     */
    CurationLock acquire( Investigation ee, String lockedBy, boolean steal, int ttlMinutes,
            @Nullable String runId, @Nullable String agentName );

    /**
     * Extend the caller's existing lease without taking one they do not hold.
     * Called on every draft autosave, so working holds the lock and walking
     * away releases it.
     *
     * @return the refreshed lock, or empty if {@code heldBy} does not hold it
     *         — a refresh must never quietly become an acquire, or an autosave
     *         would steal a lock the curator never asked for
     */
    Optional<CurationLock> refresh( Investigation ee, String heldBy, int ttlMinutes );

    /**
     * Release the lock if {@code heldBy} holds it.
     *
     * @return whether a lock was released
     */
    boolean release( Investigation ee, String heldBy );

    /**
     * Release whoever holds it. For administrators.
     */
    boolean forceRelease( Investigation ee );

    /**
     * The current holder, or empty when the dataset is free.
     * <p>
     * An expired row reads as empty rather than as a holder: nothing sweeps
     * expiry, so "is it locked" has to mean "is there an unexpired claim".
     */
    Optional<CurationLock> current( Investigation ee );

    /**
     * The current holders of many datasets, keyed by dataset id, in ONE query.
     * <p>
     * Exists because the question is asked about a LIST: the curation queue pages up to 1000 rows, and
     * asking per row is 1000 round-trips to paint one screen. A dataset nobody holds is simply absent from
     * the map rather than mapped to null.
     * <p>
     * Same expiry rule as {@link #current(Investigation)} — a lapsed claim is not a holder — so a caller can
     * treat presence in this map as "held right now" without re-checking dates.
     */
    Map<Long, CurationLock> current( Collection<Long> investigationIds );

    /**
     * Whether {@code username} holds an unexpired lock. This is what sign-off
     * checks.
     */
    boolean isHeldBy( Investigation ee, @Nullable String username );

    /**
     * Thrown when a lock is held by someone else and the caller did not ask to
     * steal it. Carries the holder so the caller can name them.
     */
    class CurationLockedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final String heldBy;

        public CurationLockedException( String heldBy, String message ) {
            super( message );
            this.heldBy = heldBy;
        }

        public String getHeldBy() {
            return heldBy;
        }
    }
}
