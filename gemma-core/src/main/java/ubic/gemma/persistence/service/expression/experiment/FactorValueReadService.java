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

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.Slice;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Read-only retrieval service for {@link FactorValue}.
 * <p>
 * Phase 3 of the {@link FactorValueService} decomposition (strangler fig). This service
 * houses the read-side cluster previously implemented directly on the
 * {@code FactorValueServiceImpl} facade: {@code loadWithExperimentalFactor},
 * {@code loadWithExperimentalFactorOrFail}, {@code getExperimentalFactorCategoriesIgnoreAcls},
 * {@code getExpressionExperimentsIgnoreAcls}, {@code loadWithOldStyleCharacteristics},
 * {@code loadIdsWithNumberOfOldStyleCharacteristics}, {@code loadIgnoreAcls},
 * {@code loadAll(int,int)}, {@code loadAllIds()}, {@code loadAllIds(int,int)}, and
 * {@code findByValueStartingWith}. All methods delegate to {@link FactorValueDao} with no
 * orchestration of write-side collaborators.
 * <p>
 * Callers should generally keep using {@link FactorValueService} as the facade — the
 * facade delegates to this service. Direct injection is appropriate where a class would
 * otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link FactorValueService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so
 * this interface is intentionally unsecured.
 *
 * @see FactorValueService
 */
public interface FactorValueReadService {

    /**
     * Load a {@link FactorValue} by id and eagerly initialize its experimental factor.
     */
    @Nullable
    FactorValue loadWithExperimentalFactor( Long id );

    /**
     * Load a {@link FactorValue} with an initialized experimental factor, or throw the
     * supplied exception if no such factor value exists.
     */
    @NonNull
    <T extends Exception> FactorValue loadWithExperimentalFactorOrFail( Long id, Function<String, T> exceptionSupplier ) throws T;

    /**
     * Return the experimental factor categories of the given factor values.
     *
     * @see FactorValueDao#getExperimentalFactorCategories(Collection)
     */
    Map<FactorValue, Characteristic> getExperimentalFactorCategoriesIgnoreAcls( Collection<FactorValue> factorValues );

    /**
     * Return the {@link ExpressionExperiment} that owns each of the given factor values.
     * <p>
     * For efficiency, only the ID, short name and name of the EEs are populated.
     *
     * @see FactorValueDao#getExpressionExperimentsIgnoreAcls(Collection)
     */
    Map<FactorValue, ExpressionExperiment> getExpressionExperimentsIgnoreAcls( Collection<FactorValue> factorValues );

    /**
     * @see FactorValueDao#loadWithOldStyleCharacteristics(Long, boolean)
     * @deprecated do not use, this is only for migrating old-style characteristics to
     *             statements and will be removed
     */
    @Nullable
    @Deprecated
    FactorValue loadWithOldStyleCharacteristics( Long id, boolean readOnly );

    /**
     * @see FactorValueDao#loadIdsWithNumberOfOldStyleCharacteristics(Set)
     * @deprecated do not use, this is only for migrating old-style characteristics to
     *             statements and will be removed
     */
    @Deprecated
    Map<Long, Integer> loadIdsWithNumberOfOldStyleCharacteristics( Set<Long> excludedIds );

    /**
     * Load {@link FactorValue}s by IDs, ignoring ACLs.
     */
    Collection<FactorValue> loadIgnoreAcls( Set<Long> ids );

    /**
     * @see FactorValueDao#loadAll(int, int)
     */
    Slice<FactorValue> loadAll( int offset, int limit );

    /**
     * @see FactorValueDao#loadAllIds()
     */
    Collection<Long> loadAllIds();

    /**
     * @see FactorValueDao#loadAllIds(int, int)
     */
    Slice<Long> loadAllIds( int offset, int limit );

    /**
     * @see FactorValueDao#findByValueStartingWith(String, int)
     * @deprecated because {@link FactorValue#getValue()} is deprecated
     */
    @Deprecated
    Collection<FactorValue> findByValueStartingWith( String valuePrefix, int maxResults );
}
