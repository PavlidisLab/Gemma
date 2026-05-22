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
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraft;
import ubic.gemma.model.expression.experiment.AgentProposal;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service surface for the unified {@link CurationDraft} buffer (one row per
 * {@code (investigation, curator)} pair, carrying both the WIP payload AND
 * any seeding proposal snapshot — see {@code STATUS_UNIFIED_CURATION_DRAFT.md}).
 *
 * <p>Draft state changes are NOT audited; they're buffer state. The COMMIT
 * step downstream emits typed audit events through the existing design /
 * annotation write endpoints.</p>
 */
public interface CurationDraftService {

    /**
     * @return the draft for the given {@code (investigationId, curator)}, or
     *         empty if none exists.
     */
    Optional<CurationDraft> findForCurator( Long investigationId, User curator );

    /**
     * @return the draft with the given id, or empty.
     */
    Optional<CurationDraft> findById( Long draftId );

    /**
     * Upsert the draft for the given {@code (investigationId, curator)}. If
     * no row exists one is created with {@code startedAt = now}. Otherwise
     * the existing row is mutated and its {@code lastEditedAt} stamped.
     *
     * @param investigationId      target EE id; required.
     * @param curator              owner; required.
     * @param payloadJson          full WIP payload; required.
     * @param proposalId           optionally bind/rebind the seeding
     *                             proposal. If supplied and the draft did
     *                             not already carry this proposal id, the
     *                             snapshot is captured from
     *                             {@code AgentProposal.payloadJson} at this
     *                             call.
     * @param parkedElementsJson   JSON array of parked element keys, or
     *                             {@code null} to leave the existing field
     *                             untouched. Pass an empty array
     *                             ({@code "[]"}) to clear.
     */
    CurationDraft saveOrUpdate( Long investigationId, User curator,
            String payloadJson,
            @Nullable Long proposalId,
            @Nullable String parkedElementsJson );

    /**
     * Seed a draft from a fresh agent proposal. Creates the row (or
     * overwrites the existing one's snapshot + proposal binding) and
     * captures the proposal payload verbatim as the disposition baseline.
     */
    CurationDraft seedFromProposal( Long investigationId, User curator,
            AgentProposal proposal, String initialPayloadJson );

    /**
     * Discard the draft for the given pair. No-op if none exists.
     */
    void delete( Long investigationId, User curator );

    /**
     * List drafts owned by the curator. {@code since} optionally bounds by
     * {@code lastEditedAt >= since}. {@code limit=0} returns all.
     */
    List<CurationDraft> findByCurator( User curator, @Nullable Date since, int offset, int limit );

    /**
     * List drafts that were seeded from the given proposal — one per curator
     * who has started reviewing.
     */
    List<CurationDraft> findByProposal( Long proposalId );

    /**
     * Stamp {@code finalizedAt = now}. Lighter than commit: the row stays
     * intact, but downstream tooling can treat it as ready-to-push. Returns
     * the updated draft; throws {@link IllegalStateException} if no draft
     * exists for the pair.
     */
    CurationDraft finalize( Long investigationId, User curator );
}
