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
package ubic.gemma.externalDb;

import org.apache.commons.configuration.Configuration;
import org.hibernate.exception.JDBCConnectionException;

import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Tests connections to external databases.
 * 
 * @author keshav
 * @version $Id$
 * @see ExternalDatabase
 */
public class ExternalDatabaseTest extends BaseSpringContextTest {
    Configuration conf;
    GoldenPathHumanDao goldenPathHumanDao;

    /**
     * Get the bean name of the appropriate dao object. These dao objects will be replaced by the "table name + Dao" for
     * the database in question. For the moment, I have only set this up for the hg18 database as the other 3 will work
     * in the same manner.
     * 
     * @throws ConfigurationException
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        // ctx.getBean( conf.getString( "external.database.1" ) );
        // ctx.getBean( conf.getString( "external.database.2" ) )
        // ctx.getBean( conf.getString( "external.database.3" ) )
    }

    /**
     * Tests the database connection.
     * 
     * @throws HibernateException
     * @throws SQLException
     */
    public void testConnectToDatabase() throws Exception {
        try {
            boolean connectionIsClosed = goldenPathHumanDao.connectToDatabase();
            assertEquals( connectionIsClosed, false );
        } catch ( JDBCConnectionException e ) {
            log.warn( "Could not connect to Goldenpath DB, check configuration; skipping test" );
            return;
        }
    }

    /**
     * @param goldenPathHumanDao The goldenPathHumanDao to set.
     */
    public void setGoldenPathHumanDao( GoldenPathHumanDao goldenPathHumanDao ) {
        this.goldenPathHumanDao = goldenPathHumanDao;
    }

}