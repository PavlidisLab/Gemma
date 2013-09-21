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
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * Spring Service base class for <code>BioAssayDimensionService</code>, provides access to all services and entities
 * referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService
 */
public abstract class BioAssayDimensionServiceBase implements BioAssayDimensionService {

    @Autowired
    private BioAssayDimensionDao bioAssayDimensionDao;

    /**
     * @see BioAssayDimensionService#create(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    @Transactional
    public BioAssayDimension create( final BioAssayDimension bioAssayDimension ) {
        return this.handleCreate( bioAssayDimension );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    @Transactional
    public BioAssayDimension findOrCreate( final BioAssayDimension bioAssayDimension ) {
        return this.handleFindOrCreate( bioAssayDimension );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#load(java.lang.Long)
     */
    @Transactional(readOnly = true)
    public BioAssayDimension load( final java.lang.Long id ) {
        return this.handleLoad( id );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#remove(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    @Transactional
    public void remove( final ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) {
        this.handleRemove( bioAssayDimension );
    }

    /**
     * Sets the reference to <code>bioAssayDimension</code>'s DAO.
     */
    public void setBioAssayDimensionDao( BioAssayDimensionDao bioAssayDimensionDao ) {
        this.bioAssayDimensionDao = bioAssayDimensionDao;
    }

    /**
     * @see BioAssayDimensionService#update(BioAssayDimension)
     */
    @Transactional
    public void update( final BioAssayDimension bioAssayDimension ) {
        this.handleUpdate( bioAssayDimension );
    }

    /**
     * Gets the reference to <code>bioAssayDimension</code>'s DAO.
     */
    protected BioAssayDimensionDao getBioAssayDimensionDao() {
        return this.bioAssayDimensionDao;
    }

    /**
     * Performs the core logic for {@link #create(BioAssayDimension)}
     */
    protected abstract BioAssayDimension handleCreate( BioAssayDimension bioAssayDimension );

    /**
     * Performs the core logic for {@link #findOrCreate(BioAssayDimension)}
     */
    protected abstract BioAssayDimension handleFindOrCreate( BioAssayDimension bioAssayDimension );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract BioAssayDimension handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #remove(BioAssayDimension)}
     */
    protected abstract void handleRemove( BioAssayDimension bioAssayDimension );

    /**
     * Performs the core logic for {@link #update(BioAssayDimension)}
     */
    protected abstract void handleUpdate( BioAssayDimension bioAssayDimension );

}