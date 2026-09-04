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
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecision;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecisionScope;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecisionType;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;

import java.util.List;

/**
 * Standing rulings that a change must not be made to an experiment -- and the
 * rarer ruling that one may be.
 *
 * <p>A refusal has nothing to commit, so it has nowhere else to live. This is
 * the record that keeps a curator's "no" from being re-proposed next quarter.
 * Gemma stores and returns it; it does NOT enforce it, because the key is the
 * producer's own and Gemma never parses curation content.</p>
 *
 * <p>As with {@link AnnotationSetTriageService}, every identity is passed in
 * explicitly rather than read from the security context: curation writes reach
 * Gemma through an agent acting on a curator's behalf, so the principal is
 * usually not the person who decided.</p>
 */
public interface CurationDecisionService {

    /**
     * Record a decision. Always a new row: a curator who reverses a refusal
     * adds to the history rather than erasing it, so why it was refused in the
     * first place survives the reversal.
     *
     * @param scope         how wide the ruling reaches;
     *                      {@link CurationDecisionScope#KEY} gates siblings
     *                      that have not been proposed yet, which is the whole
     *                      point of a refusal
     * @param decisionKey   WHAT was ruled on, in the producer's own terms.
     *                      Required except for
     *                      {@link CurationDecisionScope#PROPOSAL}. Opaque here.
     * @param annotationSet the proposal answered, if any. Required for
     *                      {@link CurationDecisionScope#PROPOSAL}, which has no
     *                      key of its own.
     * @param reason        why -- REQUIRED. A refusal has no other content, and
     *                      a later reader needs it to judge whether the
     *                      refusal still applies.
     * @param decidedBy     the deciding identity, never the transport's
     *                      principal
     * @throws IllegalArgumentException if the reason is blank, or the scope and
     *                                  the key/proposal pair disagree
     */
    CurationDecision decide( Investigation investigation, CurationDecisionType decision,
            CurationDecisionScope scope, @Nullable String decisionKey,
            @Nullable AnnotationSet annotationSet, String reason,
            String decidedBy, TriageJudgeKind judgeKind );

    /**
     * Every decision on one experiment, most recent first -- the full log.
     */
    List<CurationDecision> findByInvestigation( Investigation investigation );

    /**
     * The standing decision under each key. A key whose latest row is
     * {@link CurationDecisionType#ALLOWED} appears here as allowed rather than
     * being absent: "refused, then lifted" and "never ruled on" are different
     * states and the caller decides what to do about each.
     */
    List<CurationDecision> standingFor( Investigation investigation );

    /**
     * Every decision on one key, most recent first.
     */
    List<CurationDecision> historyForKey( Investigation investigation, String decisionKey );
}
