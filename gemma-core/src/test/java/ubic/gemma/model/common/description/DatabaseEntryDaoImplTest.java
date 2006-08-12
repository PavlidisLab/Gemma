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

    DatabaseEntry de;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "bar" );
        ExternalDatabase ed = externalDatabaseDao.findByName( "PubMed" );
        assert ed != null;
        de.setExternalDatabase( ed );
        databaseEntryDao.create( de );
    }

    /*
     * Class under test for DatabaseEntry findOrCreate(DatabaseEntry)
     */
    public void testFindOrCreateDatabaseEntry() {
        databaseEntryDao.remove( de );
        DatabaseEntry actualReturn = databaseEntryDao.findOrCreate( de );
        assertEquals( de.getAccession(), actualReturn.getAccession() );
    }

    /*
     * Class under test for ubic.gemma.model.common.description.DatabaseEntry
     * find(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public void testFindDatabaseEntry() {
        DatabaseEntry actualReturn = databaseEntryDao.find( de );
        assertEquals( de, actualReturn );
    }

}
