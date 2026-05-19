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

import org.hibernate.NonUniqueResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link QuantitationTypeReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)} except those that the
 * facade explicitly marked as not requiring a transaction
 * ({@code getVectorTypes}, {@code getMappedDataVectorType}). ACL enforcement is the
 * responsibility of the facade {@link QuantitationTypeService} interface -- this class
 * is unsecured at the AOP boundary on purpose, so intra-{@code gemma-core} callers can
 * bypass duplicate ACL checks once authenticated.
 *
 * @see QuantitationTypeService
 */
@Service("quantitationTypeReadService")
public class QuantitationTypeReadServiceImpl implements QuantitationTypeReadService {

    private final QuantitationTypeDao quantitationTypeDao;

    @Autowired
    public QuantitationTypeReadServiceImpl( QuantitationTypeDao quantitationTypeDao ) {
        this.quantitationTypeDao = quantitationTypeDao;
    }

    @Override
    // does not need to a transaction
    public Collection<Class<? extends DataVector>> getVectorTypes() {
        return this.quantitationTypeDao.getVectorTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Class<? extends DataVector>, Set<QuantitationType>> findByExpressionExperiment( ExpressionExperiment ee ) {
        return quantitationTypeDao.findByExpressionExperiment( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Class<? extends T> dataVectorType ) {
        return quantitationTypeDao.findByExpressionExperiment( ee, dataVectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Collection<Class<? extends T>> vectorTypes ) {
        Collection<QuantitationType> results = new HashSet<>();
        for ( Class<? extends DataVector> vectorType : vectorTypes ) {
            results.addAll( findByExpressionExperiment( ee, vectorType ) );
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return quantitationTypeDao.findByExpressionExperimentAndDimension( expressionExperiment, dimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Collection<Class<? extends BulkExpressionDataVector>> vectorTypes ) {
        return quantitationTypeDao.findByExpressionExperimentAndDimension( expressionExperiment, dimension, vectorTypes );
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment expressionExperiment ) {
        return this.quantitationTypeDao.loadValueObjectsWithExpressionExperiment( qts, expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public Class<? extends DataVector> getDataVectorType( QuantitationType qt ) {
        return quantitationTypeDao.getDataVectorType( qt );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<QuantitationType, Class<? extends DataVector>> getDataVectorTypes( Collection<QuantitationType> qts ) {
        Map<QuantitationType, Class<? extends DataVector>> vectorTypes = new HashMap<>();
        for ( QuantitationType qt : qts ) {
            if ( !vectorTypes.containsKey( qt ) ) {
                vectorTypes.put( qt, getDataVectorType( qt ) );
            }
        }
        return vectorTypes;
    }

    @Override
    // no need for a transaction
    public <T extends DataVector> Collection<Class<? extends T>> getMappedDataVectorType( Class<T> vectorType ) {
        return quantitationTypeDao.getMappedDataVectorTypes( vectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType loadById( Long id, ExpressionExperiment ee ) {
        return quantitationTypeDao.loadById( id, ee );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType ) {
        return quantitationTypeDao.loadByIdAndVectorType( id, ee, dataVectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType reload( QuantitationType quantitationType ) {
        return quantitationTypeDao.reload( quantitationType );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType, Class<? extends DataVector> dataVectorTypes ) {
        return this.quantitationTypeDao.find( ee, quantitationType, Collections.singleton( dataVectorTypes ) );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType findByName( ExpressionExperiment ee, String name ) throws NonUniqueQuantitationTypeByNameException {
        try {
            return quantitationTypeDao.findByNameAndVectorType( ee, name, RawExpressionDataVector.class );
        } catch ( NonUniqueResultException e ) {
            throw new NonUniqueQuantitationTypeByNameException( String.format( "More than one QuantitationType uses %s as name.", name ), e );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) throws NonUniqueQuantitationTypeByNameException {
        try {
            return this.quantitationTypeDao.findByNameAndVectorType( ee, name, dataVectorType );
        } catch ( NonUniqueResultException e ) {
            throw new NonUniqueQuantitationTypeByNameException( String.format( "More than one QuantitationType uses %s as name in %s for vectors of type %s.", name, ee, dataVectorType ), e );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends DataVector> Collection<QuantitationType> findAllByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends T> vectorType ) {
        return quantitationTypeDao.findAllByNameAndVectorType( ee, name, vectorType );
    }
}
