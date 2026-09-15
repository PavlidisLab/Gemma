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
package ubic.gemma.persistence.service.expression.biomaterial;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Read-only retrieval service for {@link BioMaterial}.
 * <p>
 * Phase 3 of the {@link BioMaterialService} decomposition (strangler fig). This service
 * houses the read-side cluster previously implemented directly on the
 * {@code BioMaterialServiceImpl} facade: {@code copy}, {@code findSubBioMaterials},
 * {@code findSiblings}, {@code findByExperiment}, {@code findByFactor},
 * {@code loadAndThawOrFail}, {@code getExpressionExperiments}, and the {@code thaw}
 * variants. All methods delegate to {@link BioMaterialDao} (and
 * {@link ubic.gemma.persistence.util.Thaws} for eager-loading) with no orchestration of
 * write-side collaborators.
 * <p>
 * Callers should generally keep using {@link BioMaterialService} as the facade — the
 * facade delegates to this service. Direct injection is appropriate where a class would
 * otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link BioMaterialService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so
 * this interface is intentionally unsecured.
 *
 * @see BioMaterialService
 */
public interface BioMaterialReadService {

    /**
     * Copy a {@link BioMaterial}.
     *
     * @see BioMaterialDao#copy(BioMaterial)
     */
    BioMaterial copy( BioMaterial bioMaterial );

    /**
     * @see BioMaterialDao#findSubBioMaterials(BioMaterial, boolean)
     */
    Collection<BioMaterial> findSubBioMaterials( BioMaterial bioMaterial, boolean direct );

    /**
     * Find the siblings of a given biomaterial (other direct sub-materials of the same source).
     */
    Collection<BioMaterial> findSiblings( BioMaterial bioMaterial );

    /**
     * @see BioMaterialDao#findByExperiment(ExpressionExperiment)
     */
    Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment );

    /**
     * @see BioMaterialDao#findByFactor(ExperimentalFactor)
     */
    Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor );

    /**
     * Load a {@link BioMaterial} by ID and thaw it eagerly, or throw the supplied exception
     * if no such biomaterial exists.
     */
    <T extends Exception> BioMaterial loadAndThawOrFail( Long bmId, Function<String, T> exceptionSupplier, String message ) throws T;

    /**
     * Return the {@link ExpressionExperiment} occurrences of a given biomaterial, organized
     * by {@link BioAssay}.
     *
     * @see BioMaterialDao#getExpressionExperiments(BioMaterial)
     */
    Map<BioMaterial, Map<BioAssay, ExpressionExperiment>> getExpressionExperiments( BioMaterial bm );

    /**
     * Thaw a single {@link BioMaterial} for full traversal.
     */
    @CheckReturnValue
    BioMaterial thaw( BioMaterial bioMaterial );

    /**
     * Thaw a collection of {@link BioMaterial} for full traversal.
     */
    @CheckReturnValue
    Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials );
}
