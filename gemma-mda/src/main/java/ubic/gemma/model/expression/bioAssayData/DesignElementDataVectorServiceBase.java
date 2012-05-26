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
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.countAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#create(java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> create(
            final java.util.Collection<? extends DesignElementDataVector> vectors ) {
        try {
            return this.handleCreate( vectors );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.create(java.util.Collection vectors)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#find(java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> find(
            final java.util.Collection<QuantitationType> quantitationTypes ) {
        try {
            return this.handleFind( quantitationTypes );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.find(java.util.Collection quantitationTypes)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#find(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> find(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.handleFind( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.find(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public java.util.Collection<? extends DesignElementDataVector> find(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.handleFind( arrayDesign, quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.find(ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
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
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#remove(java.util.Collection)
     */
    @Override
    public void remove( final java.util.Collection<? extends DesignElementDataVector> vectors ) {
        try {
            this.handleRemove( vectors );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.remove(java.util.Collection vectors)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#remove(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)
     */
    @Override
    public void remove( final ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector designElementDataVector ) {
        try {
            this.handleRemove( designElementDataVector );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.remove(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector designElementDataVector)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#removeDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public void removeDataForCompositeSequence(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        try {
            this.handleRemoveDataForCompositeSequence( compositeSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.removeDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#removeDataForQuantitationType(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public void removeDataForQuantitationType(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            this.handleRemoveDataForQuantitationType( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.removeDataForQuantitationType(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
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
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#thaw(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)
     */
    @Override
    public void thaw( final DesignElementDataVector designElementDataVector ) {
        try {
            this.handleThaw( designElementDataVector );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.thaw(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector designElementDataVector)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final java.util.Collection<? extends DesignElementDataVector> designElementDataVectors ) {
        try {
            this.handleThaw( designElementDataVectors );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.thaw(java.util.Collection designElementDataVectors)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#update(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)
     */
    @Override
    public void update( final DesignElementDataVector dedv ) {
        try {
            this.handleUpdate( dedv );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.DesignElementDataVector.update(ubic.gemma.model.expression.bioAssayData.DesignElementDataVector dedv)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends DesignElementDataVector> dedvs ) {
        try {
            this.handleUpdate( dedvs );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorServiceException(
                    "Error performing 'ubic.gemma.model.expression.bioAssayData.RawExpressionDataVectorService.update(java.util.Collection dedvs)' --> "
                            + th, th );
        }
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
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(java.util.Collection)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleCreate(
            java.util.Collection<? extends DesignElementDataVector> vectors ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(java.util.Collection)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            java.util.Collection<QuantitationType> quantitationTypes ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #find(ubic.gemma.model.expression.arrayDesign.ArrayDesign, ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract java.util.Collection<? extends DesignElementDataVector> handleFind(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.bioAssayData.DesignElementDataVector handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)}
     */
    protected abstract void handleRemove( DesignElementDataVector designElementDataVector ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(java.util.Collection)}
     */
    protected abstract void handleRemove( java.util.Collection<? extends DesignElementDataVector> vectors )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #removeDataForCompositeSequence(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract void handleRemoveDataForCompositeSequence(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #removeDataForQuantitationType(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract void handleRemoveDataForQuantitationType(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)}
     */
    protected abstract void handleThaw( DesignElementDataVector designElementDataVector ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<? extends DesignElementDataVector> designElementDataVectors )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector)}
     */
    protected abstract void handleUpdate( DesignElementDataVector dedv ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(java.util.Collection)}
     */
    protected abstract void handleUpdate( java.util.Collection<? extends DesignElementDataVector> dedvs )
            throws java.lang.Exception;

}