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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.junit.Before;
import org.junit.Test;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.RegressionTesting;
import ubic.gemma.analysis.preprocess.svd.ExpressionDataSVD;
import ubic.gemma.datastructure.matrix.ExpressioDataTestMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.loader.expression.geo.DatasetCombiner;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.loader.expression.geo.GeoParseResult;
import ubic.gemma.loader.expression.geo.GeoSampleCorrespondence;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionDataSVDTest {

    ExpressionDataDoubleMatrix testData = null;
    ExpressionDataSVD svd = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {

        testData = new ExpressioDataTestMatrix();
        svd = new ExpressionDataSVD( testData, false );

    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.svd.ExpressionDataSVD#getS()}.
     */
    @Test
    public void testGetS() {
        DoubleMatrix<Integer, Integer> s = svd.getS();
        assertNotNull( s );
        List<Integer> colNames = s.getColNames();
        for ( Integer integer : colNames ) {
            assertNotNull( integer );
        }
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.svd.ExpressionDataSVD#getU()}.
     */
    @Test
    public void testGetU() {
        DoubleMatrix<CompositeSequence, Integer> u = svd.getU();
        assertNotNull( u );
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.svd.ExpressionDataSVD#svdNormalize()}.
     */
    @Test
    public void testMatrixReconstruct() {
        ExpressionDataDoubleMatrix svdNormalize = svd.removeHighestComponents( 0 );
        assertNotNull( svdNormalize );
        RegressionTesting.closeEnough( testData.getMatrix(), svdNormalize.getMatrix(), 0.001 );
    }

    /**
     * <pre>
     * testdata<-read.table("C:/users/paul/dev/eclipseworkspace/Gemma/gemma-core/src/test/resources/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort", 
     *          header=T, row.names=1)
     * testdata.s <- t(scale(t(testdata)))
     * testdata.s <- t(scale(t(testdata)))
     * for(i in 1:5) {
     *   testdata.s <-  t( scale(t(scale(testdata.s))));  
     * }
     * s<-svd(testdata.s)
     * s$d
     * # eigens:
     * s$d^2/ (nrow(testdata.s) - 1)
     * # or
     * p<-prcomp(testdata.s, center=F, scale=F)
     * p$sdev^2
     * # or
     * eigen(cov(testdata.s), only.values = TRUE)
     * </pre>
     */
    @Test
    public void testEigenvalues() {
        svd = new ExpressionDataSVD( testData, true );

        double[] singularValues = svd.getSingularValues();

        double[] actualSingularValues = new double[] { 4.582846e+01, 4.321314e+01, 3.798573e+01, 3.118461e+01,
                2.588194e+01, 2.361015e+01, 2.137710e+01, 2.002888e+01, 1.878038e+01, 1.730959e+01, 1.667858e+01,
                1.449183e+01, 1.422045e+01, 1.355147e+01, 1.217966e+01, 1.115845e+01, 1.089228e+01, 1.025242e+01,
                9.714624e+00, 9.414357e+00, 8.517218e+00, 8.273277e+00, 7.677081e+00, 7.508930e+00, 7.173066e+00,
                6.771654e+00, 6.562776e+00, 6.294326e+00, 6.245939e+00, 5.839531e+00, 5.678269e+00, 5.628377e+00,
                5.451532e+00, 5.186600e+00, 4.949012e+00, 4.775920e+00, 4.707305e+00, 4.550837e+00, 4.284304e+00,
                4.216276e+00, 4.113958e+00, 3.970669e+00, 3.710419e+00, 3.670523e+00, 3.470229e+00, 3.341110e+00,
                3.263424e+00, 3.258202e+00, 3.087673e+00, 2.968599e+00, 2.854167e+00, 2.804421e+00, 2.542775e+00,
                2.476547e+00, 2.331988e+00, 2.110575e+00, 2.064411e+00, 1.939713e+00, 2.635306e-15 };

        assertEquals( 59, singularValues.length );

        assertTrue( RegressionTesting.closeEnough( actualSingularValues, singularValues, 0.02 ) );

        double[] eigenvalues = svd.getEigenvalues();

        double[] actualEigenValues = new double[] { 1.055401e+01, 9.383797e+00, 7.250834e+00, 4.886834e+00,
                3.366206e+00, 2.801201e+00, 2.296385e+00, 2.015859e+00, 1.772374e+00, 1.505638e+00, 1.397865e+00,
                1.055343e+00, 1.016187e+00, 9.228257e-01, 7.454481e-01, 6.256833e-01, 5.961893e-01, 5.282020e-01,
                4.742408e-01, 4.453775e-01, 3.645377e-01, 3.439553e-01, 2.961687e-01, 2.833368e-01, 2.585572e-01,
                2.304286e-01, 2.164323e-01, 1.990882e-01, 1.960390e-01, 1.713574e-01, 1.620238e-01, 1.591891e-01,
                1.493427e-01, 1.351800e-01, 1.230790e-01, 1.146202e-01, 1.113504e-01, 1.040709e-01, 9.223749e-02,
                8.933157e-02, 8.504851e-02, 7.922720e-02, 6.918194e-02, 6.770221e-02, 6.051502e-02, 5.609556e-02,
                5.351728e-02, 5.334612e-02, 4.790815e-02, 4.428433e-02, 4.093601e-02, 3.952148e-02, 3.249097e-02,
                3.082052e-02, 2.732748e-02, 2.238455e-02, 2.141604e-02, 1.890698e-02, 3.489867e-32 };
        assertEquals( 59, eigenvalues.length );
        assertTrue( RegressionTesting.closeEnough( actualEigenValues, eigenvalues, 0.01 ) );
    }

    /**
     * See testEigenvalues
     * 
     * <pre>
     * cat( signif( p$sdev &circ; 2 / sum( p$sdev &circ; 2 ), 3 ), sep = &quot;,\n&quot; )
     * </pre>
     */
    @Test
    public void testVarianceFractions() {
        Double[] varianceFractions = svd.getVarianceFractions();

        double[] actualVarFractions = new double[] { 0.181, 0.161, 0.124, 0.0838, 0.0577, 0.0481, 0.0394, 0.0346,
                0.0304, 0.0258, 0.024, 0.0181, 0.0174, 0.0158, 0.0128, 0.0107, 0.0102, 0.00906, 0.00814, 0.00764,
                0.00625, 0.0059, 0.00508, 0.00486, 0.00444, 0.00395, 0.00371, 0.00342, 0.00336, 0.00294, 0.00278,
                0.00273, 0.00256, 0.00232, 0.00211, 0.00197, 0.00191, 0.00179, 0.00158, 0.00153, 0.00146, 0.00136,
                0.00119, 0.00116, 0.00104, 0.000962, 0.000918, 0.000915, 0.000822, 0.00076, 0.000702, 0.000678,
                0.000557, 0.000529, 0.000469, 0.000384, 0.000367, 0.000324, 5.99e-34 };

        assertEquals( 59, varianceFractions.length );
        assertTrue( RegressionTesting.closeEnough( actualVarFractions, actualVarFractions, 0.01 ) );
    }

    /**
     * Test on full-sized data set.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
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

    @Test
    public void testUMatrixAsExpressionData() throws Exception {
        svd = new ExpressionDataSVD( testData, true );
        ExpressionDataDoubleMatrix matrixAsExpressionData = svd.uMatrixAsExpressionData();
        assertNotNull( matrixAsExpressionData );
    }

    @Test
    public void testUMatrixAsExpressionDataUnnormalized() throws Exception {
        try {
            svd.uMatrixAsExpressionData();
            fail( "Should have gotten an exception" );
        } catch ( IllegalStateException e ) {
            //
        }
    }

    @Test
    public void testWinnow() throws Exception {
        ExpressionDataDoubleMatrix winnow = svd.winnow( 0.5 );
        assertEquals( 100, winnow.rows() );
    }

}
