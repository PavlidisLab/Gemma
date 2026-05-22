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
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraft;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Date;
import java.util.List;

/**
 * DAO for {@link CurationDraft}. The contract is small because drafts are
 * addressed either by id or by the {@code (investigation, curator)} pair (the
 * UNIQUE key).
 */
public interface CurationDraftDao extends BaseDao<CurationDraft> {

    /**
     * Find the draft for the given {@code (investigation, curator)} pair, or
     * {@code null} if none exists.
     */
    @Nullable
    CurationDraft findByInvestigationAndCurator( Investigation investigation, Contact curator );

    /**
     * List drafts owned by a curator. {@code since} optionally bounds by
     * {@code lastEditedAt >= since}. Ordering is {@code lastEditedAt DESC,
     * id DESC}. {@code offset} and {@code limit} are bounded by the caller;
     * pass {@code limit=0} to return all.
     */
    List<CurationDraft> findByCurator( Contact curator, @Nullable Date since, int offset, int limit );

    /**
     * List drafts that were seeded from the given proposal (one per curator
     * who has started reviewing it).
     */
    List<CurationDraft> findByProposal( Long proposalId );
}
