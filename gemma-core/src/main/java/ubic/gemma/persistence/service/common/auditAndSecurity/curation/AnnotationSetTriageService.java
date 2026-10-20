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
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Triage rulings on {@link AnnotationSet}s: how much a set matters, one
 * standing judgement per judge.
 *
 * <p>🛑 <b>{@code judgedBy} is the curator, not the caller.</b> Curation writes
 * reach Gemma through the curation agent rather than from the curator's browser,
 * so the authenticated principal on these calls is normally the agent acting on
 * someone's behalf. Every identity here is therefore passed in explicitly and
 * never read from the security context — resolving it from the principal would
 * stamp every human ruling with the agent's name, make
 * {@link TriageJudgeKind#CURATOR} unreachable, and collapse two curators'
 * distinct judgements onto one row through the
 * {@code UNIQUE(annotationSet, judgedBy)} key.</p>
 *
 * <p>The REST layer decides who may claim to be whom; see the delegation gate
 * on the triage endpoint.</p>
 */
public interface AnnotationSetTriageService {

    /**
     * Record or replace {@code judgedBy}'s ruling on {@code annotationSet}.
     * Idempotent per judge: a second call from the same judge updates the
     * standing row rather than adding one.
     *
     * @param judgedBy  the ruling identity — a username for a person, a run id
     *                  for an agent. Never the transport's principal.
     * @param judgeKind stored rather than inferred from {@code judgedBy}, so
     *                  "has a person ruled on this" does not depend on knowing
     *                  every agent run id.
     * @param note      why; the only place a {@link TriageVerdict#WontFix} says
     *                  what it is declining to fix. Optional.
     */
    AnnotationSetTriage judge( AnnotationSet annotationSet, TriageVerdict verdict,
            String judgedBy, TriageJudgeKind judgeKind, @Nullable String note );

    /**
     * Withdraw {@code judgedBy}'s ruling, returning the set to un-triaged if it
     * was the only one.
     *
     * @return whether a row was removed
     */
    boolean withdraw( AnnotationSet annotationSet, String judgedBy );

    /**
     * Every ruling on one set, most recent first.
     */
    List<AnnotationSetTriage> findBySet( AnnotationSet annotationSet );

    /**
     * The ruling that counts — the most recent — or empty when nobody has
     * ruled.
     */
    Optional<AnnotationSetTriage> effectiveFor( AnnotationSet annotationSet );

    /**
     * Batched {@link #effectiveFor}. Sets with no ruling are absent from the
     * map rather than mapped to null.
     */
    Map<Long, AnnotationSetTriage> effectiveForIds( Collection<Long> annotationSetIds );

    /**
     * The effective ruling for each of several datasets, keyed by dataset id — newest ruling
     * across every annotation set the dataset owns. One round-trip for a whole page.
     */
    Map<Long, AnnotationSetTriage> effectiveForInvestigationIds( Collection<Long> investigationIds );

    /**
     * Every ruling on any set of one investigation, most recent first.
     */
    List<AnnotationSetTriage> findByInvestigation( Investigation investigation );

    /**
     * Whether a person — not only a machine — has ruled on this set.
     */
    boolean reviewedByHuman( AnnotationSet annotationSet );

    /**
     * Corpus-wide tally by verdict.
     */
    Map<TriageVerdict, Long> countByVerdict();
}
