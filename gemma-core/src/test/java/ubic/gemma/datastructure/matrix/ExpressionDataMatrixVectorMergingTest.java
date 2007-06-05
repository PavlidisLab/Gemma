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

import java.util.Collection;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.VectorMergingService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
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
public class ExpressionDataMatrixVectorMergingTest extends BaseSpringContextTest {

    ExpressionExperimentService expressionExperimentService;
    DesignElementDataVectorService designElementDataVectorService;
    protected AbstractGeoService geoService;

    /**
     * Used 4 related platforms. This is the Sorlie breast cancer data set (2001), altered to be simpler for us to
     * interpret test results.
     * <p>
     * This test has problems running in a transaction, it works best if the data gets loaded once and then reused each
     * time you run the test (the test isn't about loading the data).
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testMatrixConversionGSE3193() throws Exception {
        endTransaction();
        designElementDataVectorService = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );
        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );

        ExpressionExperiment newee = null;
        try {
            String path = ConfigUtils.getString( "gemma.home" );
            assert path != null;
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "GSE3193Short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                    "GSE3193trick", false, false, false ); // don't do sample matching!!
            newee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) e.getData();
        }

        assertNotNull( newee );
        expressionExperimentService.thaw( newee );

        Collection<DesignElementDataVector> designElementDataVectors = newee.getDesignElementDataVectors();
        designElementDataVectorService.thaw( designElementDataVectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( designElementDataVectors );
        ExpressionDataDoubleMatrix matrix = builder.getPreferredData();

        assertEquals( 17, matrix.columns() );
        assertEquals( 20, matrix.rows() );

        BioAssayDimension combinedBad = matrix.getBioAssayDimension();

        assertEquals( 17, combinedBad.getBioAssays().size() );

        VectorMergingService mergingService = ( VectorMergingService ) this.getBean( "vectorMergingService" );

        mergingService.mergeVectors( newee );

        expressionExperimentService.thaw( newee );

        assertEquals( 20, newee.getDesignElementDataVectors().size() );

    }

}
