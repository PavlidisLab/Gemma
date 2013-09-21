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

import ubic.gemma.model.expression.experiment.ExperimentalFactorDao;
import ubic.gemma.model.expression.experiment.FactorValueDao;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.expression.biomaterial.BioMaterialService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.expression.biomaterial.BioMaterialService
 */
public abstract class BioMaterialServiceBase implements ubic.gemma.model.expression.biomaterial.BioMaterialService {

    @Autowired
    protected ubic.gemma.model.expression.biomaterial.BioMaterialDao bioMaterialDao;

    @Autowired
    protected FactorValueDao factorValueDao;

    @Autowired
    protected ExperimentalFactorDao experimentalFactorDao;

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#copy(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional(readOnly = true)
    public BioMaterial copy( final BioMaterial bioMaterial ) {

        return this.handleCopy( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.lang.Integer countAll() {

        return this.handleCountAll();

    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#create(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional
    public ubic.gemma.model.expression.biomaterial.BioMaterial create(
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {

        return this.handleCreate( bioMaterial );

    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional
    public BioMaterial findOrCreate( final BioMaterial bioMaterial ) {

        return this.handleFindOrCreate( bioMaterial );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.expression.biomaterial.BioMaterial load( final java.lang.Long id ) {

        return this.handleLoad( id );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioMaterial> loadAll() {

        return this.handleLoadAll();
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#loadMultiple(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<BioMaterial> loadMultiple( final java.util.Collection<Long> ids ) {

        return this.handleLoadMultiple( ids );
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#remove(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    @Transactional
    public void remove( final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {

        this.handleRemove( bioMaterial );
    }

    /**
     * Sets the reference to <code>bioMaterial</code>'s DAO.
     */
    public void setBioMaterialDao( ubic.gemma.model.expression.biomaterial.BioMaterialDao bioMaterialDao ) {
        this.bioMaterialDao = bioMaterialDao;
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#update(ubic.gemma.model.expression.biomaterial.BioMaterial)
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
     * Performs the core logic for {@link #copy(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleCopy(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleCreate(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleFindOrCreate(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<BioMaterial> handleLoadAll();

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioMaterial> handleLoadMultiple( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

}