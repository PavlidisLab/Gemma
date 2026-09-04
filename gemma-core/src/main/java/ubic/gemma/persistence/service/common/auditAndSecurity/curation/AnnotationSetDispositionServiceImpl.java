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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetDisposition;
import ubic.gemma.model.common.auditAndSecurity.curation.FindingDisposition;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Default {@link AnnotationSetDispositionService}.
 *
 * <p>Deliberately emits no audit event, for the reason
 * {@link AnnotationSetTriageServiceImpl} gives: any audit event sets
 * {@code curationDetails.lastUpdated}, which is the optimistic-concurrency
 * token the curation commit checks — so ruling on a finding would 409 every
 * draft in flight on that dataset. The row carries its own {@code decidedBy} /
 * {@code decidedAt}, so it is its own record.</p>
 */
@Service
public class AnnotationSetDispositionServiceImpl implements AnnotationSetDispositionService {

    /**
     * Cap matching {@code REASON VARCHAR(1024)}. Truncated rather than
     * rejected: losing the tail of an explanation is a smaller harm than
     * refusing a ruling the curator has already made, and the disposition is
     * the part that matters. Same rule as
     * {@link AnnotationSetTriageServiceImpl}'s note.
     */
    private static final int MAX_REASON_LENGTH = 1024;

    /** Cap matching {@code TARGET_ID VARCHAR(255)}. */
    private static final int MAX_TARGET_ID_LENGTH = 255;

    private final AnnotationSetDispositionDao annotationSetDispositionDao;

    @Autowired
    public AnnotationSetDispositionServiceImpl( AnnotationSetDispositionDao annotationSetDispositionDao ) {
        this.annotationSetDispositionDao = annotationSetDispositionDao;
    }

    @Override
    @Transactional
    public AnnotationSetDisposition rule( AnnotationSet annotationSet, String targetId,
            FindingDisposition disposition, String decidedBy, TriageJudgeKind judgeKind,
            @Nullable String reason ) {
        Assert.notNull( annotationSet, "annotationSet must not be null." );
        Assert.notNull( annotationSet.getId(), "annotationSet must be persistent." );
        Assert.hasText( targetId, "targetId must be non-blank -- a ruling on no finding is not a ruling." );
        Assert.notNull( disposition, "disposition must not be null; an un-ruled finding has no row." );
        Assert.hasText( decidedBy, "decidedBy must be non-blank -- a ruling with no author is not a ruling." );
        Assert.notNull( judgeKind, "judgeKind must not be null." );
        // Truncating this one silently would change WHICH finding was ruled on, so unlike the
        // reason it is refused outright.
        Assert.isTrue( targetId.trim().length() <= MAX_TARGET_ID_LENGTH,
                "targetId must be at most " + MAX_TARGET_ID_LENGTH + " characters." );
        if ( annotationSet.getFinalizedAt() != null ) {
            throw new IllegalStateException( "Annotation set " + annotationSet.getId()
                    + " was finalized at " + annotationSet.getFinalizedAt()
                    + " and is not taking rulings; reopen it first." );
        }
        String reasonToStore = truncate( reason );
        // NEEDS_MORE_INFO is the one value whose entire content is the reason. ACCEPTED and
        // DISMISSED are self-describing -- the finding says what was accepted or dismissed --
        // but "a human looked and stopped" without the blocker is indistinguishable from nobody
        // having looked, which is the collapse having no stored PENDING exists to prevent. The
        // requirement keeps that distinction from eroding from the other side, and the producing
        // side enforces the same rule (curation-agents `agents/audit/schemas.py`), so a row
        // accepted here without one is a row it would refuse to construct.
        if ( disposition == FindingDisposition.NEEDS_MORE_INFO && reasonToStore == null ) {
            throw new IllegalArgumentException( "A reason is required for disposition"
                    + " 'needs_more_info' -- it is what says which follow-up is missing."
                    + " Without it the ruling cannot be told apart from nobody having looked." );
        }

        AnnotationSetDisposition d = new AnnotationSetDisposition();
        d.setAnnotationSet( annotationSet );
        d.setTargetId( targetId.trim() );
        d.setDisposition( disposition );
        d.setDecidedBy( decidedBy );
        d.setJudgeKind( judgeKind );
        d.setDecidedAt( new Date() );
        d.setReason( reasonToStore );
        return annotationSetDispositionDao.create( d );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationSetDisposition> findBySet( AnnotationSet annotationSet ) {
        Assert.notNull( annotationSet, "annotationSet must not be null." );
        return annotationSetDispositionDao.findBySet( annotationSet );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationSetDisposition> historyFor( AnnotationSet annotationSet, String targetId ) {
        Assert.notNull( annotationSet, "annotationSet must not be null." );
        Assert.hasText( targetId, "targetId must be non-blank." );
        return annotationSetDispositionDao.findBySetAndTarget( annotationSet, targetId.trim() );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, AnnotationSetDisposition> standingFor( AnnotationSet annotationSet ) {
        Assert.notNull( annotationSet, "annotationSet must not be null." );
        return annotationSetDispositionDao.findLatestBySet( annotationSet );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Map<String, AnnotationSetDisposition>> standingForIds(
            Collection<Long> annotationSetIds ) {
        return annotationSetDispositionDao.findLatestBySetIds( annotationSetIds );
    }

    @Nullable
    private static String truncate( @Nullable String reason ) {
        if ( reason == null ) {
            return null;
        }
        String trimmed = reason.trim();
        if ( trimmed.isEmpty() ) {
            return null;
        }
        return trimmed.length() <= MAX_REASON_LENGTH ? trimmed : trimmed.substring( 0, MAX_REASON_LENGTH );
    }
}
