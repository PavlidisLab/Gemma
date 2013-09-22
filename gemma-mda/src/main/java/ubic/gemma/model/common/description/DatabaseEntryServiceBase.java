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
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Service base class for <code>DatabaseEntryService</code>, provides access to all services and entities
 * referenced by this service.
 * 
 * @see DatabaseEntryService
 */
public abstract class DatabaseEntryServiceBase implements DatabaseEntryService {

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    /**
     * @see DatabaseEntryService#countAll()
     */
    @Override
    @Transactional(readOnly = true)
    public Integer countAll() {
        return this.handleCountAll();
    }

    /**
     * @see DatabaseEntryService#find(DatabaseEntry)
     */
    @Override
    @Transactional(readOnly = true)
    public DatabaseEntry find( final DatabaseEntry databaseEntry ) {
        return this.handleFind( databaseEntry );
    }

    /**
     * @see DatabaseEntryService#remove(DatabaseEntry)
     */
    @Override
    @Transactional
    public void remove( final DatabaseEntry databaseEntry ) {
        this.handleRemove( databaseEntry );
    }

    /**
     * Sets the reference to <code>databaseEntry</code>'s DAO.
     */
    public void setDatabaseEntryDao( DatabaseEntryDao databaseEntryDao ) {
        this.databaseEntryDao = databaseEntryDao;
    }

    /**
     * @see DatabaseEntryService#update(DatabaseEntry)
     */
    @Override
    @Transactional
    public void update( final DatabaseEntry databaseEntry ) {
        this.handleUpdate( databaseEntry );

    }

    /**
     * Gets the reference to <code>databaseEntry</code>'s DAO.
     */
    protected DatabaseEntryDao getDatabaseEntryDao() {
        return this.databaseEntryDao;
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract Integer handleCountAll();

    /**
     * Performs the core logic for {@link #find(DatabaseEntry)}
     */
    protected abstract DatabaseEntry handleFind( DatabaseEntry databaseEntry );

    /**
     * Performs the core logic for {@link #remove(DatabaseEntry)}
     */
    protected abstract void handleRemove( DatabaseEntry databaseEntry );

    /**
     * Performs the core logic for {@link #update(DatabaseEntry)}
     */
    protected abstract void handleUpdate( DatabaseEntry databaseEntry );

}