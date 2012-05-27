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
 * Spring Service base class for <code>ubic.gemma.model.common.description.DatabaseEntryService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.DatabaseEntryService
 */
public abstract class DatabaseEntryServiceBase implements ubic.gemma.model.common.description.DatabaseEntryService {

    @Autowired
    private ubic.gemma.model.common.description.DatabaseEntryDao databaseEntryDao;

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryService#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.DatabaseEntryServiceException(
                    "Error performing 'ubic.gemma.model.common.description.DatabaseEntryService.countAll()' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryService#find(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public ubic.gemma.model.common.description.DatabaseEntry find(
            final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        try {
            return this.handleFind( databaseEntry );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.DatabaseEntryServiceException(
                    "Error performing 'ubic.gemma.model.common.description.DatabaseEntryService.find(ubic.gemma.model.common.description.DatabaseEntry databaseEntry)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryService#remove(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public void remove( final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        try {
            this.handleRemove( databaseEntry );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.DatabaseEntryServiceException(
                    "Error performing 'ubic.gemma.model.common.description.DatabaseEntryService.remove(ubic.gemma.model.common.description.DatabaseEntry databaseEntry)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>databaseEntry</code>'s DAO.
     */
    public void setDatabaseEntryDao( ubic.gemma.model.common.description.DatabaseEntryDao databaseEntryDao ) {
        this.databaseEntryDao = databaseEntryDao;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntryService#update(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public void update( final ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        try {
            this.handleUpdate( databaseEntry );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.DatabaseEntryServiceException(
                    "Error performing 'ubic.gemma.model.common.description.DatabaseEntryService.update(ubic.gemma.model.common.description.DatabaseEntry databaseEntry)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>databaseEntry</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.DatabaseEntryDao getDatabaseEntryDao() {
        return this.databaseEntryDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.common.description.DatabaseEntry)}
     */
    protected abstract ubic.gemma.model.common.description.DatabaseEntry handleFind(
            ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.description.DatabaseEntry)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.description.DatabaseEntry databaseEntry )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.description.DatabaseEntry)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.description.DatabaseEntry databaseEntry )
            throws java.lang.Exception;

}