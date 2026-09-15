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
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Read-only retrieval service for {@link ExpressionExperimentSubSet} objects in the context of
 * a parent {@link ExpressionExperiment}.
 * <p>
 * Phase 3 of the {@link ExpressionExperimentService} decomposition (strangler fig). This
 * service houses the subset-retrieval cluster previously implemented directly on the
 * {@code ExpressionExperimentServiceImpl} facade: {@code getSubSets*},
 * {@code getSubSetById*}, and the {@code getSubSetsByFactorValue*} variants. All methods
 * delegate to {@link ExpressionExperimentDao} (and occasionally call
 * {@link org.hibernate.Hibernate#initialize} or {@link ubic.gemma.persistence.util.Thaws} for
 * eager-loading) with no orchestration of write-side collaborators.
 * <p>
 * Callers should generally keep using {@link ExpressionExperimentService} as the facade —
 * the facade delegates to this service. Direct injection is appropriate where a class would
 * otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ExpressionExperimentService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so this
 * interface is intentionally unsecured.
 * <p>
 * This service also houses the read methods that used to live on
 * {@link ExpressionExperimentSubSetService} (the standalone subset CRUD service):
 * {@link #findByBioAssayIn}, {@link #getFactorValuesUsed}, {@link #getFactorValuesUsedAsVO},
 * {@link #getArrayDesignsUsed}, {@link #loadSubSet}, {@link #loadSubSetWithBioAssays}. The
 * standalone {@link ExpressionExperimentSubSetService} now delegates these reads here, keeping
 * its facade ACL annotations in place.
 *
 * @see ExpressionExperimentService
 * @see ExpressionExperimentSubSetService
 */
public interface ExpressionExperimentSubSetReadService {

    /**
     * Obtain all the subsets for a given dataset.
     */
    Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( ExpressionExperiment expressionExperiment );

    /**
     * Batched variant: obtain subsets for every experiment in the input collection in a single
     * query, keyed by source experiment.
     */
    Map<ExpressionExperiment, Collection<ExpressionExperimentSubSet>> getSubSetsWithBioAssays( Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Obtain all the subsets for a given dataset, with characteristics initialized.
     */
    Collection<ExpressionExperimentSubSet> getSubSetsWithCharacteristics( ExpressionExperiment ee );

    /**
     * Obtain all the subsets organized by dimension for a given dataset.
     */
    Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimension( ExpressionExperiment expressionExperiment );

    /**
     * Obtain all the subsets organized by dimension for a given dataset, with assays thawed.
     */
    Map<BioAssayDimension, Set<ExpressionExperimentSubSet>> getSubSetsByDimensionWithBioAssays( ExpressionExperiment expressionExperiment );

    /**
     * Obtain the subsets for a particular dimension.
     */
    Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment, BioAssayDimension dimension );

    /**
     * Obtain the subsets for a particular dimension, with assays lightly thawed.
     */
    Collection<ExpressionExperimentSubSet> getSubSetsWithBioAssays( ExpressionExperiment expressionExperiment, BioAssayDimension dimension );

    /**
     * Reconstitute the FV-to-subset mapping for a given experiment along all separating factors.
     */
    Map<ExperimentalFactor, Map<FactorValue, ExpressionExperimentSubSet>> getSubSetsByFactorValue(
            ExpressionExperiment expressionExperiment, BioAssayDimension dimension );

    /**
     * Reconstitute the FV-to-subset mapping for a given experiment and factor.
     */
    @Nullable
    Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValue(
            ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension );

    /**
     * Reconstitute the FV-to-subset mapping for a given experiment and factor, with subset
     * characteristics initialized and assays thawed.
     */
    @Nullable
    Map<FactorValue, ExpressionExperimentSubSet> getSubSetsByFactorValueWithCharacteristicsAndBioAssays(
            ExpressionExperiment expressionExperiment, ExperimentalFactor experimentalFactor, BioAssayDimension dimension );

    /**
     * Obtain a particular subset by ID, with characteristics initialized.
     */
    @Nullable
    ExpressionExperimentSubSet getSubSetByIdWithCharacteristics( ExpressionExperiment ee, Long subSetId );

    /**
     * Obtain a particular subset by ID, with characteristics initialized and assays thawed.
     */
    @Nullable
    ExpressionExperimentSubSet getSubSetByIdWithCharacteristicsAndBioAssays( ExpressionExperiment ee, Long subSetId );

    // -----------------------------------------------------------------------
    // Read methods migrated from the standalone ExpressionExperimentSubSetService.
    // These do NOT take a parent ExpressionExperiment context; they operate directly
    // on ExpressionExperimentSubSet IDs / instances. ACL coverage stays on the
    // ExpressionExperimentSubSetService facade.
    // -----------------------------------------------------------------------

    /**
     * Load a subset by ID, without initializing collections.
     *
     * @see ExpressionExperimentSubSetService#load(Long)
     */
    @Nullable
    ExpressionExperimentSubSet loadSubSet( Long id );

    /**
     * Load a subset by ID with its bio-assays initialized.
     *
     * @see ExpressionExperimentSubSetService#loadWithBioAssays(Long)
     */
    @Nullable
    ExpressionExperimentSubSet loadSubSetWithBioAssays( Long id );

    /**
     * Find every subset that contains any of the supplied bio-assays.
     *
     * @see ExpressionExperimentSubSetService#findByBioAssayIn(Collection)
     */
    Collection<ExpressionExperimentSubSet> findByBioAssayIn( Collection<BioAssay> bioAssays );

    /**
     * Obtain the {@link FactorValue}s used by samples in the given subset for the given factor.
     *
     * @see ExpressionExperimentSubSetService#getFactorValuesUsed(ExpressionExperimentSubSet, ExperimentalFactor)
     */
    Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor );

    /**
     * VO-form variant of {@link #getFactorValuesUsed(ExpressionExperimentSubSet, ExperimentalFactor)}
     * accepting plain IDs.
     *
     * @see ExpressionExperimentSubSetService#getFactorValuesUsed(Long, Long)
     */
    Collection<FactorValueValueObject> getFactorValuesUsedAsVO( Long subSetId, Long experimentalFactor );

    /**
     * Obtain the array designs used by the assays in the subset.
     *
     * @see ExpressionExperimentSubSetService#getArrayDesignsUsed(ExpressionExperimentSubSet)
     */
    Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperimentSubSet subset );
}
