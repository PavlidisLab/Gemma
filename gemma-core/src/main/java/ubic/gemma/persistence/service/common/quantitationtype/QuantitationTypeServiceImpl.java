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
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;

import java.util.Collection;
import java.util.List;

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
    public List<QuantitationType> loadByDescription( String description ) {
        return this.quantitationTypeDao.loadByDescription( description );
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment expressionExperiment ) {
        return this.quantitationTypeDao.loadValueObjectsWithExpressionExperiment( qts, expressionExperiment );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType ) {
        return quantitationTypeDao.loadByIdAndVectorType( id, ee, dataVectorType );
    }

    @Override
    @Transactional(readOnly = true)
    public QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType ) {
        return this.quantitationTypeDao.find( ee, quantitationType );
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
}