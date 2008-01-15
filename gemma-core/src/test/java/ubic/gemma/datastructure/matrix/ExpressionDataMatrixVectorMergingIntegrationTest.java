/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.datastructure.matrix;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.VectorMergingService;
import ubic.gemma.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionDataMatrixVectorMergingIntegrationTest extends BaseSpringContextTest {

    ExpressionExperimentService expressionExperimentService;
    DesignElementDataVectorService designElementDataVectorService;
    protected AbstractGeoService geoService;
    ExpressionExperiment newee = null;

    @Override
    @SuppressWarnings("unchecked")
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();
        endTransaction();
        designElementDataVectorService = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );
        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );

        String path = ConfigUtils.getString( "gemma.home" );
        assert path != null;
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "GSE3193Short" ) );
        Collection<ExpressionExperiment> results;
        try {

            results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE3193trick", false, false, false ); // don't do sample matching!!

        } catch ( AlreadyExistsInSystemException e ) {
            expressionExperimentService.delete( ( ExpressionExperiment ) e.getData() );

            results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE3193trick", false, false, false ); // don't do sample matching!!

        }
        newee = results.iterator().next();
        assertNotNull( newee );
        expressionExperimentService.thaw( newee );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        if ( newee != null ) expressionExperimentService.delete( newee );
        super.onTearDownInTransaction();

    }

    /**
     * Note that this test is incomplete and will fail. We need to merge the platforms before combining the vectors.
     * <p>
     * Used 4 related platforms. This is the Sorlie breast cancer data set (2001), mini, altered to be simpler for us to
     * interpret test results.
     * 
     * @throws Exception
     */

    public void testMatrixConversionGSE3193() throws Exception {

        /*
         * Show the expression expermient is in the old state
         */
        Collection<DesignElementDataVector> designElementDataVectors = newee.getDesignElementDataVectors();
        assertEquals( 100, newee.getDesignElementDataVectors().size() );

        designElementDataVectorService.thaw( designElementDataVectors );
        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( designElementDataVectors );
        ExpressionDataDoubleMatrix matrix = builder.getPreferredData();
        assertEquals( 17, matrix.columns() );
        assertEquals( 60, matrix.rows() );
        BioAssayDimension combinedBad = matrix.getBioAssayDimension();
        assertEquals( 17, combinedBad.getBioAssays().size() );

        Collection<ArrayDesign> otherArrayDesigns = new ArrayList<ArrayDesign>();
        ArrayDesign start = null;
        for ( BioAssay ba : newee.getBioAssays() ) {
            ArrayDesign ad = ba.getArrayDesignUsed();
            otherArrayDesigns.add( ad );
            start = ad;
        }

        otherArrayDesigns.remove( start );

        ArrayDesignMergeService adms = ( ArrayDesignMergeService ) this.getBean( "arrayDesignMergeService" );
        adms.merge( start, otherArrayDesigns, "testOnSorlie", "tsor" );

        ExpressionExperimentPlatformSwitchService eepss = ( ExpressionExperimentPlatformSwitchService ) this
                .getBean( "expressionExperimentPlatformSwitchService" );

        eepss.switchExperimentToMergedPlatform( newee );

        VectorMergingService mergingService = ( VectorMergingService ) this.getBean( "vectorMergingService" );

        mergingService.mergeVectors( newee );

        expressionExperimentService.thaw( newee );

        assertEquals( 600, newee.getDesignElementDataVectors().size() ); // multiple quantitation types.

    }

}
