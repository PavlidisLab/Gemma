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
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link AnnotationSetTriageService}.
 *
 * <p>Deliberately emits no audit event. Any audit event sets
 * {@code curationDetails.lastUpdated}, and that field is the
 * optimistic-concurrency token the curation commit checks — so triaging a
 * dataset would 409 every draft in flight on it. Same reasoning as
 * {@code bebe778980} for snapshots. The triage row carries its own
 * {@code judgedBy} / {@code judgedAt}, so it is its own record.</p>
 */
@Service
public class AnnotationSetTriageServiceImpl implements AnnotationSetTriageService {

    /**
     * Cap matching {@code NOTE VARCHAR(1024)}. Truncated rather than rejected:
     * losing the tail of an explanation is a smaller harm than refusing a
     * ruling a curator has already made, and the verdict is the part that
     * matters.
     */
    private static final int MAX_NOTE_LENGTH = 1024;

    private final AnnotationSetTriageDao annotationSetTriageDao;

    @Autowired
    public AnnotationSetTriageServiceImpl( AnnotationSetTriageDao annotationSetTriageDao ) {
        this.annotationSetTriageDao = annotationSetTriageDao;
    }

    @Override
    @Transactional
    public AnnotationSetTriage judge( AnnotationSet annotationSet, TriageVerdict verdict,
            String judgedBy, TriageJudgeKind judgeKind, @Nullable String note ) {
        Assert.notNull( annotationSet, "annotationSet must not be null." );
        Assert.notNull( annotationSet.getId(), "annotationSet must be persistent." );
        Assert.notNull( verdict, "verdict must not be null; an un-triaged set has no row." );
        Assert.hasText( judgedBy, "judgedBy must be non-blank -- a ruling with no judge is not a ruling." );
        Assert.notNull( judgeKind, "judgeKind must not be null." );

        AnnotationSetTriage existing = annotationSetTriageDao.findBySetAndJudge( annotationSet, judgedBy );
        Date now = new Date();
        if ( existing != null ) {
            existing.setVerdict( verdict );
            existing.setJudgeKind( judgeKind );
            existing.setJudgedAt( now );
            existing.setNote( truncate( note ) );
            annotationSetTriageDao.update( existing );
            return existing;
        }
        AnnotationSetTriage t = new AnnotationSetTriage();
        t.setAnnotationSet( annotationSet );
        t.setVerdict( verdict );
        t.setJudgedBy( judgedBy );
        t.setJudgeKind( judgeKind );
        t.setJudgedAt( now );
        t.setNote( truncate( note ) );
        return annotationSetTriageDao.create( t );
    }

    @Override
    @Transactional
    public boolean withdraw( AnnotationSet annotationSet, String judgedBy ) {
        Assert.notNull( annotationSet, "annotationSet must not be null." );
        Assert.hasText( judgedBy, "judgedBy must be non-blank." );
        AnnotationSetTriage existing = annotationSetTriageDao.findBySetAndJudge( annotationSet, judgedBy );
        if ( existing == null ) {
            return false;
        }
        annotationSetTriageDao.remove( existing );
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationSetTriage> findBySet( AnnotationSet annotationSet ) {
        Assert.notNull( annotationSet, "annotationSet must not be null." );
        return annotationSetTriageDao.findBySet( annotationSet );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnnotationSetTriage> effectiveFor( AnnotationSet annotationSet ) {
        // findBySet is already newest-first, so the effective ruling is its head.
        return findBySet( annotationSet ).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AnnotationSetTriage> effectiveForIds( Collection<Long> annotationSetIds ) {
        return annotationSetTriageDao.findEffectiveBySetIds( annotationSetIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AnnotationSetTriage> effectiveForInvestigationIds( Collection<Long> investigationIds ) {
        return annotationSetTriageDao.findEffectiveByInvestigationIds( investigationIds );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationSetTriage> findByInvestigation( Investigation investigation ) {
        Assert.notNull( investigation, "investigation must not be null." );
        return annotationSetTriageDao.findByInvestigation( investigation );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean reviewedByHuman( AnnotationSet annotationSet ) {
        return AnnotationSetTriage.reviewedByHuman( findBySet( annotationSet ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<TriageVerdict, Long> countByVerdict() {
        return annotationSetTriageDao.countByVerdict();
    }

    @Nullable
    private static String truncate( @Nullable String note ) {
        if ( note == null ) {
            return null;
        }
        String trimmed = note.trim();
        if ( trimmed.isEmpty() ) {
            return null;
        }
        return trimmed.length() <= MAX_NOTE_LENGTH ? trimmed : trimmed.substring( 0, MAX_NOTE_LENGTH );
    }
}
