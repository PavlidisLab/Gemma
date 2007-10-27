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
package ubic.gemma.model.expression.bioAssayData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BioAssayDimensionDaoImplTest extends BaseSpringContextTest {

    private static final int NUMTESTCOMPOSITESEQUENCES = 15;

    private static final int NUMTESTBIOASSAYS = 5;

    protected DesignElementDataVectorDao designElementDataVectorDao;

    protected BioAssayDimensionDao bioAssayDimensionDao;

    BioAssayDimension bad;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();
        List<BioAssay> bas = new ArrayList<BioAssay>();
        ExpressionExperiment ee = this.getTestPersistentExpressionExperiment();
        bad = ( BioAssayDimension ) bioAssayDimensionDao.create( BioAssayDimension.Factory.newInstance() );
        ArrayDesign ad = this.getTestPersistentArrayDesign( NUMTESTCOMPOSITESEQUENCES, true );
        for ( int i = 0; i < NUMTESTBIOASSAYS; i++ ) {
            BioAssay ba = this.getTestPersistentBioAssay( ad );
            bas.add( ba );
        }

        bad.setBioAssays( bas );
        bioAssayDimensionDao.update( bad );

        QuantitationType qt = this.getTestPersistentQuantitationType();

        for ( DesignElement de : ad.getCompositeSequences() ) {
            assert de.getId() != null;
            DesignElementDataVector dedv = DesignElementDataVector.Factory.newInstance();
            dedv.setDesignElement( de );
            dedv.setExpressionExperiment( ee );
            dedv.setQuantitationType( qt );
            dedv.setBioAssayDimension( bad );
            dedv.setData( new byte[] {} );
            dedv = ( DesignElementDataVector ) designElementDataVectorDao.create( dedv );
        }

    }

    // public void testFindBioAssayDimension() {
    // fail( "Not yet implemented" );
    // }
    //
    // public void testFindOrCreateBioAssayDimension() {
    // fail( "Not yet implemented" );
    // }

    /**
     * Test for HQL query.
     */
    public void testFindDesignElementDataVectorsLong() {
        assertTrue( bad.getId() != null );
        Collection result = bioAssayDimensionDao.findDesignElementDataVectors( bad.getId() );
        assertEquals( NUMTESTCOMPOSITESEQUENCES, result.size() );
    }

    public void setBioAssayDimensionDao( BioAssayDimensionDao bioAssayDimensionDao ) {
        this.bioAssayDimensionDao = bioAssayDimensionDao;
    }

    public void setDesignElementDataVectorDao( DesignElementDataVectorDao designElementDataVectorDao ) {
        this.designElementDataVectorDao = designElementDataVectorDao;
    }

}
