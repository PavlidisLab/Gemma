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
package ubic.gemma.model.expression.biomaterial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.expression.bioAssay.BioAssayDao;
import ubic.gemma.model.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.model.expression.experiment.FactorValueDao;

/**
 * Spring Service base class for <code>BioMaterialService</code>, provides access to all services and entities
 * referenced by this service.
 * 
 * @see BioMaterialService
 */
public abstract class BioMaterialServiceBase implements BioMaterialService {

    @Autowired
    protected BioMaterialDao bioMaterialDao;

    @Autowired
    protected FactorValueDao factorValueDao;

    @Autowired
    protected BioAssayDao bioAssayDao;

    @Autowired
    protected ExperimentalFactorDao experimentalFactorDao;

    /**
     * @see BioMaterialService#copy(BioMaterial)
     */
    @Override
    @Transactional(readOnly = true)
    public BioMaterial copy( final BioMaterial bioMaterial ) {

        return this.handleCopy( bioMaterial );
    }

    /**
     * @see BioMaterialService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {

        return this.handleCountAll();

    }

    /**
     * @see BioMaterialService#create(BioMaterial)
     */
    @Override
    @Transactional
    public BioMaterial create( final BioMaterial bioMaterial ) {

        return this.handleCreate( bioMaterial );

    }

    /**
     * @see BioMaterialService#findOrCreate(BioMaterial)
     */
    @Override
    @Transactional
    public BioMaterial findOrCreate( final BioMaterial bioMaterial ) {

        return this.handleFindOrCreate( bioMaterial );
    }

    /**
     * @see BioMaterialService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public BioMaterial load( final java.lang.Long id ) {

        return this.handleLoad( id );
    }

    /**
     * @see BioMaterialService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioMaterial> loadAll() {

        return this.handleLoadAll();
    }

    /**
     * @see BioMaterialService#loadMultiple(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioMaterial> loadMultiple( final java.util.Collection<Long> ids ) {

        return this.handleLoadMultiple( ids );
    }

    /**
     * @see BioMaterialService#remove(BioMaterial)
     */
    @Override
    @Transactional
    public void remove( final BioMaterial bioMaterial ) {

        this.handleRemove( bioMaterial );
    }

    /**
     * Sets the reference to <code>bioMaterial</code>'s DAO.
     */
    public void setBioMaterialDao( BioMaterialDao bioMaterialDao ) {
        this.bioMaterialDao = bioMaterialDao;
    }

    /**
     * @see BioMaterialService#update(BioMaterial)
     */
    @Override
    @Transactional
    public void update( final BioMaterial bioMaterial ) {
        this.handleUpdate( bioMaterial );

    }

    /**
     * Gets the reference to <code>bioMaterial</code>'s DAO.
     */
    protected BioMaterialDao getBioMaterialDao() {
        return this.bioMaterialDao;
    }

    /**
     * Performs the core logic for {@link #copy(BioMaterial)}
     */
    protected abstract BioMaterial handleCopy( BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(BioMaterial)}
     */
    protected abstract BioMaterial handleCreate( BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #findOrCreate(BioMaterial)}
     */
    protected abstract BioMaterial handleFindOrCreate( BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract BioMaterial handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<BioMaterial> handleLoadAll();

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioMaterial> handleLoadMultiple( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #remove(BioMaterial)}
     */
    protected abstract void handleRemove( BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #update(BioMaterial)}
     */
    protected abstract void handleUpdate( BioMaterial bioMaterial );

}