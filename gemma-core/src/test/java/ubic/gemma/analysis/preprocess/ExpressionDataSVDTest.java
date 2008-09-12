/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.RegressionTesting;
import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.TestExpressionDataDoubleMatrix;
import ubic.gemma.loader.expression.geo.DatasetCombiner;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.loader.expression.geo.GeoParseResult;
import ubic.gemma.loader.expression.geo.GeoSampleCorrespondence;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import junit.framework.TestCase;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionDataSVDTest extends TestCase {

    ExpressionDataDoubleMatrix testData = null;
    ExpressionDataSVD svd = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        testData = new TestExpressionDataDoubleMatrix();
        svd = new ExpressionDataSVD( testData, false );
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.ExpressionDataSVD#getS()}.
     */
    public void testGetS() {
        DoubleMatrix<Integer, Integer> s = svd.getS();
        assertNotNull( s );
        List<Integer> colNames = s.getColNames();
        for ( Integer integer : colNames ) {
            assertNotNull( integer );
        }
    }

    public void testUMatrixAsExpressionDataUnnormalized() throws Exception {
        try {
            svd.uMatrixAsExpressionData();
            fail( "Should have gotten an exception" );
        } catch ( IllegalStateException e ) {
            //
        }
    }

    public void testUMatrixAsExpressionData() throws Exception {
        svd = new ExpressionDataSVD( testData, true );
        ExpressionDataDoubleMatrix matrixAsExpressionData = svd.uMatrixAsExpressionData();
        assertNotNull( matrixAsExpressionData );
    }

    /**
     * Test on full-sized data set.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testMatrixReconstructB() throws Exception {
        GeoConverter gc = new GeoConverter();
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/fullSizeTests/GSE1623_family.soft.txt.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE1623" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) gc.convert( series );
        assertNotNull( result );
        assertEquals( 1, result.size() );
        ExpressionExperiment ee = result.iterator().next();

        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee.getRawExpressionDataVectors(), ee
                .getQuantitationTypes().iterator().next() );

        svd = new ExpressionDataSVD( matrix, false );

        ExpressionDataDoubleMatrix svdNormalize = svd.removeHighestComponents( 1 );
        assertNotNull( svdNormalize );
    }

    public void testWinnow() throws Exception {
        ExpressionDataDoubleMatrix winnow = svd.winnow( 0.5 );
        assertEquals( 100, winnow.rows() );
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.ExpressionDataSVD#getU()}.
     */
    public void testGetU() {
        DoubleMatrix<DesignElement, Integer> u = svd.getU();
        assertNotNull( u );
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.ExpressionDataSVD#svdNormalize()}.
     */
    public void testMatrixReconstruct() {
        ExpressionDataDoubleMatrix svdNormalize = svd.removeHighestComponents( 0 );
        assertNotNull( svdNormalize );
        RegressionTesting.closeEnough( testData.getMatrix(), svdNormalize.getMatrix(), 0.001 );
    }

}
