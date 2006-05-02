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
package ubic.gemma.model.expression.experiment.biomaterial;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialDao;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author Kiran Keshav $Id$
 */
public class BioMaterialDaoImplTest extends BaseTransactionalSpringContextTest {

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

    }

    /**
     * @throws Exception
     */
    public void testSetExternalAccession() throws Exception {

        /* set to avoid using stale data (data from previous tests */
        setFlushModeCommit();

        /* uncomment to use prod environment as opposed to the test environment */
        // this.setDisableTestEnv( true );
        onSetUpInTransaction();

        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( "Test Biomaterial" );

        ExternalDatabaseDao edd = ( ExternalDatabaseDao ) this.getBean( "externalDatabaseDao" );
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "Test Database Entry" );
        ed = edd.findOrCreate( ed );

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "Test Biomaterial Accession" );
        de.setExternalDatabase( ed );

        /* testing this method */
        bm.setExternalAccession( de );

        BioMaterialDao bmDao = ( BioMaterialDao ) this.getBean( "bioMaterialDao" );
        // bmDao.findOrCreate( bm ); - FIXME use this
        bmDao.create( bm );

        // setComplete();
    }
}
