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
package ubic.gemma.persistence.service.genome.biosequence;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Collection;
import java.util.Map;

/**
 * Read-only retrieval service for {@link BioSequence}.
 * <p>
 * Phase 3 of the {@link BioSequenceService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on
 * the {@code BioSequenceServiceImpl} facade: {@code findByAccession},
 * {@code findByGenes}, {@code findByName}, {@code getGenesByAccession},
 * {@code getGenesByName}, {@code thaw} (x2), and
 * {@code findByCompositeSequence}. All methods delegate to
 * {@link BioSequenceDao} and orchestrate no writes.
 * <p>
 * Write-side methods ({@code findOrCreate}, plus the inherited
 * {@code BaseService} mutators) stay on the {@link BioSequenceService} facade.
 * <p>
 * Callers should generally keep using {@link BioSequenceService} as the
 * facade -- the facade delegates to this service. Direct injection is
 * appropriate where a class is logically read-only (REST endpoints, CLIs,
 * browser controllers, intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link BioSequenceService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy
 * boundary, so this interface is intentionally unsecured.
 *
 * @see BioSequenceService
 */
public interface BioSequenceReadService {

    @Nullable
    BioSequence findByAccession( DatabaseEntry accession );

    /**
     * @param genes genes
     * @return matching biosequences for the given genes in a Map (gene to a collection of biosequences). Genes which
     * had no associated sequences are not included in the result.
     */
    Map<Gene, Collection<BioSequence>> findByGenes( Collection<Gene> genes );

    /**
     * @param name name
     * @return all biosequences with names matching the given string. This matches only the name field, not the
     * accession.
     */
    Collection<BioSequence> findByName( String name );

    Collection<Gene> getGenesByAccession( String search );

    Collection<Gene> getGenesByName( String search );

    Collection<BioSequence> thaw( Collection<BioSequence> bioSequences );

    BioSequence thaw( BioSequence bs );

    @Nullable
    BioSequence findByCompositeSequence( CompositeSequence compositeSequence );
}
