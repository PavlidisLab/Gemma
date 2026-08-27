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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationLock;
import ubic.gemma.model.analysis.Investigation;

import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Default {@link CurationLockService}.
 *
 * <p>Written straight through the session rather than via a curatable update
 * path, and emitting no audit event — see the interface for why both matter.</p>
 */
@Service
public class CurationLockServiceImpl implements CurationLockService {

    private final SessionFactory sessionFactory;

    @Autowired
    public CurationLockServiceImpl( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional
    public CurationLock acquire( Investigation ee, String lockedBy, boolean steal, int ttlMinutes ) {
        return acquire( ee, lockedBy, steal, ttlMinutes, null, null );
    }

    @Override
    @Transactional
    public CurationLock acquire( Investigation ee, String lockedBy, boolean steal, int ttlMinutes,
            @Nullable String runId, @Nullable String agentName ) {
        CurationLock lock = doAcquire( ee, lockedBy, steal, ttlMinutes );
        // Set on every acquire, including a refresh, so the row always describes the CURRENT tenure. Carrying
        // a previous holder's run id forward would be worse than carrying nothing -- it would name a job that
        // is no longer here.
        lock.setRunId( runId );
        lock.setAgentName( agentName );
        return lock;
    }

    private CurationLock doAcquire( Investigation ee, String lockedBy, boolean steal, int ttlMinutes ) {
        Assert.notNull( ee, "dataset must not be null." );
        Assert.hasText( lockedBy, "lockedBy must be non-blank." );
        Date now = new Date();
        CurationLock existing = load( ee );

        if ( existing == null ) {
            CurationLock lock = new CurationLock();
            lock.setInvestigation( ee );
            stamp( lock, lockedBy, now, ttlMinutes );
            sessionFactory.getCurrentSession().persist( lock );
            return lock;
        }

        boolean free = existing.isExpired( now ) || lockedBy.equals( existing.getLockedBy() );
        if ( !free && !steal ) {
            throw new CurationLockedException( existing.getLockedBy(),
                    "Dataset " + ee.getId() + " is being curated by " + existing.getLockedBy()
                            + " (last refreshed " + existing.getLockedAt() + ")." );
        }
        // A steal of an already-expired lock is not a steal: nobody was displaced, so recording one would put a
        // grievance in the record that never happened.
        boolean displacedSomeone = !free && steal;
        if ( displacedSomeone ) {
            existing.setStolenFrom( existing.getLockedBy() );
            existing.setStolenAt( now );
        } else if ( !lockedBy.equals( existing.getLockedBy() ) ) {
            // Taking over a lapsed claim: clear any earlier steal so the row describes this tenure, not a past one.
            existing.setStolenFrom( null );
            existing.setStolenAt( null );
        }
        stamp( existing, lockedBy, now, ttlMinutes );
        return existing;
    }

    @Override
    @Transactional
    public Optional<CurationLock> refresh( Investigation ee, String heldBy, int ttlMinutes ) {
        Assert.notNull( ee, "dataset must not be null." );
        Assert.hasText( heldBy, "heldBy must be non-blank." );
        Date now = new Date();
        CurationLock existing = load( ee );
        // Deliberately not an upsert. An autosave calls this on every keystroke burst; if a refresh could create
        // or take over a lock, a curator with a stale tab open would silently steal it back from whoever now
        // holds it, without anyone touching a button.
        if ( existing == null || !existing.isHeldBy( heldBy, now ) ) {
            return Optional.empty();
        }
        existing.setExpiresAt( expiry( now, ttlMinutes ) );
        return Optional.of( existing );
    }

    @Override
    @Transactional
    public boolean release( Investigation ee, String heldBy ) {
        Assert.notNull( ee, "dataset must not be null." );
        CurationLock existing = load( ee );
        if ( existing == null || !heldBy.equals( existing.getLockedBy() ) ) {
            return false;
        }
        sessionFactory.getCurrentSession().remove( existing );
        return true;
    }

    @Override
    @Transactional
    public boolean forceRelease( Investigation ee ) {
        Assert.notNull( ee, "dataset must not be null." );
        CurationLock existing = load( ee );
        if ( existing == null ) {
            return false;
        }
        sessionFactory.getCurrentSession().remove( existing );
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurationLock> current( Investigation ee ) {
        Assert.notNull( ee, "dataset must not be null." );
        CurationLock existing = load( ee );
        // Expiry is never swept, so a lapsed row is still in the table. "Is it locked" has to mean "is there an
        // unexpired claim", or a dataset stays locked forever by whoever last closed a tab.
        if ( existing == null || existing.isExpired( new Date() ) ) {
            return Optional.empty();
        }
        return Optional.of( existing );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isHeldBy( Investigation ee, @Nullable String username ) {
        if ( username == null ) {
            return false;
        }
        return current( ee ).map( l -> username.equals( l.getLockedBy() ) ).orElse( false );
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public Map<Long, CurationLock> current( Collection<Long> investigationIds ) {
        if ( investigationIds == null || investigationIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // One `in` query rather than a get() per id: the caller is painting a queue page, not looking at one
        // dataset. The id IS the identifier (CurationLock's @Id is the Investigation association), so reading
        // getInvestigation().getId() off the result never initializes the lazy proxy.
        List<CurationLock> locks = sessionFactory.getCurrentSession()
                .createQuery( "select l from CurationLock l where l.investigation.id in :ids", CurationLock.class )
                .setParameterList( "ids", investigationIds )
                .list();
        Date now = new Date();
        Map<Long, CurationLock> out = new HashMap<>( locks.size() );
        for ( CurationLock l : locks ) {
            // Expiry is never swept, so the table still holds lapsed rows. Dropping them here keeps this
            // method's contract identical to the single-dataset current(): present means held right now.
            if ( !l.isExpired( now ) ) {
                out.put( l.getInvestigation().getId(), l );
            }
        }
        return out;
    }

    private CurationLock load( Investigation ee ) {
        return sessionFactory.getCurrentSession().get( CurationLock.class, ee.getId() );
    }

    private static void stamp( CurationLock lock, String lockedBy, Date now, int ttlMinutes ) {
        lock.setLockedBy( lockedBy );
        lock.setLockedAt( now );
        lock.setExpiresAt( expiry( now, ttlMinutes ) );
    }

    private static Date expiry( Date now, int ttlMinutes ) {
        int minutes = ttlMinutes > 0 ? ttlMinutes : DEFAULT_TTL_MINUTES;
        return new Date( now.getTime() + TimeUnit.MINUTES.toMillis( minutes ) );
    }
}
