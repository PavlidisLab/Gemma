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
package ubic.gemma.model.expression.bioAssay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BioAssayDaoImplTest extends BaseSpringContextTest {

    private static final int NUMTESTBIOASSAYS = 5;

    protected BioAssayDimensionDao bioAssayDimensionDao;

    private static BioAssay ba;

    private BioAssayDao bioAssayDao;

    static boolean setupDone = false;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();
        this.bioAssayDao = ( BioAssayDao ) getBean( "bioAssayDao" );
        List<BioAssay> bas = new ArrayList<BioAssay>();
        BioAssayDimension bad = bioAssayDimensionDao.create( BioAssayDimension.Factory.newInstance() );

        if ( !setupDone ) {
            ArrayDesign a = this.getTestPersistentArrayDesign( 5, true, false, true ); // readonly

            for ( int i = 0; i < NUMTESTBIOASSAYS; i++ ) {
                ba = this.getTestPersistentBioAssay( a );
                bas.add( ba );
            }

            bad.setBioAssays( bas );
            bioAssayDimensionDao.update( bad );
        }
    }

    /**
     * Tests HQL
     */
    public void testFindBioAssayDimensionsLong() {
        assertTrue( ba.getId() != null );
        Collection<BioAssayDimension> result = bioAssayDao.findBioAssayDimensions( ba );
        assertEquals( 1, result.size() );
    }

    public void testGetCount() {
        Integer count = bioAssayDao.countAll();
        assertNotNull( count );
        assertTrue( count > 0 );
    }

    public void setBioAssayDimensionDao( BioAssayDimensionDao bioAssayDimensionDao ) {
        this.bioAssayDimensionDao = bioAssayDimensionDao;
    }

}
