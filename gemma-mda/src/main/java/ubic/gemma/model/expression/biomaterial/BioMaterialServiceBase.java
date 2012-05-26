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
    private ubic.gemma.model.expression.biomaterial.BioMaterialDao bioMaterialDao;

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#copy(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public ubic.gemma.model.expression.biomaterial.BioMaterial copy(
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        try {
            return this.handleCopy( bioMaterial );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.copy(ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.countAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#create(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public ubic.gemma.model.expression.biomaterial.BioMaterial create(
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        try {
            return this.handleCreate( bioMaterial );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.create(ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public ubic.gemma.model.expression.biomaterial.BioMaterial findOrCreate(
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        try {
            return this.handleFindOrCreate( bioMaterial );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.expression.biomaterial.BioMaterial load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#loadAll()
     */
    @Override
    public java.util.Collection<BioMaterial> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.loadAll()' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#loadMultiple(java.util.Collection)
     */
    @Override
    public java.util.Collection<BioMaterial> loadMultiple( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.expression.biomaterial.BioMaterialService#remove(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    public void remove( final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        try {
            this.handleRemove( bioMaterial );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.remove(ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
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
    public void update( final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        try {
            this.handleUpdate( bioMaterial );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.expression.biomaterial.BioMaterialServiceException(
                    "Error performing 'ubic.gemma.model.expression.biomaterial.BioMaterialService.update(ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>bioMaterial</code>'s DAO.
     */
    protected ubic.gemma.model.expression.biomaterial.BioMaterialDao getBioMaterialDao() {
        return this.bioMaterialDao;
    }

    /**
     * Performs the core logic for {@link #copy(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleCopy(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleCreate(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleFindOrCreate(
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.expression.biomaterial.BioMaterial handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<BioMaterial> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BioMaterial> handleLoadMultiple( java.util.Collection<Long> ids )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract void handleRemove( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial )
            throws java.lang.Exception;

}