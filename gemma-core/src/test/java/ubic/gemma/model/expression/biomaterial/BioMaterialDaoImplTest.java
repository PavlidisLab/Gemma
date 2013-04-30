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

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BioMaterialDaoImplTest extends BaseSpringContextTest {

    private String searchkeyName;
    private String searchkeyAcc;

    @Autowired
    BioMaterialDao bioMaterialDao;

    @Before
    public void setup() {
        log.info( "Starting setup" );
        BioMaterial testbm = this.getTestPersistentBioMaterial();
        searchkeyName = testbm.getName();
        searchkeyAcc = testbm.getExternalAccession().getAccession();

        // create a couple more.
        this.getTestPersistentBioMaterial();
        this.getTestPersistentBioMaterial();
        log.info( "Ending setup" );
    }

    /**
     * Test method for
     * {@link ubic.gemma.model.expression.biomaterial.BioMaterialDaoImpl#find(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     * .
     */
    @Test
    public final void testFindBioMaterial() {
        log.info( "Starting test" );
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( searchkeyName );
        bm.setExternalAccession( DatabaseEntry.Factory.newInstance() );
        bm.getExternalAccession().setAccession( searchkeyAcc );
        BioMaterial found = this.bioMaterialDao.find( bm );
        assertTrue( found != null );
    }

    @Test
    public final void testFindBioMaterialByAccessionOnly() {
        log.info( "Starting test" );
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setExternalAccession( DatabaseEntry.Factory.newInstance() );
        bm.getExternalAccession().setAccession( searchkeyAcc );
        BioMaterial found = this.bioMaterialDao.find( bm );
        assertTrue( found != null );
    }

    @Test
    public final void testFindBioMaterialByNameOnly() {
        log.info( "Starting test" );
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( searchkeyName );
        BioMaterial found = this.bioMaterialDao.find( bm );
        assertTrue( found != null );
    }

}
