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
package ubic.gemma.persistence.service.genome.gene;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;

import javax.annotation.CheckReturnValue;
import java.util.Collection;

/**
 * Read-only retrieval service for {@link GeneProduct}.
 * <p>
 * Phase 3 of the {@link GeneProductService} decomposition (strangler fig). This service
 * houses the DAO-bound read cluster previously implemented directly on the
 * {@code GeneProductServiceImpl} facade: {@code getGenesByName}, {@code getGenesByNcbiId},
 * {@code findByName}, and {@code thaw}. All methods delegate to {@link GeneProductDao}
 * and orchestrate no other collaborators.
 * <p>
 * The sibling {@link GeneReadService} carries a {@code getProducts(Long geneId)} method
 * that returns a {@code GeneProductValueObject} collection bound to a Gene id; that
 * method belongs to the Gene-side read surface and is not duplicated here.
 * <p>
 * Callers should generally keep using {@link GeneProductService} as the facade -- the
 * facade delegates to this service. Direct injection is appropriate where a class would
 * otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link GeneProductService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so
 * this interface is intentionally unsecured.
 *
 * @see GeneProductService
 */
public interface GeneProductReadService {

    /**
     * @see GeneProductDao#getGenesByName(String)
     */
    Collection<Gene> getGenesByName( String search );

    /**
     * @see GeneProductDao#getGenesByNcbiId(String)
     */
    Collection<Gene> getGenesByNcbiId( String search );

    /**
     * @see GeneProductDao#findByName(String, Taxon)
     */
    Collection<GeneProduct> findByName( String name, Taxon taxon );

    /**
     * @see GeneProductDao#thaw(GeneProduct)
     */
    @CheckReturnValue
    GeneProduct thaw( GeneProduct geneProduct );
}
