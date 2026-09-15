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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author keshav
 * @author pavlidis
 * @see    QuantitationTypeService
 */
@Service
public class QuantitationTypeServiceImpl extends AbstractFilteringVoEnabledService<QuantitationType, QuantitationTypeValueObject> implements QuantitationTypeService {

    private final QuantitationTypeDao quantitationTypeDao;

    @Autowired
    private QuantitationTypeReadService readService;

    @Autowired
    public QuantitationTypeServiceImpl( QuantitationTypeDao quantitationTypeDao ) {
        super( quantitationTypeDao );
        this.quantitationTypeDao = quantitationTypeDao;
    }

    // =====================================================================
    // Read methods -- delegate to QuantitationTypeReadService.
    // ACL @Secured annotations live on the QuantitationTypeService interface
    // and apply at the facade proxy boundary.
    // =====================================================================

    @Override
    public Collection<Class<? extends DataVector>> getVectorTypes() {
        return readService.getVectorTypes();
    }

    @Override
    public Map<Class<? extends DataVector>, Set<QuantitationType>> findByExpressionExperiment( ExpressionExperiment ee ) {
        return readService.findByExpressionExperiment( ee );
    }

    @Override
    public <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Class<? extends T> dataVectorType ) {
        return readService.findByExpressionExperiment( ee, dataVectorType );
    }

    @Override
    public <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Collection<Class<? extends T>> vectorTypes ) {
        return readService.findByExpressionExperiment( ee, vectorTypes );
    }

    @Override
    public Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension ) {
        return readService.findByExpressionExperimentAndDimension( expressionExperiment, dimension );
    }

    @Override
    public Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Collection<Class<? extends BulkExpressionDataVector>> vectorTypes ) {
        return readService.findByExpressionExperimentAndDimension( expressionExperiment, dimension, vectorTypes );
    }

    @Override
    public List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment expressionExperiment ) {
        return readService.loadValueObjectsWithExpressionExperiment( qts, expressionExperiment );
    }

    @Override
    public Class<? extends DataVector> getDataVectorType( QuantitationType qt ) {
        return readService.getDataVectorType( qt );
    }

    @Override
    public Map<QuantitationType, Class<? extends DataVector>> getDataVectorTypes( Collection<QuantitationType> qts ) {
        return readService.getDataVectorTypes( qts );
    }

    @Override
    public <T extends DataVector> Collection<Class<? extends T>> getMappedDataVectorType( Class<T> vectorType ) {
        return readService.getMappedDataVectorType( vectorType );
    }

    @Override
    public QuantitationType loadById( Long id, ExpressionExperiment ee ) {
        return readService.loadById( id, ee );
    }

    @Override
    public QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType ) {
        return readService.loadByIdAndVectorType( id, ee, dataVectorType );
    }

    @Override
    public QuantitationType reload( QuantitationType quantitationType ) {
        return readService.reload( quantitationType );
    }

    @Override
    public QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType, Class<? extends DataVector> dataVectorTypes ) {
        return readService.find( ee, quantitationType, dataVectorTypes );
    }

    @Override
    public QuantitationType findByName( ExpressionExperiment ee, String name ) throws NonUniqueQuantitationTypeByNameException {
        return readService.findByName( ee, name );
    }

    @Override
    public QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) throws NonUniqueQuantitationTypeByNameException {
        return readService.findByNameAndVectorType( ee, name, dataVectorType );
    }

    @Override
    public <T extends DataVector> Collection<QuantitationType> findAllByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends T> vectorType ) {
        return readService.findAllByNameAndVectorType( ee, name, vectorType );
    }

    // =====================================================================
    // Write methods stay on the facade.
    // =====================================================================

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
