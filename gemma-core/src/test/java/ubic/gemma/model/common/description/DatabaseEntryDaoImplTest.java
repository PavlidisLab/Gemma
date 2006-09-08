/*
 * The Gemma project
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
package ubic.gemma.model.common.description;

import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class DatabaseEntryDaoImplTest extends BaseTransactionalSpringContextTest {

    DatabaseEntryDao databaseEntryDao;

    /**
     * @param databaseEntryDao the databaseEntryDao to set
     */
    public void setDatabaseEntryDao( DatabaseEntryDao databaseEntryDao ) {
        this.databaseEntryDao = databaseEntryDao;
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
    }

    /*
     * Class under test for DatabaseEntry findOrCreate(DatabaseEntry)
     */
    public void testFindOrCreateDatabaseEntry() {
        DatabaseEntry de = this.getTestPersistentDatabaseEntry();
        databaseEntryDao.remove( de );
        DatabaseEntry actualReturn = databaseEntryDao.findOrCreate( de );
        assertEquals( de.getAccession(), actualReturn.getAccession() );
    }

    /*
     * Class under test for ubic.gemma.model.common.description.DatabaseEntry
     * find(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public void testFindDatabaseEntry() {
        DatabaseEntry de = this.getTestPersistentDatabaseEntry();
        DatabaseEntry actualReturn = databaseEntryDao.find( de );
        assertEquals( de, actualReturn );
    }

}
