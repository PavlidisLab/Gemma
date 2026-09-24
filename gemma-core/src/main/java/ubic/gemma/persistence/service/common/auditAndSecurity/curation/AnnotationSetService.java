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
import ubic.gemma.model.expression.experiment.AgentCurationKind;

/**
 * Service surface for {@link AnnotationSet} rows. Drives the
 * {@code /datasets/{id}/annotation-sets} + {@code /annotation-sets/{id}}
 * REST endpoints and the {@code /preboarded/{id}/annotation-sets}
 * pre-load surface.
 *
 * <p>{@link #attach} is idempotent on
 * {@code (investigation, role, runId)}: a retry with the same triple
 * returns the existing row instead of creating a duplicate. The REST
 * layer reports the existing row as 200 OK rather than 201 Created in
 * that case.</p>
 */
public interface AnnotationSetService {

    /**
     * Run-id prefix on a {@code SNAPSHOT} taken by a curation commit to keep the state it displaced, as opposed to
     * one a caller asked for through {@code POST /datasets/{id}/annotation-sets/snapshot}. Both are ordinary
     * snapshots and restore the same way; the prefix is what tells them apart in a list.
     * <p>
     * The rest of the id is a UUID, as for any snapshot &mdash; a commit-taken backup names no run of its own.
     */
    String PRE_COMMIT_SNAPSHOT_RUN_ID_PREFIX = "precommit-";

    /**
     * Attach (or return existing) an annotation set to the given
     * investigation. The {@code runId} semantic depends on role and is
     * the caller's responsibility:
     * <ul>
     *   <li>{@code PROPOSAL} &mdash; pass the agent runner's unique id.</li>
     *   <li>{@code DRAFT} &mdash; pass {@code "draft-{createdBy}"} (or the
     *       service derives it if blank; see
     *       {@link #upsertDraft(Investigation, String, String, String, AnnotationSet)}).</li>
     *   <li>{@code SNAPSHOT} &mdash; pass a generated UUID, or let the
     *       service generate one when {@code runId} is null/blank.</li>
     * </ul>
     *
     * @return the persisted row plus a flag noting whether it was created
     *         (true) or returned as existing (false).
     */
    AttachedAnnotationSet attach( Investigation investigation,
            AnnotationSetRole role,
            AnnotationSetSource source,
            @Nullable AgentCurationKind kind,
            @Nullable String runId,
            @Nullable String createdBy,
            @Nullable String agentVersion,
            @Nullable String model,
            @Nullable Date ranAt,
            @Nullable String payloadJson,
            @Nullable AnnotationSet parent );

    /**
     * As {@link #attach}, but carrying the full run provenance rather than the {@code agentVersion} /
     * {@code model} / {@code ranAt} triple.
     * <p>
     * An overload rather than two more parameters on the method above: that signature already takes eleven
     * arguments, six of which would then be adjacent nullable strings meaning different things
     * ({@code runId}, {@code createdBy}, {@code agentVersion}, {@code model}, {@code runSha},
     * {@code agentName}). Transposing two of those compiles silently and mis-files provenance in a way nothing
     * would catch. {@link RunProvenance} makes that impossible and leaves the existing callers untouched.
     */
    AttachedAnnotationSet attach( Investigation investigation,
            AnnotationSetRole role,
            AnnotationSetSource source,
            @Nullable AgentCurationKind kind,
            @Nullable String runId,
            @Nullable String createdBy,
            @Nullable RunProvenance runProvenance,
            @Nullable String payloadJson,
            @Nullable AnnotationSet parent );

    /**
     * Who produced an annotation set, and from which build.
     * <p>
     * Answers the "which agent · when" half of curation provenance. Every field is optional — provenance is
     * expected to be sparse, and an absent value means "not recorded", never "none".
     */
    class RunProvenance {
        @Nullable
        private final String agentVersion;
        @Nullable
        private final String model;
        /**
         * The producing repository's git head sha. Not redundant with {@link #model}: behaviour differs between
         * shas at one model, so the model alone does not identify the build.
         */
        @Nullable
        private final String runSha;
        /** Which specialist produced it ({@code cell_type}, {@code disease}, …) — "the agent" is a fleet. */
        @Nullable
        private final String agentName;
        @Nullable
        private final Date ranAt;

        public RunProvenance( @Nullable String agentVersion, @Nullable String model, @Nullable String runSha,
                @Nullable String agentName, @Nullable Date ranAt ) {
            this.agentVersion = agentVersion;
            this.model = model;
            this.runSha = runSha;
            this.agentName = agentName;
            this.ranAt = ranAt;
        }

        @Nullable
        public String getAgentVersion() {
            return agentVersion;
        }

        @Nullable
        public String getModel() {
            return model;
        }

        @Nullable
        public String getRunSha() {
            return runSha;
        }

        @Nullable
        public String getAgentName() {
            return agentName;
        }

        @Nullable
        public Date getRanAt() {
            return ranAt;
        }
    }

    /**
     * Convenience overload for the common DRAFT-upsert path used by the
     * curation-UI: ensures one DRAFT per {@code (investigation, curator)}
     * by deriving {@code runId} as {@code "draft-{createdBy}"}. If a row
     * already exists, its {@code payloadJson} / {@code parkedElements} /
     * {@code updatedAt} are updated in place; otherwise a new row is
     * created.
     *
     * @param parent  optional {@code PROPOSAL} this draft was seeded from
     *                (forms the lineage edge for diff-derived
     *                dispositions).
     */
    AnnotationSet upsertDraft( Investigation investigation,
            String createdBy,
            String payloadJson,
            @Nullable String parkedElements,
            @Nullable AnnotationSet parent );

    /**
     * @return all sets attached to the given investigation matching the
     *         role filter (or all roles if null), newest first.
     */
    List<AnnotationSet> findByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    /**
     * Thin metadata projection.
     */
    List<AnnotationSetSummaryValueObject> findSummariesByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    @Nullable
    AnnotationSet findLatestByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    @Nullable
    AnnotationSet load( Long id );

    @Nullable
    AnnotationSet findByInvestigationAndRoleAndRunId( Investigation investigation,
            AnnotationSetRole role, String runId );

    long countByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter );

    int rebindInvestigation( Investigation from, Investigation to );

    List<AnnotationSetSummaryValueObject> listSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable AgentCurationKind kindFilter,
            @Nullable String statusFilter,
            @Nullable List<Long> investigationIds, int offset, int limit,
            @Nullable AnnotationSetDao.SummarySort sort, boolean descending );

    long countSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable AgentCurationKind kindFilter,
            @Nullable String statusFilter,
            @Nullable List<Long> investigationIds );

    /**
     * Stamp {@code finalizedAt} + {@code finalizedBy} on the row,
     * marking a DRAFT as "done editing" or a SNAPSHOT as the polished
     * canonical view. Idempotent: a row already finalized returns
     * unchanged (no re-stamp).
     *
     * @param notes the curator's closing note, or {@code null}. Trimmed, and
     *              truncated to fit rather than rejected — losing the tail of
     *              an explanation is a smaller harm than refusing a closure
     *              the curator has already decided on. A blank note is stored
     *              as {@code null}.
     *              <p>
     *              🛑 The one thing this is NOT idempotent about. A set that
     *              is already finalized is still re-stamped with a non-blank
     *              {@code notes}, because dropping the sentence and answering
     *              200 is the exact failure the parameter exists to fix; a
     *              caller cannot tell that from a successful write. Everything
     *              else about the row stays as it was.
     */
    @Nullable
    AnnotationSet finalizeSet( Long id, @Nullable String finalizedBy, @Nullable String notes );

    /**
     * Clear {@code finalizedAt} + {@code finalizedBy} + {@code finalizedNotes}
     * on the row, reopening a finalized DRAFT or unblessing a polished
     * SNAPSHOT. Idempotent: a row already not finalized returns unchanged.
     *
     * <p>The note goes with the closure it explains. Carrying it across a
     * reopen would attach one closure's words to the next one, and the UI
     * pre-fills the re-close box from the value it read BEFORE reopening.</p>
     */
    @Nullable
    AnnotationSet reopenSet( Long id );

    /**
     * Correct the run-provenance envelope on an existing row in place, so a mis-stamped
     * {@code agentVersion} / {@code model} / {@code agentName} / {@code runSha} / {@code ranAt} is
     * fixed without the delete-and-recreate that mints a new id. Only the envelope moves; the set's
     * content ({@code payloadJson}, {@code parkedElements}), its identity ({@code role},
     * {@code source}, {@code runId}, {@code investigation}, {@code parent}) and its finalized status
     * are untouched.
     * <p>
     * Per field: null leaves the stored value alone, and a blank string clears it. {@code ranAt} has
     * no blank form, so it can be corrected but not cleared.
     *
     * @return the updated row, or null if no row has that id
     */
    @Nullable
    AnnotationSet updateProvenance( Long id, RunProvenance provenance );

    /**
     * Delete the row with the given id. Returns true if a row was
     * removed, false if no such row existed. Cascades on parent edges
     * are governed by {@code ON DELETE SET NULL}: descendants survive,
     * their {@code parent} link is cleared.
     */
    boolean delete( Long id );

    long countSince( @Nullable Date since, @Nullable AnnotationSetRole roleFilter );

    Map<AnnotationSetRole, Long> countByRoleSince( @Nullable Date since );

    long countDistinctRunIdsSince( @Nullable Date since,
            @Nullable AnnotationSetRole roleFilter );

    @Nullable
    Date findLatestCreatedAt( @Nullable AnnotationSetRole roleFilter );

    /**
     * Return value of {@link #attach}. Carries the persisted row plus a
     * flag noting whether it was created (true) or returned as existing
     * (false). REST callers use it to choose 201 vs 200.
     */
    class AttachedAnnotationSet {
        private final AnnotationSet annotationSet;
        private final boolean created;

        public AttachedAnnotationSet( AnnotationSet annotationSet, boolean created ) {
            this.annotationSet = annotationSet;
            this.created = created;
        }

        public AnnotationSet getAnnotationSet() {
            return annotationSet;
        }

        public boolean isCreated() {
            return created;
        }
    }

    /**
     * Record where a proposal stands with its reviewer, and return the updated set.
     * <p>
     * 🛑 The value is stored as given. There is deliberately no vocabulary check here — Paul,
     * 2026-09-04: <i>"don't lock us into any kind of enums. if we settle down on this we might
     * formalize it."</i> {@code pending | needs_changes | accepted | rejected} are the values in use,
     * not the values permitted, and a caller sending a fifth gets it stored rather than rejected. The
     * only gate is structural: non-blank, and short enough for the column.
     *
     * @param annotationSet the set to rule on
     * @param status        the status, already trimmed and lowercased by the caller
     * @return the updated set
     */
    AnnotationSet updateStatus( AnnotationSet annotationSet, String status );
}
