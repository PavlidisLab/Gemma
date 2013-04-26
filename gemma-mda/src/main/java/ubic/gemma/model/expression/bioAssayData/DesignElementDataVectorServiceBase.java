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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService
 */
public abstract class DesignElementDataVectorServiceBase implements
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService {

    @Autowired
    private ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Autowired
    private ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        return this.handleCountAll();
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#create(java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> create(
            final java.util.Collection<? extends DesignElementDataVector> vectors ) {
        return this.handleCreate( vectors );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#find(java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> find(
            final java.util.Collection<QuantitationType> quantitationTypes ) {
        return this.handleFind( quantitationTypes );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#find(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> find(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.handleFind( quantitationType );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> find(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.handleFind( arrayDesign, quantitationType );

    }

    /**
     * @return the processedExpressionDataVectorDao
     */
    public ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao getProcessedExpressionDataVectorDao() {
        return processedExpressionDataVectorDao;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#load(java.lang.Long)
     */
    @Override
    public DesignElementDataVector load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#remove(java.util.Collection)
     */
    @Override
    public void remove( final java.util.Collection<? extends DesignElementDataVector> vectors ) {
        this.handleRemove( vectors );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#remove(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)
     */
    @Override
    public void remove( final ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector designElementDataVector ) {
        this.handleRemove( designElementDataVector );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#removeDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public void removeDataForCompositeSequence(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        this.handleRemoveDataForCompositeSequence( compositeSequence );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#removeDataForQuantitationType(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public void removeDataForQuantitationType(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        this.handleRemoveDataForQuantitationType( quantitationType );

    }

    /**
     * @param processedExpressionDataVectorDao the processedExpressionDataVectorDao to set
     */
    public void setProcessedExpressionDataVectorDao(
            ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorDao processedExpressionDataVectorDao ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
    }

    /**
     * @param rawExpressionDataVectorDao the rawExpressionDataVectorDao to set
     */
    public void setRawExpressionDataVectorDao(
            ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorDao rawExpressionDataVectorDao ) {
        this.rawExpressionDataVectorDao = rawExpressionDataVectorDao;
    }

    /**
     * @return
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final java.util.Collection<? extends DesignElementDataVector> designElementDataVectors ) {
        this.handleThaw( designElementDataVectors );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#update(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)
     */
    @Override
    public void update( final DesignElementDataVector dedv ) {
        this.handleUpdate( dedv );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends DesignElementDataVector> dedvs ) {
        this.handleUpdate( dedvs );
    }

    /**
     * Gets the reference to <code>designElementDataVector</code>'s DAO.
     */
    protected ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorDao getRawExpressionDataVectorDao() {
        return this.rawExpressionDataVectorDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleCreate(
            java.util.Collection<? extends DesignElementDataVector> vectors );

    /**
     * Performs the core logic for {@link #find(java.util.Collection)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            java.util.Collection<QuantitationType> quantitationTypes );

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * Performs the core logic for
     * {@link #find(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.bioAssayData.DesignElementDataVector handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)}
     */
    protected abstract void handleRemove( DesignElementDataVector designElementDataVector );

    /**
     * Performs the core logic for {@link #remove(java.util.Collection)}
     */
    protected abstract void handleRemove( java.util.Collection<? extends DesignElementDataVector> vectors );

    /**
     * Performs the core logic for
     * {@link #removeDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract void handleRemoveDataForCompositeSequence(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * Performs the core logic for
     * {@link #removeDataForQuantitationType(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract void handleRemoveDataForQuantitationType(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     * 
     * @return
     */
    protected abstract void handleThaw( Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)}
     */
    protected abstract void handleUpdate( DesignElementDataVector dedv );

    /**
     * Performs the core logic for {@link #update(java.util.Collection)}
     */
    protected abstract void handleUpdate( java.util.Collection<? extends DesignElementDataVector> dedvs );

}