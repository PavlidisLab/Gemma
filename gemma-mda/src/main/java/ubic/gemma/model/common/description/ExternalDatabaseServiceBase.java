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
package ubic.gemma.model.common.description;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.description.ExternalDatabaseService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.ExternalDatabaseService
 */
public abstract class ExternalDatabaseServiceBase   implements
        ubic.gemma.model.common.description.ExternalDatabaseService {

    @Autowired
    private ubic.gemma.model.common.description.ExternalDatabaseDao externalDatabaseDao;

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseService#find(java.lang.String)
     */
    @Override
    public ubic.gemma.model.common.description.ExternalDatabase find( final java.lang.String name ) {
        try {
            return this.handleFind( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.ExternalDatabaseServiceException(
                    "Error performing 'ubic.gemma.model.common.description.ExternalDatabaseService.find(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseService#findOrCreate(ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public ubic.gemma.model.common.description.ExternalDatabase findOrCreate(
            final ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        try {
            return this.handleFindOrCreate( externalDatabase );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.ExternalDatabaseServiceException(
                    "Error performing 'ubic.gemma.model.common.description.ExternalDatabaseService.findOrCreate(ubic.gemma.model.common.description.ExternalDatabase externalDatabase)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseService#loadAll()
     */
    @Override
    public java.util.Collection loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.ExternalDatabaseServiceException(
                    "Error performing 'ubic.gemma.model.common.description.ExternalDatabaseService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.ExternalDatabaseService#remove(ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    public void remove( final ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) {
        try {
            this.handleRemove( externalDatabase );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.ExternalDatabaseServiceException(
                    "Error performing 'ubic.gemma.model.common.description.ExternalDatabaseService.remove(ubic.gemma.model.common.description.ExternalDatabase externalDatabase)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>externalDatabase</code>'s DAO.
     */
    public void setExternalDatabaseDao( ubic.gemma.model.common.description.ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * Gets the reference to <code>externalDatabase</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.ExternalDatabaseDao getExternalDatabaseDao() {
        return this.externalDatabaseDao;
    }

    /**
     * Performs the core logic for {@link #find(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.description.ExternalDatabase handleFind( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.description.ExternalDatabase)}
     */
    protected abstract ubic.gemma.model.common.description.ExternalDatabase handleFindOrCreate(
            ubic.gemma.model.common.description.ExternalDatabase externalDatabase ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.description.ExternalDatabase)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.description.ExternalDatabase externalDatabase )
            throws java.lang.Exception;

}