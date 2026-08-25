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
import ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * DAO for {@link AnnotationSetTriage} rows — one standing ruling per
 * (annotation set, judge).
 */
public interface AnnotationSetTriageDao extends BaseDao<AnnotationSetTriage> {

    /**
     * The judgement {@code judgedBy} has standing on this set, or
     * {@code null}. This is the uniqueness key, so there is at most one.
     */
    @Nullable
    AnnotationSetTriage findBySetAndJudge( AnnotationSet annotationSet, String judgedBy );

    /**
     * Every judgement on one set, most recent first. The first element is the
     * effective verdict.
     */
    List<AnnotationSetTriage> findBySet( AnnotationSet annotationSet );

    /**
     * Every judgement on any set attached to one investigation, most recent
     * first — the per-dataset triage view, without loading the sets.
     */
    List<AnnotationSetTriage> findByInvestigation( Investigation investigation );

    /**
     * The effective (most recent) judgement for each of {@code annotationSetIds}.
     * <p>
     * Batched deliberately: the effective verdict is a per-set "latest row"
     * lookup, and doing it one set at a time turns a list query into N+1 round
     * trips. Sets with no judgement are absent from the map rather than mapped
     * to null, so {@code containsKey} answers "has anyone ruled".
     */
    Map<Long, AnnotationSetTriage> findEffectiveBySetIds( Collection<Long> annotationSetIds );

    /**
     * Corpus-wide tally by verdict, for the triage queue's counts.
     */
    Map<TriageVerdict, Long> countByVerdict();
}
