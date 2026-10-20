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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.util.Slice;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Implementation of {@link FactorValueReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link FactorValueService} interface — this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see FactorValueService
 */
@Service("factorValueReadService")
public class FactorValueReadServiceImpl implements FactorValueReadService {

    private final FactorValueDao factorValueDao;

    @Autowired
    public FactorValueReadServiceImpl( FactorValueDao factorValueDao ) {
        this.factorValueDao = factorValueDao;
    }

    @Override
    @Transactional(readOnly = true)
    public FactorValue loadWithExperimentalFactor( Long id ) {
        FactorValue fv = factorValueDao.load( id );
        if ( fv != null ) {
            Hibernate.initialize( fv.getExperimentalFactor() );
        }
        return fv;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> FactorValue loadWithExperimentalFactorOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        FactorValue fv = factorValueDao.load( id );
        if ( fv == null ) {
            throw exceptionSupplier.apply( String.format( "No %s with ID %d.",
                    FactorValue.class.getName(), id ) );
        }
        Hibernate.initialize( fv.getExperimentalFactor() );
        return fv;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, Characteristic> getExperimentalFactorCategoriesIgnoreAcls( Collection<FactorValue> factorValues ) {
        return factorValueDao.getExperimentalFactorCategories( factorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperiment> getExpressionExperimentsIgnoreAcls( Collection<FactorValue> factorValues ) {
        return factorValueDao.getExpressionExperimentsIgnoreAcls( factorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public FactorValue loadWithOldStyleCharacteristics( Long id, boolean readOnly ) {
        return factorValueDao.loadWithOldStyleCharacteristics( id, readOnly );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> loadIdsWithNumberOfOldStyleCharacteristics( Set<Long> excludedIds ) {
        return factorValueDao.loadIdsWithNumberOfOldStyleCharacteristics( excludedIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValue> loadIgnoreAcls( Set<Long> ids ) {
        return factorValueDao.load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<FactorValue> loadAll( int offset, int limit ) {
        return factorValueDao.loadAll( offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> loadAllIds() {
        return factorValueDao.loadAllIds();
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<Long> loadAllIds( int offset, int limit ) {
        return factorValueDao.loadAllIds( offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValue> findByValueStartingWith( String valuePrefix, int maxResults ) {
        return this.factorValueDao.findByValueStartingWith( valuePrefix, maxResults );
    }
}
