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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;

import java.util.Collection;

/**
 * Read-only retrieval service for {@link BlatAssociation}.
 * <p>
 * Phase 3 of the {@link BlatAssociationService} decomposition (strangler fig).
 * This service houses the read cluster previously implemented directly on the
 * {@code BlatAssociationServiceImpl} facade: the two {@code find} overloads
 * (by {@link BioSequence} and by {@link Gene}) and {@code findAndThaw}. The
 * {@code find} methods delegate straight to {@link BlatAssociationDao};
 * {@code findAndThaw} also calls {@code thaw} on the DAO.
 * <p>
 * Write-side methods (the inherited
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.AdminEditableBaseImmutableService}
 * mutators {@code create}, {@code findOrCreate}, {@code remove}, all
 * {@code @Secured("GROUP_ADMIN")}) stay on the {@link BlatAssociationService}
 * facade.
 * <p>
 * Callers should generally keep using {@link BlatAssociationService} as the
 * facade -- the facade delegates to this service. Direct injection is
 * appropriate where a class is logically read-only (analysis pipelines that
 * look up BLAT associations for a BioSequence or Gene).
 * <p>
 * ACL / {@code @Secured} annotations: the three read methods on the facade
 * carry no security annotations of their own (the {@code @Secured} on
 * {@link ubic.gemma.persistence.service.common.auditAndSecurity.AdminEditableBaseImmutableService}
 * covers the write side only). The new read impl is unsecured at the AOP
 * boundary on purpose, so intra-{@code gemma-core} callers that already hold
 * an authenticated session bypass duplicate ACL checks.
 *
 * @see BlatAssociationService
 */
public interface BlatAssociationReadService {

    Collection<BlatAssociation> find( BioSequence bioSequence );

    Collection<BlatAssociation> find( Gene gene );

    Collection<BlatAssociation> findAndThaw( BioSequence bioSequence );
}
