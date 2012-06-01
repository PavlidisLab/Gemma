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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService
 */
public abstract class BioAssayDimensionServiceBase implements
        ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService {

    @Autowired
    private ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao bioAssayDimensionDao;

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#create(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public ubic.gemma.model.expression.bioAssayData.BioAssayDimension create(
            final ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        return this.handleCreate( bioAssayDimension );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public ubic.gemma.model.expression.bioAssayData.BioAssayDimension findOrCreate(
            final ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        return this.handleFindOrCreate( bioAssayDimension );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#load(java.lang.Long)
     */
    public ubic.gemma.model.expression.bioAssayData.BioAssayDimension load( final java.lang.Long id ) {
        return this.handleLoad( id );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#remove(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    public void remove( final ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        this.handleRemove( bioAssayDimension );
    }

    /**
     * Sets the reference to <code>bioAssayDimension</code>'s DAO.
     */
    public void setBioAssayDimensionDao(
            ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao bioAssayDimensionDao ) {
        this.bioAssayDimensionDao = bioAssayDimensionDao;
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#update(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    public void update( final ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        this.handleUpdate( bioAssayDimension );
    }

    /**
     * Gets the reference to <code>bioAssayDimension</code>'s DAO.
     */
    protected ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao getBioAssayDimensionDao() {
        return this.bioAssayDimensionDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)}
     */
    protected abstract ubic.gemma.model.expression.bioAssayData.BioAssayDimension handleCreate(
            ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)}
     */
    protected abstract ubic.gemma.model.expression.bioAssayData.BioAssayDimension handleFindOrCreate(
            ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.bioAssayData.BioAssayDimension handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension );

}