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
package ubic.gemma.persistence.service.genome.taxon;

import org.springframework.lang.Nullable;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;

import java.util.Collection;

/**
 * Read-only retrieval service for {@link Taxon}.
 * <p>
 * Phase 3 of the {@link TaxonService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on the
 * {@code TaxonServiceImpl} facade: {@code findByCommonName}, {@code findByScientificName},
 * {@code findByNcbiId}, {@code loadAllTaxaWithGenes}, {@code getTaxaWithGenes},
 * {@code getTaxaWithDatasets}, and {@code getTaxaWithArrays}. All methods delegate to
 * {@link TaxonDao} (with simple aggregation / value-object mapping where appropriate),
 * with two methods orchestrating {@code ExpressionExperimentService} and
 * {@code ArrayDesignService} per-taxon counts (those collaborators are themselves
 * thin read paths into their own DAOs).
 * <p>
 * Write-side methods ({@code updateGenesUsable} plus the inherited {@code BaseService}
 * mutators -- {@code create}, {@code findOrCreate}, {@code remove} -- which carry
 * {@code @Secured("GROUP_ADMIN")} via {@code AdminEditableBaseImmutableService}) stay
 * on the {@link TaxonService} facade.
 * <p>
 * Callers should generally keep using {@link TaxonService} as the facade -- the facade
 * delegates to this service. Direct injection is appropriate where a class is logically
 * read-only (REST endpoints, CLIs, browser controllers, intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link TaxonService} (the caller-facing
 * facade interface); enforcement happens at the facade proxy boundary, not here. The
 * read methods carry no {@code @Secured} annotation on the facade today, so there is
 * nothing security-relevant to carry over.
 *
 * @see TaxonService
 */
public interface TaxonReadService {

    @Nullable
    Taxon findByCommonName( String commonName );

    @Nullable
    Taxon findByScientificName( String scientificName );

    @Nullable
    Taxon findByNcbiId( Integer ncbiId );

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    Collection<Taxon> loadAllTaxaWithGenes();

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    Collection<TaxonValueObject> getTaxaWithGenes();

    /**
     * @return collection of taxa that have expression experiments available.
     */
    Collection<TaxonValueObject> getTaxaWithDatasets();

    /**
     * @return List of taxa with array designs in gemma
     */
    Collection<TaxonValueObject> getTaxaWithArrays();
}
