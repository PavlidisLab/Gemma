/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.common.description;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DatabaseEntryDaoImplTest extends BaseDAOTestCase {
    DatabaseEntryDao deDao;
    DatabaseEntry de;

    ExternalDatabaseDao exdbDao;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        deDao = ( DatabaseEntryDao ) ctx.getBean( "databaseEntryDao" );
        exdbDao = ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" );
        de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "bar" );
        ExternalDatabase ed = exdbDao.findByName( "PubMed" );
        assert ed != null;
        de.setExternalDatabase( ed );
        de = deDao.create( de );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        deDao.remove( de );
    }

    /*
     * Class under test for DatabaseEntry findOrCreate(DatabaseEntry)
     */
    public void testFindOrCreateDatabaseEntry() {
        deDao.remove( de );
        DatabaseEntry actualReturn = deDao.findOrCreate( de );
        assertEquals( de.getAccession(), actualReturn.getAccession() );
    }

    /*
     * Class under test for edu.columbia.gemma.common.description.DatabaseEntry
     * find(edu.columbia.gemma.common.description.DatabaseEntry)
     */
    public void testFindDatabaseEntry() {
        DatabaseEntry actualReturn = deDao.find( de );
        assertEquals( de, actualReturn );
    }

}
