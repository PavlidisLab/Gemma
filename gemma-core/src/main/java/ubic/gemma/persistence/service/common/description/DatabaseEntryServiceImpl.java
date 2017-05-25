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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * Spring Service base class for <code>DatabaseEntryService</code>, provides access to all services and entities
 * referenced by this service.
 *
 * @see DatabaseEntryService
 */
@Service
public class DatabaseEntryServiceImpl implements DatabaseEntryService {

    private DatabaseEntryDao databaseEntryDao;

    /* ********************************
     * Constructors
     * ********************************/

    public DatabaseEntryServiceImpl() {
    }

    @Autowired
    public DatabaseEntryServiceImpl( DatabaseEntryDao databaseEntryDao ) {
        this.databaseEntryDao = databaseEntryDao;
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    @Transactional(readOnly = true)
    public Integer countAll() {
        return this.databaseEntryDao.countAll();
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseEntry find( final DatabaseEntry databaseEntry ) {
        return this.databaseEntryDao.find( databaseEntry );
    }

    @Override
    @Transactional
    public void remove( final DatabaseEntry databaseEntry ) {
        this.databaseEntryDao.remove( databaseEntry );
    }

    @Override
    @Transactional
    public void update( final DatabaseEntry databaseEntry ) {
        this.databaseEntryDao.update( databaseEntry );
    }

    @Override
    @Transactional
    public DatabaseEntry load( Long id ) {
        return this.databaseEntryDao.load( id );
    }

    @Override
    @Transactional
    public DatabaseEntry load( String accession ) {
        return this.databaseEntryDao.findByAccession( accession );
    }

}