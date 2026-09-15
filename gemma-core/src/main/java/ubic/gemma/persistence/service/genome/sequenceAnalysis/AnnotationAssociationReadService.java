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
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;

import java.util.Collection;

/**
 * Read-only retrieval service for {@link AnnotationAssociation}.
 * <p>
 * Phase 3 of the {@link AnnotationAssociationService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on the
 * {@code AnnotationAssociationServiceImpl} facade: the two {@code find(...)} overloads
 * keyed by {@link BioSequence} and {@link Gene}. Both delegate to
 * {@link AnnotationAssociationDao} and orchestrate no other collaborators.
 * <p>
 * The pure in-memory transformation {@code removeRootTerms} is not a persistence read
 * and remains on the facade.
 * <p>
 * Callers should generally keep using {@link AnnotationAssociationService} as the facade
 * -- the facade delegates to this service. Direct injection is appropriate where a class
 * would otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link AnnotationAssociationService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so
 * this interface is intentionally unsecured.
 *
 * @see AnnotationAssociationService
 */
public interface AnnotationAssociationReadService {

    /**
     * @see AnnotationAssociationDao#find(BioSequence)
     */
    Collection<AnnotationAssociation> find( BioSequence bioSequence );

    /**
     * @see AnnotationAssociationDao#find(Gene)
     */
    Collection<AnnotationAssociation> find( Gene gene );
}
