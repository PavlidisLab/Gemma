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
package ubic.gemma.model.common.quantitationtype;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.quantitationtype.QuantitationTypeService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService
 */
public abstract class QuantitationTypeServiceBase implements
        ubic.gemma.model.common.quantitationtype.QuantitationTypeService {

    @Autowired
    private ubic.gemma.model.common.quantitationtype.QuantitationTypeDao quantitationTypeDao;

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#create(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType create(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.handleCreate( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.create(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#find(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType find(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.handleFind( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.find(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#findOrCreate(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType findOrCreate(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.handleFindOrCreate( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.findOrCreate(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#load(java.lang.Long)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#loadAll()
     */
    public java.util.Collection loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#remove(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public void remove( final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            this.handleRemove( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.remove(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>quantitationType</code>'s DAO.
     */
    public void setQuantitationTypeDao( ubic.gemma.model.common.quantitationtype.QuantitationTypeDao quantitationTypeDao ) {
        this.quantitationTypeDao = quantitationTypeDao;
    }

    /**
     * @see ubic.gemma.model.common.quantitationtype.QuantitationTypeService#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public void update( final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            this.handleUpdate( quantitationType );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.quantitationtype.QuantitationTypeServiceException(
                    "Error performing 'ubic.gemma.model.common.quantitationtype.QuantitationTypeService.update(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>quantitationType</code>'s DAO.
     */
    protected ubic.gemma.model.common.quantitationtype.QuantitationTypeDao getQuantitationTypeDao() {
        return this.quantitationTypeDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract ubic.gemma.model.common.quantitationtype.QuantitationType handleCreate(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract ubic.gemma.model.common.quantitationtype.QuantitationType handleFind(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract ubic.gemma.model.common.quantitationtype.QuantitationType handleFindOrCreate(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.common.quantitationtype.QuantitationType handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType )
            throws java.lang.Exception;

}