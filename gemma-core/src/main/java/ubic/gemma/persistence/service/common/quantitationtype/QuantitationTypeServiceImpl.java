/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.common.quantitationtype;

import org.hibernate.NonUniqueResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;

import java.util.*;

/**
 * @author keshav
 * @author pavlidis
 * @see    QuantitationTypeService
 */
@Service
public class QuantitationTypeServiceImpl extends AbstractFilteringVoEnabledService<QuantitationType, QuantitationTypeValueObject> implements QuantitationTypeService {

    private final QuantitationTypeDao quantitationTypeDao;

    @Autowired
    public QuantitationTypeServiceImpl( QuantitationTypeDao quantitationTypeDao ) {
        super( quantitationTypeDao );
        this.quantitationTypeDao = quantitationTypeDao;
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
    public QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) throws NonUniqueQuantitationTypeByNameException {
        try {
            return this.quantitationTypeDao.findByNameAndVectorType( ee, name, dataVectorType );
        } catch ( NonUniqueResultException e ) {
            throw new NonUniqueQuantitationTypeByNameException( String.format( "More than one QuantitationType uses %s as name in %s for vectors of type %s.", name, ee, dataVectorType ), e );
        }
    }

    @Override
    @Transactional
    public QuantitationType findOrCreate( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType ) {
        return quantitationTypeDao.findOrCreate( quantitationType, dataVectorType );
    }

    @Override
    @Transactional
    public QuantitationType create( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType ) {
        return this.quantitationTypeDao.create( quantitationType, dataVectorType );
    }
}