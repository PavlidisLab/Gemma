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
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecision;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecisionScope;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecisionType;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;

import java.util.Date;
import java.util.List;

/**
 * Default {@link CurationDecisionService}.
 *
 * <p>Emits no audit event, for the reason {@link AnnotationSetTriageServiceImpl}
 * gives: any audit event sets {@code curationDetails.lastUpdated}, the
 * optimistic-concurrency token the curation commit checks, so recording a
 * refusal would 409 every draft in flight on that dataset. The row carries its
 * own author and timestamp.</p>
 */
@Service
public class CurationDecisionServiceImpl implements CurationDecisionService {

    /** Cap matching {@code REASON VARCHAR(1024)}. */
    private static final int MAX_REASON_LENGTH = 1024;

    /** Cap matching {@code DECISION_KEY VARCHAR(255)}. */
    private static final int MAX_KEY_LENGTH = 255;

    private final CurationDecisionDao curationDecisionDao;

    @Autowired
    public CurationDecisionServiceImpl( CurationDecisionDao curationDecisionDao ) {
        this.curationDecisionDao = curationDecisionDao;
    }

    @Override
    @Transactional
    public CurationDecision decide( Investigation investigation, CurationDecisionType decision,
            CurationDecisionScope scope, @Nullable String decisionKey,
            @Nullable AnnotationSet annotationSet, String reason,
            String decidedBy, TriageJudgeKind judgeKind ) {
        Assert.notNull( investigation, "investigation must not be null." );
        Assert.notNull( investigation.getId(), "investigation must be persistent." );
        Assert.notNull( decision, "decision must not be null." );
        Assert.notNull( scope, "scope must not be null." );
        Assert.hasText( decidedBy, "decidedBy must be non-blank -- a ruling with no author is not a ruling." );
        Assert.notNull( judgeKind, "judgeKind must not be null." );

        String trimmedReason = truncate( reason );
        if ( trimmedReason == null ) {
            // NOT NULL in the schema, and for a reason worth restating: the whole content of a
            // refusal is the "no", so a row without one records that something was rejected and
            // leaves a later reader nothing to judge whether it still applies.
            throw new IllegalArgumentException( "A reason is required for a curation decision --"
                    + " a refusal has no other content." );
        }

        String trimmedKey = decisionKey != null && !decisionKey.trim().isEmpty()
                ? decisionKey.trim() : null;
        Assert.isTrue( trimmedKey == null || trimmedKey.length() <= MAX_KEY_LENGTH,
                "decisionKey must be at most " + MAX_KEY_LENGTH + " characters." );

        if ( scope == CurationDecisionScope.PROPOSAL ) {
            if ( annotationSet == null ) {
                throw new IllegalArgumentException( "A decision scoped to a whole proposal must name"
                        + " the annotation set it answers." );
            }
        } else if ( trimmedKey == null ) {
            // Without a key an ITEM or KEY ruling has no subject, so it could never be matched
            // against a later proposal -- which is the only thing it is for.
            throw new IllegalArgumentException( "A decision scoped to " + scope.getDbValue()
                    + " must carry a decisionKey naming what was ruled on." );
        }

        CurationDecision d = new CurationDecision();
        d.setInvestigation( investigation );
        d.setDecision( decision );
        d.setScope( scope );
        d.setDecisionKey( trimmedKey );
        d.setAnnotationSet( annotationSet );
        d.setReason( trimmedReason );
        d.setDecidedBy( decidedBy );
        d.setJudgeKind( judgeKind );
        d.setDecidedAt( new Date() );
        return curationDecisionDao.create( d );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurationDecision> findByInvestigation( Investigation investigation ) {
        Assert.notNull( investigation, "investigation must not be null." );
        return curationDecisionDao.findByInvestigation( investigation );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurationDecision> standingFor( Investigation investigation ) {
        Assert.notNull( investigation, "investigation must not be null." );
        return curationDecisionDao.findStandingByInvestigation( investigation );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurationDecision> historyForKey( Investigation investigation, String decisionKey ) {
        Assert.notNull( investigation, "investigation must not be null." );
        Assert.hasText( decisionKey, "decisionKey must be non-blank." );
        return curationDecisionDao.findByInvestigationAndKey( investigation, decisionKey.trim() );
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
