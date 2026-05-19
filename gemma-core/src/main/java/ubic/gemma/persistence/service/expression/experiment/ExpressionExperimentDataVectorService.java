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

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Raw + processed data vector CRUD operations for {@link ExpressionExperiment}.
 * <p>
 * Phase 3 of the {@link ExpressionExperimentService} decomposition (strangler fig, slice 3).
 * Houses the raw / processed expression-data-vector cluster previously implemented directly on
 * the {@code ExpressionExperimentServiceImpl} facade: the {@code getRawDataVectors} family, the
 * {@code addRawDataVectors} / {@code replaceRawDataVectors} / {@code replaceAllRawDataVectors}
 * / {@code removeRawDataVectors} family, the corresponding {@code *ProcessedDataVectors}
 * methods, and the supporting {@link ubic.gemma.model.expression.bioAssayData.BioAssayDimension}
 * + {@link QuantitationType} create-if-needed orchestration shared between raw and processed
 * paths.
 * <p>
 * Callers should generally keep using {@link ExpressionExperimentService} as the facade — the
 * facade delegates to this service. Direct injection is appropriate where a class would
 * otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ExpressionExperimentService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so this
 * interface is intentionally unsecured.
 *
 * @see ExpressionExperimentService
 */
public interface ExpressionExperimentDataVectorService {

    /**
     * @see ExpressionExperimentDao#getRawDataVectors(ExpressionExperiment, QuantitationType)
     */
    Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see ExpressionExperimentDao#getRawDataVectors(ExpressionExperiment, List, QuantitationType)
     */
    Collection<RawExpressionDataVector> getRawDataVectors( ExpressionExperiment ee, List<BioAssay> samples, QuantitationType qt );

    /**
     * @see ExpressionExperimentDao#getPreferredRawDataVectors(ExpressionExperiment)
     */
    Collection<RawExpressionDataVector> getPreferredRawDataVectors( ExpressionExperiment expressionExperiment );

    /**
     * @see ExpressionExperimentDao#getMissingValuesVectors(ExpressionExperiment)
     */
    Map<QuantitationType, Collection<RawExpressionDataVector>> getMissingValuesVectors( ExpressionExperiment ee );

    /**
     * Add raw data vectors for a quantitation type. The dimension and QT are created on-demand
     * if they are still transient.
     */
    int addRawDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<RawExpressionDataVector> newVectors );

    /**
     * @see ExpressionExperimentDao#replaceRawDataVectors(ExpressionExperiment, QuantitationType, Collection)
     */
    int replaceRawDataVectors( ExpressionExperiment ee, QuantitationType quantitationType, Collection<RawExpressionDataVector> vectors );

    /**
     * Replace all raw vectors on an experiment, grouping the input by QT and routing each group
     * to either {@link #replaceRawDataVectors} or {@link #addRawDataVectors} depending on
     * whether the QT already exists; any QT in the experiment that is no longer represented in
     * the input is removed.
     */
    int replaceAllRawDataVectors( ExpressionExperiment ee, Collection<RawExpressionDataVector> vectors );

    /**
     * @see ExpressionExperimentDao#removeAllRawDataVectors(ExpressionExperiment)
     */
    int removeAllRawDataVectors( ExpressionExperiment ee );

    /**
     * Equivalent to {@code removeRawDataVectors(ee, qt, false)}.
     */
    int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt );

    /**
     * @see ExpressionExperimentDao#removeRawDataVectors(ExpressionExperiment, QuantitationType, boolean)
     */
    int removeRawDataVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension );

    /**
     * @see ExpressionExperimentDao#getProcessedDataVectors(ExpressionExperiment)
     */
    Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee );

    /**
     * @see ExpressionExperimentDao#getProcessedDataVectors(ExpressionExperiment, List)
     */
    Optional<Collection<ProcessedExpressionDataVector>> getProcessedDataVectors( ExpressionExperiment ee, List<BioAssay> assays );

    /**
     * @see ExpressionExperimentDao#createProcessedDataVectors(ExpressionExperiment, Collection)
     */
    int createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );

    /**
     * @see ExpressionExperimentDao#removeProcessedDataVectors(ExpressionExperiment)
     */
    int removeProcessedDataVectors( ExpressionExperiment ee );

    /**
     * @see ExpressionExperimentDao#replaceProcessedDataVectors(ExpressionExperiment, Collection)
     */
    int replaceProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vectors );
}
