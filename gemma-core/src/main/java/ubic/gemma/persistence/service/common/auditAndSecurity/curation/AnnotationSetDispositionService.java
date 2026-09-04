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
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetDisposition;
import ubic.gemma.model.common.auditAndSecurity.curation.FindingDisposition;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageJudgeKind;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A curator's rulings on the individual findings inside an audit
 * {@link AnnotationSet}.
 *
 * <p>🛑 <b>{@code decidedBy} is the curator, not the caller.</b> Curation
 * writes reach Gemma through the curation agent rather than from the curator's
 * browser, so the authenticated principal on these calls is normally the agent
 * acting on someone's behalf. Every identity here is passed in explicitly and
 * never read from the security context — the same rule, for the same reasons,
 * as {@link AnnotationSetTriageService}. The REST layer decides who may claim
 * to be whom.</p>
 *
 * <p>Rulings are append-only; {@link #standingFor(AnnotationSet)} applies the
 * latest-wins fold.</p>
 */
public interface AnnotationSetDispositionService {

    /**
     * Record a ruling on one finding. Always a new row: a curator changing
     * their mind adds to the history rather than overwriting it.
     *
     * @param findingId  which finding, when the producer supplies one. Opaque,
     *                   like {@code targetId}. A target does NOT name a
     *                   finding — one target routinely carries several, and
     *                   two of them can be actionable — so a ruling keyed on
     *                   the target alone cannot say what it ruled on. Null
     *                   keeps the row target-keyed, which is what pre-existing
     *                   rows and producers not emitting ids get.
     * @param targetId   the target, in the producer's own numbering. Opaque —
     *                   the payload is never parsed here, so an id naming no
     *                   finding is accepted and only the producer can tell.
     * @param decidedBy  the ruling identity — a username for a person, a run
     *                   id for an agent. Never the transport's principal.
     * @param judgeKind  stored rather than inferred from {@code decidedBy}, so
     *                   "has a person ruled on this" does not depend on
     *                   knowing every agent run id.
     * @param reason     why; what the agent needs in order to stop emitting a
     *                   finding it got wrong. Optional on
     *                   {@link FindingDisposition#ACCEPTED} and
     *                   {@link FindingDisposition#DISMISSED}, REQUIRED on
     *                   {@link FindingDisposition#NEEDS_MORE_INFO} — see
     *                   below. Free text either way; there is no vocabulary.
     * @throws IllegalArgumentException if {@code disposition} is
     *                   {@link FindingDisposition#NEEDS_MORE_INFO} and no
     *                   non-blank reason came with it. That value's entire
     *                   content IS the reason: without the blocker, "a human
     *                   looked and stopped" cannot be told apart from nobody
     *                   having looked, which is the collapse that having no
     *                   stored {@code PENDING} exists to prevent.
     * @throws IllegalStateException if the set is already finalized. A review
     *                               that has been closed out is not still
     *                               taking rulings, and accepting one would
     *                               leave a disposition dated after the
     *                               finalization that produced the summary.
     *                               Reopen the set first.
     */
    AnnotationSetDisposition rule( AnnotationSet annotationSet, String targetId,
            @Nullable String findingId, FindingDisposition disposition, String decidedBy,
            TriageJudgeKind judgeKind, @Nullable String reason );

    /**
     * Every ruling on one set, most recent first — the full log, superseded
     * rulings included.
     */
    List<AnnotationSetDisposition> findBySet( AnnotationSet annotationSet );

    /**
     * Every ruling on one finding, most recent first. The head is the standing
     * ruling.
     */
    List<AnnotationSetDisposition> historyFor( AnnotationSet annotationSet, String targetId );

    /**
     * The standing ruling for each finding in one set — the most recent per
     * finding id, or per target id for rows carrying no finding id. The two
     * are separate buckets: a ruling on a finding never supersedes a ruling on
     * a target, or the reverse.
     */
    List<AnnotationSetDisposition> standingFor( AnnotationSet annotationSet );

    /**
     * Batched {@link #standingFor}, keyed by annotation set id. One round-trip
     * for a whole page rather than one query per set.
     */
    Map<Long, List<AnnotationSetDisposition>> standingForIds( Collection<Long> annotationSetIds );
}
