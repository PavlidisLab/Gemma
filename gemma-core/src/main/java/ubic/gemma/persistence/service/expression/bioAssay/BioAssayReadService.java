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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;

import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Read-only retrieval service for {@link BioAssay}.
 * <p>
 * Phase 3 of the {@link BioAssayService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on the
 * {@code BioAssayServiceImpl} facade: {@code findBioAssayDimensions},
 * {@code findByShortName}, {@code findByAccession}, {@code findSubBioAssays},
 * {@code findSiblings}, {@code getBioAssaySets}, {@code thaw} (x2),
 * {@code loadValueObjects}, {@code loadValueObjectsByCursorForExpressionExperiment},
 * and {@code loadValueObjectsByCursorForSubSet}. All methods delegate to
 * {@link BioAssayDao} (and read-side collaborators) and orchestrate no writes.
 * <p>
 * Write-side methods ({@code addBioMaterialAssociation},
 * {@code removeBioMaterialAssociation}, plus the inherited {@code BaseService} mutators)
 * stay on the {@link BioAssayService} facade.
 * <p>
 * Callers should generally keep using {@link BioAssayService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a class
 * is logically read-only (REST endpoints, CLIs, browser controllers, intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link BioAssayService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary,
 * so this interface is intentionally unsecured.
 *
 * @see BioAssayService
 */
public interface BioAssayReadService {

    /**
     * Locate all BioAssayDimensions in which the selected BioAssay occurs.
     */
    Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay );

    @Nullable
    BioAssay findByShortName( String shortName );

    /**
     * @param accession eg GSM12345.
     * @return BioAssays that match based on the plain accession (unconstrained by ExternalDatabase).
     */
    Collection<BioAssay> findByAccession( String accession );

    Collection<BioAssay> findSubBioAssays( BioAssay bioAssay, boolean direct );

    Collection<BioAssay> findSiblings( BioAssay bioAssay );

    /**
     * Obtain all the {@link BioAssaySet} that contain the given {@link BioAssay}.
     */
    Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay );

    @CheckReturnValue
    BioAssay thaw( BioAssay bioAssay );

    @CheckReturnValue
    Collection<BioAssay> thaw( Collection<BioAssay> bioAssays );

    /**
     * @see BioAssayDao#loadValueObjects(Collection, Map, Map, boolean, boolean)
     */
    List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap, boolean basic, boolean allFactorValues );

    /**
     * @see BioAssayDao#loadValueObjectsByCursorForExpressionExperiment(ExpressionExperiment, Cursor, int)
     */
    CursorPage<BioAssayValueObject> loadValueObjectsByCursorForExpressionExperiment(
            ExpressionExperiment ee, @Nullable Cursor cursor, int limit );

    /**
     * @see BioAssayDao#loadValueObjectsByCursorForSubSet(ExpressionExperimentSubSet, Cursor, int)
     */
    CursorPage<BioAssayValueObject> loadValueObjectsByCursorForSubSet(
            ExpressionExperimentSubSet subset, @Nullable Cursor cursor, int limit );
}
