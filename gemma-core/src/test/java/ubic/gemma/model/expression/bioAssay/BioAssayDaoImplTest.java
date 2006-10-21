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

    protected DesignElementDataVectorDao designElementDataVectorDao;

    protected BioAssayDimensionDao bioAssayDimensionDao;

    private BioAssay ba;

    private BioAssayDao bioAssayDao;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        this.bioAssayDao = ( BioAssayDao ) getBean( "bioAssayDao" );
        List<BioAssay> bas = new ArrayList<BioAssay>();
        // ExpressionExperiment ee = this.getTestPersistentExpressionExperiment();
        BioAssayDimension bad = ( BioAssayDimension ) bioAssayDimensionDao.create( BioAssayDimension.Factory
                .newInstance() );

        ArrayDesign a = this.getTestPersistentArrayDesign( 10, true );

        for ( int i = 0; i < NUMTESTBIOASSAYS; i++ ) {
            ba = this.getTestPersistentBioAssay( a );
            bas.add( ba );
        }

        bad.setBioAssays( bas );
        bioAssayDimensionDao.update( bad );

    }

    /**
     * Tests HQL
     */
    public void testFindBioAssayDimensionsLong() {
        assertTrue( ba.getId() != null );
        Collection result = bioAssayDao.findBioAssayDimensions( ba );
        assertEquals( 1, result.size() );
    }

    public void setBioAssayDimensionDao( BioAssayDimensionDao bioAssayDimensionDao ) {
        this.bioAssayDimensionDao = bioAssayDimensionDao;
    }

    public void setDesignElementDataVectorDao( DesignElementDataVectorDao designElementDataVectorDao ) {
        this.designElementDataVectorDao = designElementDataVectorDao;
    }

}
