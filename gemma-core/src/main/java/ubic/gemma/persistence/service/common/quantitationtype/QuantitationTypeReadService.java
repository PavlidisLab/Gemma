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
package ubic.gemma.persistence.service.common.quantitationtype;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only retrieval service for {@link QuantitationType}.
 * <p>
 * Phase 3 of the {@link QuantitationTypeService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on the
 * {@code QuantitationTypeServiceImpl} facade: {@code getVectorTypes},
 * {@code findByExpressionExperiment(...)}, {@code findByExpressionExperimentAndDimension(...)},
 * {@code loadValueObjectsWithExpressionExperiment}, {@code getDataVectorType(s)},
 * {@code getMappedDataVectorType}, {@code loadById(AndVectorType)}, {@code reload},
 * {@code find}, {@code findByName}, {@code findByNameAndVectorType}, and
 * {@code findAllByNameAndVectorType}. All methods delegate to
 * {@link QuantitationTypeDao} (with simple aggregation / exception wrapping where
 * appropriate) and orchestrate no other collaborators.
 * <p>
 * Write-side methods ({@code findOrCreate(qt, vectorType)},
 * {@code create(qt, vectorType)}, plus the inherited {@code BaseService} mutators)
 * stay on the {@link QuantitationTypeService} facade.
 * <p>
 * Callers should generally keep using {@link QuantitationTypeService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a class
 * would otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link QuantitationTypeService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary,
 * so this interface is intentionally unsecured.
 *
 * @see QuantitationTypeService
 */
public interface QuantitationTypeReadService {

    /**
     * @see QuantitationTypeDao#getVectorTypes()
     */
    Collection<Class<? extends DataVector>> getVectorTypes();

    /**
     * @see QuantitationTypeDao#findByExpressionExperiment(ExpressionExperiment)
     */
    Map<Class<? extends DataVector>, Set<QuantitationType>> findByExpressionExperiment( ExpressionExperiment ee );

    /**
     * @see QuantitationTypeDao#findByExpressionExperiment(ExpressionExperiment, Class)
     */
    <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Class<? extends T> dataVectorType );

    /**
     * Union of {@link #findByExpressionExperiment(ExpressionExperiment, Class)} across a
     * collection of vector types.
     */
    <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Collection<Class<? extends T>> vectorTypes );

    /**
     * @see QuantitationTypeDao#findByExpressionExperimentAndDimension(ExpressionExperiment, BioAssayDimension)
     */
    Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension );

    /**
     * @see QuantitationTypeDao#findByExpressionExperimentAndDimension(ExpressionExperiment, BioAssayDimension, Collection)
     */
    Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Collection<Class<? extends BulkExpressionDataVector>> vectorTypes );

    /**
     * @see QuantitationTypeDao#loadValueObjectsWithExpressionExperiment(Collection, ExpressionExperiment)
     */
    List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment expressionExperiment );

    /**
     * @see QuantitationTypeDao#getDataVectorType(QuantitationType)
     */
    @Nullable
    Class<? extends DataVector> getDataVectorType( QuantitationType qt );

    /**
     * Bulk variant of {@link #getDataVectorType(QuantitationType)} keyed by QT.
     */
    Map<QuantitationType, Class<? extends DataVector>> getDataVectorTypes( Collection<QuantitationType> qts );

    /**
     * @see QuantitationTypeDao#getMappedDataVectorTypes(Class)
     */
    <T extends DataVector> Collection<Class<? extends T>> getMappedDataVectorType( Class<T> vectorType );

    /**
     * @see QuantitationTypeDao#loadById(Long, ExpressionExperiment)
     */
    @Nullable
    QuantitationType loadById( Long id, ExpressionExperiment ee );

    /**
     * @see QuantitationTypeDao#loadByIdAndVectorType(Long, ExpressionExperiment, Class)
     */
    @Nullable
    QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType );

    /**
     * @see QuantitationTypeDao#reload(ubic.gemma.model.common.Identifiable)
     */
    QuantitationType reload( QuantitationType quantitationType );

    /**
     * Locate a QT associated with the given ee matching the specification of the passed
     * quantitationType, or null if there isn't one.
     */
    @Nullable
    QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType, Class<? extends DataVector> dataVectorTypes );

    /**
     * Find a QT by name in a given experiment, defaulting to {@code RawExpressionDataVector}.
     *
     * @throws NonUniqueQuantitationTypeByNameException if more than one QT matches the given name
     */
    QuantitationType findByName( ExpressionExperiment ee, String name ) throws NonUniqueQuantitationTypeByNameException;

    /**
     * @throws NonUniqueQuantitationTypeByNameException if more than one QT matches the given name and vector type
     * @see QuantitationTypeDao#findByNameAndVectorType(ExpressionExperiment, String, Class)
     */
    @Nullable
    QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) throws NonUniqueQuantitationTypeByNameException;

    /**
     * @see QuantitationTypeDao#findAllByNameAndVectorType(ExpressionExperiment, String, Class)
     */
    <T extends DataVector> Collection<QuantitationType> findAllByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends T> vectorType );
}
