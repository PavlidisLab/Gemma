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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.service.common.description.DatabaseEntryDao;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pavlidis
 */
public class DatabaseEntryDaoImplTest extends BaseSpringContextTest {

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    @Test
    @Transactional
    public void testCreateDatabaseEntry() {
        DatabaseEntry de = this.getTestPersistentDatabaseEntry();
        databaseEntryDao.create( de );
        assertNotNull( de.getId() );
    }

    @Test
    @Transactional
    public void testFindDatabaseEntry() {
        DatabaseEntry de = this.getTestPersistentDatabaseEntry();
        databaseEntryDao.create( de );
        DatabaseEntry actualReturn = databaseEntryDao.find( de );
        assertEquals( de, actualReturn );
    }

    @Test
    @Transactional
    public void testLoadWithEmptyCollection() {
        assertEquals( Collections.emptyList(), databaseEntryDao.load( Collections.emptySet() ) );
    }

}
