/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;

import java.util.Collection;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 *
 * @see DesignElementDataVectorService
 */
public abstract class DesignElementDataVectorServiceBase implements DesignElementDataVectorService {

    @Autowired
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {
        return this.handleCountAll();
    }

    @Override
    @Transactional
    public java.util.Collection<? extends DesignElementDataVector> create(
            final java.util.Collection<? extends DesignElementDataVector> vectors ) {
        return this.handleCreate( vectors );

    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<? extends DesignElementDataVector> find(
            final java.util.Collection<QuantitationType> quantitationTypes ) {
        return this.handleFind( quantitationTypes );

    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<? extends DesignElementDataVector> find(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.handleFind( quantitationType );

    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<? extends DesignElementDataVector> find(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.handleFind( arrayDesign, quantitationType );

    }

    /**
     * @return the processedExpressionDataVectorDao
     */
    public ProcessedExpressionDataVectorDao getProcessedExpressionDataVectorDao() {
        return processedExpressionDataVectorDao;
    }

    /**
     * @param processedExpressionDataVectorDao the processedExpressionDataVectorDao to set
     */
    public void setProcessedExpressionDataVectorDao(
            ProcessedExpressionDataVectorDao processedExpressionDataVectorDao ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
    }

    @Override
    @Transactional(readOnly = true)
    public DesignElementDataVector load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    @Override
    @Transactional
    public void remove( final java.util.Collection<? extends DesignElementDataVector> vectors ) {
        this.handleRemove( vectors );

    }

    @Override
    @Transactional
    public void remove(
            final ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector designElementDataVector ) {
        this.handleRemove( designElementDataVector );

    }

    @Override
    @Transactional
    public void removeDataForCompositeSequence(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        this.handleRemoveDataForCompositeSequence( compositeSequence );

    }

    @Override
    @Transactional
    public void removeDataForQuantitationType(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        this.handleRemoveDataForQuantitationType( quantitationType );

    }

    @Transactional(readOnly = true)
    @Override
    public void thaw( final java.util.Collection<? extends DesignElementDataVector> designElementDataVectors ) {
        this.handleThaw( designElementDataVectors );
    }

    @Override
    @Transactional
    public void update( final DesignElementDataVector dedv ) {
        this.handleUpdate( dedv );

    }

    @Override
    @Transactional
    public void update( final java.util.Collection<? extends DesignElementDataVector> dedvs ) {
        this.handleUpdate( dedvs );
    }

    protected RawExpressionDataVectorDao getRawExpressionDataVectorDao() {
        return this.rawExpressionDataVectorDao;
    }

    /**
     * @param rawExpressionDataVectorDao the rawExpressionDataVectorDao to set
     */
    public void setRawExpressionDataVectorDao( RawExpressionDataVectorDao rawExpressionDataVectorDao ) {
        this.rawExpressionDataVectorDao = rawExpressionDataVectorDao;
    }

    protected abstract java.lang.Integer handleCountAll();

    protected abstract java.util.Collection<? extends DesignElementDataVector> handleCreate(
            java.util.Collection<? extends DesignElementDataVector> vectors );

    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            java.util.Collection<QuantitationType> quantitationTypes );

    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    protected abstract ubic.gemma.model.expression.bioAssayData.DesignElementDataVector handleLoad( java.lang.Long id );

    protected abstract void handleRemove( DesignElementDataVector designElementDataVector );

    protected abstract void handleRemove( java.util.Collection<? extends DesignElementDataVector> vectors );

    protected abstract void handleRemoveDataForCompositeSequence(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    protected abstract void handleRemoveDataForQuantitationType(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    protected abstract void handleThaw( Collection<? extends DesignElementDataVector> designElementDataVectors );

    protected abstract void handleUpdate( DesignElementDataVector dedv );

    protected abstract void handleUpdate( java.util.Collection<? extends DesignElementDataVector> dedvs );

}