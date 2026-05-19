/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.Collection;

/**
 * Read-only retrieval service for {@link BlatResult}.
 * <p>
 * Phase 3 of the {@link BlatResultService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on
 * the {@code BlatResultServiceImpl} facade: {@code findByBioSequence} and the
 * two {@code thaw} overloads. All three methods delegate straight to
 * {@link BlatResultDao}.
 * <p>
 * Write-side methods (the inherited {@code AdminEditableBaseService} mutators
 * {@code create}, {@code findOrCreate}, {@code update}, {@code save},
 * {@code remove}, all {@code @Secured("GROUP_ADMIN")}) stay on the
 * {@link BlatResultService} facade.
 * <p>
 * Callers should generally keep using {@link BlatResultService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a
 * class is logically read-only (analysis pipelines that look up BLAT results
 * for a BioSequence, REST endpoints, etc.).
 * <p>
 * ACL / {@code @Secured} annotations: the three read methods on the facade
 * carry no security annotations of their own (the {@code @Secured} on
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.AdminEditableBaseService}
 * covers the write side only). The new read impl is unsecured at the AOP
 * boundary on purpose, so intra-{@code gemma-core} callers that already hold
 * an authenticated session bypass duplicate ACL checks.
 *
 * @see BlatResultService
 */
public interface BlatResultReadService {

    Collection<BlatResult> findByBioSequence( BioSequence bioSequence );

    BlatResult thaw( BlatResult blatResult );

    Collection<BlatResult> thaw( Collection<BlatResult> blatResults );
}
