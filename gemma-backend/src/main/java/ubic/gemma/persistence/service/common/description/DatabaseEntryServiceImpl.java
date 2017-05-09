/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * @author keshav
 * @version $Id$
 * @see DatabaseEntryService
 */
@Service
public class DatabaseEntryServiceImpl extends DatabaseEntryServiceBase {

    @Override
    protected Integer handleCountAll() {
        return this.getDatabaseEntryDao().countAll();
    }

    /**
     * @see DatabaseEntryService#createFromValueObject(ubic.gemma.model.common.description.DatabaseEntry)
     */
    protected DatabaseEntry handleCreate( DatabaseEntry databaseEntry ) {
        return this.getDatabaseEntryDao().create( databaseEntry );
    }

    /**
     * @see DatabaseEntryService#find(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    protected DatabaseEntry handleFind( DatabaseEntry databaseEntry ) {
        return this.getDatabaseEntryDao().find( databaseEntry );
    }

    /**
     * @see DatabaseEntryService#remove(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    protected void handleRemove( DatabaseEntry databaseEntry ) {
        this.getDatabaseEntryDao().remove( databaseEntry );
    }

    /**
     * @see DatabaseEntryService#update(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        this.getDatabaseEntryDao().update( databaseEntry );
    }

}