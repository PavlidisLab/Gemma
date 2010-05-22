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
package ubic.gemma.model.expression.bioAssay;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

/**
 * Spring Service base class for <code>ubic.gemma.model.expression.bioAssay.BioAssayService</code>, provides access to
 * all services and entities referenced by this service.
 * 
 * @see ubic.gemma.model.expression.bioAssay.BioAssayService
 * @version $Id$
 */
public abstract class BioAssayServiceBase implements ubic.gemma.model.expression.bioAssay.BioAssayService {

    @Autowired
    private ubic.gemma.model.expression.biomaterial.BioMaterialDao bioMaterialDao;

    @Autowired
    private ubic.gemma.model.expression.bioAssay.BioAssayDao bioAssayDao;

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#addBioMaterialAssociation(ubic.gemma.model.expression.bioAssay.BioAssay,
     *      ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public void addBioMaterialAssociation( final BioAssay bioAssay,
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        try {
            this.handleAddBioMaterialAssociation( bioAssay, bioMaterial );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException(
                    "Error performing 'BioAssayService.addBioMaterialAssociation(BioAssay bioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * @see BioAssayService#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new BioAssayServiceException( "Error performing 'BioAssayService.countAll()' --> " + th, th );
        }
    }

    /**
     * @see BioAssayService#findBioAssayDimensions(BioAssay)
     */
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions( final BioAssay bioAssay ) {
        try {
            return this.handleFindBioAssayDimensions( bioAssay );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException(
                    "Error performing 'BioAssayService.findBioAssayDimensions(BioAssay bioAssay)' --> " + th, th );
        }
    }

    /**
     * @see BioAssayService#findOrCreate(BioAssay)
     */
    public BioAssay findOrCreate( final BioAssay bioAssay ) {
        try {
            return this.handleFindOrCreate( bioAssay );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException(
                    "Error performing 'BioAssayService.findOrCreate(BioAssay bioAssay)' --> " + th, th );
        }
    }

    /**
     * @see BioAssayService#load(java.lang.Long)
     */
    public BioAssay load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException( "Error performing 'BioAssayService.load(java.lang.Long id)' --> " + th,
                    th );
        }
    }

    /**
     * @see BioAssayService#loadAll()
     */
    public java.util.Collection<BioAssay> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new BioAssayServiceException( "Error performing 'BioAssayService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see BioAssayService#remove(BioAssay)
     */
    public void remove( final BioAssay bioAssay ) {
        try {
            this.handleRemove( bioAssay );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException( "Error performing 'BioAssayService.remove(BioAssay bioAssay)' --> "
                    + th, th );
        }
    }

    /**
     * @see BioAssayService#removeBioMaterialAssociation(BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public void removeBioMaterialAssociation( final BioAssay bioAssay,
            final ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) {
        try {
            this.handleRemoveBioMaterialAssociation( bioAssay, bioMaterial );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException(
                    "Error performing 'BioAssayService.removeBioMaterialAssociation(BioAssay bioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>bioAssay</code>'s DAO.
     */
    public void setBioAssayDao( BioAssayDao bioAssayDao ) {
        this.bioAssayDao = bioAssayDao;
    }

    /**
     * Sets the reference to <code>bioMaterialService</code>.
     */
    public void setBioMaterialDao( ubic.gemma.model.expression.biomaterial.BioMaterialDao bioMaterialDao ) {
        this.bioMaterialDao = bioMaterialDao;
    }

    /**
     * @see BioAssayService#thaw(BioAssay)
     */
    public void thaw( final BioAssay bioAssay ) {
        try {
            this.handleThaw( bioAssay );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException( "Error performing 'BioAssayService.thaw(BioAssay bioAssay)' --> " + th,
                    th );
        }
    }

    /**
     * @see BioAssayService#update(BioAssay)
     */
    public void update( final BioAssay bioAssay ) {
        try {
            this.handleUpdate( bioAssay );
        } catch ( Throwable th ) {
            throw new BioAssayServiceException( "Error performing 'BioAssayService.update(BioAssay bioAssay)' --> "
                    + th, th );
        }
    }

    /**
     * Gets the reference to <code>bioAssay</code>'s DAO.
     */
    protected BioAssayDao getBioAssayDao() {
        return this.bioAssayDao;
    }

    /**
     * Gets the reference to <code>bioMaterialDao</code>.
     */
    protected ubic.gemma.model.expression.biomaterial.BioMaterialDao getBioMaterialDao() {
        return this.bioMaterialDao;
    }

    /**
     * Performs the core logic for
     * {@link #addBioMaterialAssociation(BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract void handleAddBioMaterialAssociation( BioAssay bioAssay,
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findBioAssayDimensions(BioAssay)}
     */
    protected abstract java.util.Collection<BioAssayDimension> handleFindBioAssayDimensions( BioAssay bioAssay )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(BioAssay)}
     */
    protected abstract BioAssay handleFindOrCreate( BioAssay bioAssay ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract BioAssay handleLoad( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<BioAssay> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(BioAssay)}
     */
    protected abstract void handleRemove( BioAssay bioAssay ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #removeBioMaterialAssociation(BioAssay, ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract void handleRemoveBioMaterialAssociation( BioAssay bioAssay,
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(BioAssay)}
     */
    protected abstract void handleThaw( BioAssay bioAssay ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(BioAssay)}
     */
    protected abstract void handleUpdate( BioAssay bioAssay ) throws java.lang.Exception;

}