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

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationLock;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract for the advisory curation lock.
 * <p>
 * The properties worth pinning are the ones that make it safe to be advisory:
 * that a lapsed claim frees itself without a sweeper, that stealing is always
 * available and records who was displaced, and above all that a refresh cannot
 * quietly become an acquire.
 */
@Transactional
public class CurationLockPersistenceIT extends BaseIntegrationTest5 {

    @Autowired
    private CurationLockService curationLockService;

    @Autowired
    private SessionFactory sessionFactory;

    private Investigation ee;

    /**
     * A PreboardedExperiment rather than a full ExpressionExperiment: it is an
     * Investigation, which is all the lock needs, and it carries no Taxon or
     * ArrayDesign — so the seed does not depend on reference data that a
     * schema rebuilt from the entities does not have.
     */
    @BeforeEach
    public void seed() {
        PreboardedExperiment preboarded = new PreboardedExperiment();
        preboarded.setAccession( "GSE-lock-it-" + UUID.randomUUID() );
        preboarded.setSource( "GEO" );
        preboarded.setName( "CurationLockIT preboarded" );
        preboarded.setWorkflowState( WorkflowState.Preboarded );
        sessionFactory.getCurrentSession().persist( preboarded );
        sessionFactory.getCurrentSession().flush();
        ee = preboarded;
    }

    @Test
    @DisplayName("an unlocked dataset reads as free")
    public void unlocked_readsAsFree() {
        assertThat( curationLockService.current( ee ) ).isEmpty();
        assertThat( curationLockService.isHeldBy( ee, "alice" ) ).isFalse();
    }

    @Test
    @DisplayName("acquire then re-acquire by the same curator refreshes rather than conflicting")
    public void sameCuratorReacquires() {
        CurationLock first = curationLockService.acquire( ee, "alice", false, 30 );
        Date firstExpiry = first.getExpiresAt();
        CurationLock again = curationLockService.acquire( ee, "alice", false, 60 );
        assertThat( again.getLockedBy() ).isEqualTo( "alice" );
        assertThat( again.getExpiresAt() ).isAfter( firstExpiry );
        assertThat( curationLockService.isHeldBy( ee, "alice" ) ).isTrue();
    }

    @Test
    @DisplayName("a second curator is refused unless they ask to steal")
    public void secondCuratorRefusedWithoutSteal() {
        curationLockService.acquire( ee, "alice", false, 30 );
        assertThatThrownBy( () -> curationLockService.acquire( ee, "bob", false, 30 ) )
                .isInstanceOf( CurationLockService.CurationLockedException.class )
                .hasMessageContaining( "alice" );
        assertThat( curationLockService.isHeldBy( ee, "alice" ) ).isTrue();
    }

    @Test
    @DisplayName("stealing always succeeds and records who was displaced")
    public void stealSucceedsAndRecordsTheDisplacedHolder() {
        curationLockService.acquire( ee, "alice", false, 30 );
        CurationLock stolen = curationLockService.acquire( ee, "bob", true, 30 );
        assertThat( stolen.getLockedBy() ).isEqualTo( "bob" );
        assertThat( stolen.getStolenFrom() ).isEqualTo( "alice" );
        assertThat( stolen.getStolenAt() ).isNotNull();
        assertThat( curationLockService.isHeldBy( ee, "alice" ) ).isFalse();
    }

    @Test
    @DisplayName("an expired lock frees itself, with no sweeper")
    public void expiredLockReadsAsFree() {
        CurationLock lock = curationLockService.acquire( ee, "alice", false, 30 );
        lock.setExpiresAt( new Date( System.currentTimeMillis() - 1000L ) );
        sessionFactory.getCurrentSession().flush();

        assertThat( curationLockService.current( ee ) )
                .as( "a lapsed claim is not a holder; nothing sweeps the row" ).isEmpty();
        assertThat( curationLockService.isHeldBy( ee, "alice" ) ).isFalse();
        // and the next curator takes it without having to steal
        CurationLock taken = curationLockService.acquire( ee, "bob", false, 30 );
        assertThat( taken.getLockedBy() ).isEqualTo( "bob" );
    }

    @Test
    @DisplayName("taking over a lapsed claim is not recorded as a steal")
    public void takingOverALapsedClaimIsNotASteal() {
        CurationLock lock = curationLockService.acquire( ee, "alice", false, 30 );
        lock.setExpiresAt( new Date( System.currentTimeMillis() - 1000L ) );
        sessionFactory.getCurrentSession().flush();

        CurationLock taken = curationLockService.acquire( ee, "bob", true, 30 );
        // Nobody was displaced, so recording a steal would put a grievance in the record that never happened.
        assertThat( taken.getStolenFrom() ).isNull();
        assertThat( taken.getStolenAt() ).isNull();
    }

    /**
     * The rule that keeps autosave from being a weapon. Refresh is called on
     * every keystroke burst; if it could create or take over a lock, a curator
     * with a stale tab open would steal it back from whoever now holds it
     * without anyone touching a button.
     */
    @Test
    @DisplayName("refresh never becomes an acquire")
    public void refreshIsNotAnAcquire() {
        assertThat( curationLockService.refresh( ee, "alice", 30 ) )
                .as( "refreshing a lock nobody holds creates nothing" ).isEmpty();
        assertThat( curationLockService.current( ee ) ).isEmpty();

        curationLockService.acquire( ee, "bob", false, 30 );
        assertThat( curationLockService.refresh( ee, "alice", 30 ) )
                .as( "refreshing someone else's lock takes nothing" ).isEmpty();
        assertThat( curationLockService.isHeldBy( ee, "bob" ) ).isTrue();
    }

    @Test
    @DisplayName("refresh extends the holder's own lease")
    public void refreshExtendsTheHoldersLease() {
        CurationLock lock = curationLockService.acquire( ee, "alice", false, 1 );
        Date before = lock.getExpiresAt();
        Optional<CurationLock> refreshed = curationLockService.refresh( ee, "alice", 60 );
        assertThat( refreshed ).isPresent();
        assertThat( refreshed.get().getExpiresAt() ).isAfter( before );
    }

    @Test
    @DisplayName("release is the holder's alone; forceRelease is not")
    public void releaseRequiresTheHolder() {
        curationLockService.acquire( ee, "alice", false, 30 );
        assertThat( curationLockService.release( ee, "bob" ) ).isFalse();
        assertThat( curationLockService.isHeldBy( ee, "alice" ) ).isTrue();

        assertThat( curationLockService.release( ee, "alice" ) ).isTrue();
        assertThat( curationLockService.current( ee ) ).isEmpty();
        // releasing again reports that there was nothing to release
        assertThat( curationLockService.release( ee, "alice" ) ).isFalse();
    }

    @Test
    @DisplayName("forceRelease clears whoever holds it")
    public void forceReleaseClearsAnyHolder() {
        curationLockService.acquire( ee, "alice", false, 30 );
        assertThat( curationLockService.forceRelease( ee ) ).isTrue();
        assertThat( curationLockService.current( ee ) ).isEmpty();
        assertThat( curationLockService.forceRelease( ee ) ).isFalse();
    }
}
