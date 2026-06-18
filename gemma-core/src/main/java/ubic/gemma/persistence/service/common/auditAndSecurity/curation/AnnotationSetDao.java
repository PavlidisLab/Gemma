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

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSummaryValueObject;
import ubic.gemma.persistence.service.BaseDao;

/**
 * DAO for {@link AnnotationSet} rows.
 *
 * <p>Addressed by id or by the {@code (investigation, role, runId)}
 * idempotency triple. The polymorphic semantic of {@code runId} (agent
 * run id / {@code "draft-{curator}"} / generated UUID per role) is
 * enforced at the service layer; the DAO treats it as an opaque key.</p>
 */
public interface AnnotationSetDao extends BaseDao<AnnotationSet> {

    /**
     * Find an existing row for the {@code (investigation, role, runId)}
     * triple, or {@code null}. Used by the service to make {@code POST
     * /datasets/{id}/annotation-sets} idempotent on retry.
     */
    @Nullable
    AnnotationSet findByInvestigationAndRoleAndRunId( Investigation investigation,
            AnnotationSetRole role, String runId );

    /**
     * All sets attached to the given investigation, newest first.
     *
     * @param investigation target. Required.
     * @param roleFilter    optional role filter; {@code null} = all roles.
     */
    List<AnnotationSet> findByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    /**
     * Thin metadata projection of sets attached to the given investigation,
     * newest first. {@code payloadJson} is NOT loaded; the projection
     * emits {@code length(payloadJson)} as {@code payloadSize} so the UI
     * can decide whether to fetch the full row.
     */
    List<AnnotationSetSummaryValueObject> findSummariesByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    /**
     * The most recent set attached to the given investigation matching the
     * role filter (or all roles if {@code null}), or {@code null}.
     * "Most recent" by {@code createdAt}, falling back to id when
     * {@code createdAt} ties.
     */
    @Nullable
    AnnotationSet findLatestByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    /**
     * Count rows attached to the given investigation matching the role
     * filter (or all roles if {@code null}).
     */
    long countByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    /**
     * Rebind every row currently attached to {@code from} so it points at
     * {@code to}. Returns the number of rows rebound. Used by the
     * preboarded-promotion flow so the agent's historical proposals
     * follow the EE after the preboarded row is replaced.
     */
    int rebindInvestigation( Investigation from, Investigation to );

    /**
     * Cross-experiment thin metadata projection: every {@link AnnotationSet}
     * matching the supplied filter, newest first, sliced by
     * {@code offset / limit}.
     *
     * @param roleFilter        optional role filter; {@code null} = all roles.
     * @param sourceFilter      optional source filter; {@code null} = all sources.
     * @param createdByFilter   optional createdBy filter; {@code null} = no filter.
     * @param investigationIds  optional restriction to a set of investigation
     *                          ids; {@code null} or empty = no additional
     *                          filter (ACL enforced upstream).
     */
    List<AnnotationSetSummaryValueObject> listSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable List<Long> investigationIds, int offset, int limit );

    /**
     * Cross-experiment count. Counterpart of
     * {@link #listSummaries(AnnotationSetRole, AnnotationSetSource, String, List, int, int)}.
     */
    long countSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable List<Long> investigationIds );

    /**
     * Count rows with {@code createdAt} on or after {@code since} (or all
     * rows if {@code since} is null). Optionally restricted by role.
     */
    long countSince( @Nullable Date since, @Nullable AnnotationSetRole roleFilter );

    /**
     * Group rows by role, counting per bucket. Rows with
     * {@code createdAt >= since} only when {@code since} is non-null.
     */
    Map<AnnotationSetRole, Long> countByRoleSince( @Nullable Date since );

    /**
     * Number of distinct {@code runId}s seen on rows with
     * {@code createdAt >= since} (or all-time if null). Approximates
     * "how many agent runs have we ingested in the window" when filtered
     * to {@code role=PROPOSAL}.
     */
    long countDistinctRunIdsSince( @Nullable Date since,
            @Nullable AnnotationSetRole roleFilter );

    /**
     * @return the most-recent {@code createdAt} across every row matching
     *         the role filter, or {@code null} if no rows.
     */
    @Nullable
    Date findLatestCreatedAt( @Nullable AnnotationSetRole roleFilter );
}
