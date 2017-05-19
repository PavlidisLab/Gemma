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
package ubic.gemma.persistence.service.common.description;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * <p>
 * Spring Service base class for <code>ExternalDatabaseService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ExternalDatabaseService
 */
public abstract class ExternalDatabaseServiceBase implements ExternalDatabaseService {

    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;

    /**
     * @see ExternalDatabaseService#find(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.common.description.ExternalDatabase find( final java.lang.String name ) {
        return this.handleFind( name );

    }

    /**
     * @see ExternalDatabaseService#findOrCreate(ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    @Transactional
    public ExternalDatabase findOrCreate( final ExternalDatabase externalDatabase ) {
        return this.handleFindOrCreate( externalDatabase );

    }

    /**
     * @see ExternalDatabaseService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<ExternalDatabase> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * @see ExternalDatabaseService#remove(ubic.gemma.model.common.description.ExternalDatabase)
     */
    @Override
    @Transactional
    public void remove( final ExternalDatabase externalDatabase ) {
        this.handleRemove( externalDatabase );

    }

    /**
     * Sets the reference to <code>externalDatabase</code>'s DAO.
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * Gets the reference to <code>externalDatabase</code>'s DAO.
     */
    protected ExternalDatabaseDao getExternalDatabaseDao() {
        return this.externalDatabaseDao;
    }

    /**
     * Performs the core logic for {@link #find(java.lang.String)}
     */
    protected abstract ExternalDatabase handleFind( java.lang.String name );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.description.ExternalDatabase)}
     */
    protected abstract ExternalDatabase handleFindOrCreate( ExternalDatabase externalDatabase );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExternalDatabase> handleLoadAll();

    /**
     * Performs the core logic for {@link #remove(ExternalDatabase)}
     */
    protected abstract void handleRemove( ExternalDatabase externalDatabase );

}