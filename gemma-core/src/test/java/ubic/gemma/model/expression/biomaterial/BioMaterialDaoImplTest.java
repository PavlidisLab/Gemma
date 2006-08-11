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
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BioMaterialDaoImplTest extends BaseTransactionalSpringContextTest {

    private String searchkeyName;
    private String searchkeyAcc;

    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
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

}
