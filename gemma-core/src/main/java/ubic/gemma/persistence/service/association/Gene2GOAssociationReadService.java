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
package ubic.gemma.persistence.service.association;

import org.springframework.lang.Nullable;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.Map;

/**
 * Read-only retrieval service for {@link Gene2GOAssociation}.
 * <p>
 * Phase 3 strangler-fig decomposition of {@link Gene2GOAssociationService}: this service
 * owns the DAO-bound read cluster previously implemented directly on the
 * {@code Gene2GOAssociationServiceImpl} facade -- {@code findAssociationByGene},
 * {@code findAssociationByGenes}, {@code findByGene}, {@code findByGenes},
 * {@code findByGOTermUris} (taxon-bound and unbound), and
 * {@code findByGOTermUrisPerTaxon}. The {@code findByGene} / {@code findByGenes} pair is
 * backed by the {@code Gene2GoServiceCache} ehcache region; this read service owns the
 * cache wiring.
 * <p>
 * Write-side methods ({@code create}, {@code findOrCreate}, {@code remove}, {@code removeAll})
 * remain on the {@link Gene2GOAssociationService} facade. The facade now delegates its
 * read surface to this service.
 * <p>
 * Callers should generally keep using {@link Gene2GOAssociationService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a class is
 * logically read-only (analysis pipelines, ontology services, search backends).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link Gene2GOAssociationService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so
 * this interface is intentionally unsecured at the AOP boundary -- intra-{@code gemma-core}
 * callers that inject this service directly bypass any duplicate ACL check. The facade
 * currently carries {@code @Secured("GROUP_ADMIN")} on writes only; read methods are
 * unsecured at the facade.
 *
 * @see Gene2GOAssociationService
 */
public interface Gene2GOAssociationReadService {

    Collection<Gene2GOAssociation> findAssociationByGene( Gene gene );

    Collection<Gene2GOAssociation> findAssociationByGenes( Collection<Gene> genes );

    Collection<Characteristic> findByGene( Gene gene );

    Map<Gene, Collection<Characteristic>> findByGenes( Collection<Gene> genes );

    /**
     * Find all the genes that match any of the terms.
     * <p>
     * Used to fetch genes associated with a term + children.
     */
    Collection<Gene> findByGOTermUris( Collection<String> uris, @Nullable Taxon taxon );

    /**
     * Find all genes associated with a given set of GO terms, grouped by taxon.
     */
    Map<Taxon, Collection<Gene>> findByGOTermUrisPerTaxon( Collection<String> uris );
}
