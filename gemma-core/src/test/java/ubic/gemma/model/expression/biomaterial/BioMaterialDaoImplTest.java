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
package ubic.gemma.model.expression.biomaterial;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BioMaterialDaoImplTest extends BaseTransactionalSpringContextTest {

    private String searchkeyName;
    private String searchkeyAcc;
    BioMaterialDao bioMaterialDao;

    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        bioMaterialDao = ( BioMaterialDao ) getBean( "bioMaterialDao" );
        BioMaterial testbm = this.getTestPersistentBioMaterial();
        searchkeyName = testbm.getName();
        searchkeyAcc = testbm.getExternalAccession().getAccession();

        // create a couple more.
        this.getTestPersistentBioMaterial();
        this.getTestPersistentBioMaterial();
    }

    /**
     * Test method for
     * {@link ubic.gemma.model.expression.biomaterial.BioMaterialDaoImpl#find(ubic.gemma.model.expression.biomaterial.BioMaterial)}.
     */
    public final void testFindBioMaterial() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( searchkeyName );
        bm.setExternalAccession( DatabaseEntry.Factory.newInstance() );
        bm.getExternalAccession().setAccession( searchkeyAcc );
        BioMaterial found = this.bioMaterialDao.find( bm );
        assertTrue( found != null );
    }

    public final void testFindBioMaterialByAccessionOnly() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setExternalAccession( DatabaseEntry.Factory.newInstance() );
        bm.getExternalAccession().setAccession( searchkeyAcc );
        BioMaterial found = this.bioMaterialDao.find( bm );
        assertTrue( found != null );
    }

    public final void testFindBioMaterialByNameOnly() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( searchkeyName );
        BioMaterial found = this.bioMaterialDao.find( bm );
        assertTrue( found != null );
    }

    /**
     * @throws Exception
     */
    public void testSetExternalAccession() throws Exception {

        /* set to avoid using stale data (data from previous tests */
        // setFlushModeCommit();
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( "Test Biomaterial" );

        ExternalDatabaseDao edd = ( ExternalDatabaseDao ) this.getBean( "externalDatabaseDao" );
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "Test Database Entry" );
        ed = edd.findOrCreate( ed );

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "Test Biomaterial Accession" );
        de.setExternalDatabase( ed );

        /* set taxon  for the test to pass*/  
 
        bm.setSourceTaxon( this.getTestPersistentTaxon() );
        /* testing this method */
        bm.setExternalAccession( de );

        BioMaterialDao bmDao = ( BioMaterialDao ) this.getBean( "bioMaterialDao" );

        // bmDao.findOrCreate( bm ); - FIXME use this
        
        bm = ( BioMaterial ) bmDao.create( bm );

        assertTrue( bm.getId() != null );

        // setComplete();
    }

}
