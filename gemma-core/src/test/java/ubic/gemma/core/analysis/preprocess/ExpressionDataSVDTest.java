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
package ubic.gemma.core.analysis.preprocess;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVectorSub;
import no.uib.cipr.matrix.sparse.ArpackSym;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.math.MatrixStats;
import ubic.gemma.core.util.math.linalg.SingularValueDecomposition;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixReader;
import ubic.gemma.core.util.RegressionTesting;
import ubic.gemma.core.analysis.preprocess.svd.ExpressionDataSVD;
import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.loader.expression.geo.*;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderServiceImpl;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimplePlatformMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleQuantitationTypeMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleTaxonMetadata;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.InputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author paul
 */
@ContextConfiguration
public class ExpressionDataSVDTest extends BaseTest5 {

    private static final Log log = LogFactory.getLog( ExpressionDataSVDTest.class );

    /**
     * Components the top-k tests compute; asking for more than {@link #COMPONENTS_USED} keeps the used ones
     * away from the edge of the requested set, where Lanczos converges last.
     */
    private static final int TOP_K = 10;

    /**
     * Components PCA consumers read (SVDServiceImpl.MAX_NUM_COMPONENTS_TO_PERSIST).
     */
    private static final int COMPONENTS_USED = 5;

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public SimpleExpressionDataLoaderService singleCellDataLoaderService() {
            return new SimpleExpressionDataLoaderServiceImpl();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public PersisterHelper persisterHelper() {
            return mock();
        }

        @Bean
        public ubic.gemma.persistence.service.expression.experiment.EeWriteService eeWriteService() {
            return mock();
        }

        @Bean
        public ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentPrePersistService expressionExperimentPrePersistService() {
            return mock();
        }

        @Bean
        public PreprocessorService preprocessorService() {
            return mock();
        }

        @Bean
        public TaxonService taxonService() {
            return mock();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock();
        }
    }

    /**
     * Singular values of the double-standardized fixture, from R; see {@link #testEigenvalues()}.
     */
    private static final double[] R_SINGULAR_VALUES = new double[] {
            44.33292, 41.91284, 40.28428, 31.91613, 25.80453, 23.97011,
            20.62388, 20.02206, 17.71859, 16.61712, 16.19350, 14.76118, 13.87069, 13.10054, 12.54802, 11.52781,
            11.11996, 10.36422, 10.09994, 9.691179, 9.153403, 8.634105, 7.881425, 7.754466, 7.245502, 6.993059,
            6.832636, 6.797229, 6.250005, 6.155713, 6.007695, 5.841762, 5.530337, 5.307789, 5.175314, 5.135153,
            4.951609, 4.775425, 4.68693, 4.447738, 4.302409, 4.195906, 3.964473, 3.808823, 3.75757, 3.701613,
            3.558042, 3.48766, 3.337000, 3.274710, 3.079108, 3.023564, 2.837062, 2.690218, 2.582036, 2.351277,
            2.171346, 1.877483, 2.745043e-15 };

    /**
     * Variance fractions of the double-standardized fixture, from R; see {@link #testVarianceFractions()}.
     */
    private static final double[] R_VARIANCE_FRACTIONS = new double[] {
            0.169, 0.151, 0.14, 0.0878, 0.0574, 0.0495, 0.0367, 0.0346,
            0.0271, 0.0238, 0.0226, 0.0188, 0.0166, 0.0148, 0.0136, 0.0115, 0.0107, 0.00926, 0.00879, 0.0081,
            0.00722, 0.00643, 0.00535, 0.00518, 0.00453, 0.00422, 0.00402, 0.00398, 0.00337, 0.00327, 0.00311,
            0.00294, 0.00264, 0.00243, 0.00231, 0.00227, 0.00211, 0.00197, 0.00189, 0.00171, 0.0016, 0.00152,
            0.00135, 0.00125, 0.00122, 0.00118, 0.00109, 0.00105, 0.00096, 0.000924, 0.000817, 0.000788,
            0.000694, 0.000624, 0.000575, 0.000477, 0.000406, 0.000304, 3.75e-33 };

    @Autowired
    private SimpleExpressionDataLoaderService service;

    @Autowired
    private TaxonService taxonService;

    private ExpressionDataDoubleMatrix testData = null;
    private ExpressionDataSVD svd = null;

    @BeforeEach
    public void setUp() throws Exception {
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();

        Collection<SimplePlatformMetadata> ads = new HashSet<>();
        SimplePlatformMetadata ad = new SimplePlatformMetadata();
        ad.setShortName( "test" );
        ad.setName( "new ad" );
        ad.setTechnologyType( TechnologyType.GENELIST );
        ads.add( ad );
        metaData.setArrayDesigns( ads );

        metaData.setTaxon( SimpleTaxonMetadata.forName( "mouse" ) );
        metaData.setShortName( "test" );
        metaData.setName( "ee" );

        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( "testing" );
        qtMetadata.setGeneralType( GeneralType.QUANTITATIVE );
        qtMetadata.setScale( ScaleType.LOG2 );
        qtMetadata.setType( StandardQuantitationType.AMOUNT );
        qtMetadata.setIsRatio( true );
        metaData.setQuantitationType( qtMetadata );

        DoubleMatrix<String, String> matrix;
        try ( InputStream data = this.getClass()
                .getResourceAsStream( "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" ) ) {
            matrix = new DoubleMatrixReader().read( data );
        }

        Taxon mouseTaxon = new Taxon();
        when( taxonService.findByCommonName( "mouse" ) ).thenReturn( mouseTaxon );

        ExpressionExperiment ee = service.convert( metaData, matrix );
        testData = new ExpressionDataDoubleMatrix( ee, ee.getRawExpressionDataVectors() );
        svd = new ExpressionDataSVD( testData, false );
    }

    @Test
    public void testGetS() {
        DoubleMatrix<Integer, Integer> s = svd.getS();
        assertNotNull( s );
        List<Integer> colNames = s.getColNames();
        for ( Integer integer : colNames ) {
            assertNotNull( integer );
        }
    }

    @Test
    public void testGetU() {
        DoubleMatrix<CompositeSequence, Integer> u = svd.getU();
        assertNotNull( u );
    }

    @Test
    public void testMatrixReconstruct() {
        ExpressionDataDoubleMatrix svdNormalize = svd.removeHighestComponents( 0 );
        assertNotNull( svdNormalize );
        RegressionTesting.closeEnough( testData.getMatrix(), svdNormalize.getMatrix(), 0.001 );
    }

    /*
     * <pre>
     * testdata<-read.table("C:/users/paul/dev/eclipseworkspace/Gemma/gemma-core/src/test/resources/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort",
     *          header=T, row.names=1)
     * testdata.s <- testdata
     * for(i in 1:5) {
     *   testdata.s <-  t(scale(t(scale(testdata.s))));
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
    public void testEigenvalues() throws SVDException {
        svd = new ExpressionDataSVD( testData, true );

        double[] singularValues = svd.getSingularValues();


        assertEquals( 59, singularValues.length );

        assertTrue( RegressionTesting.closeEnough( R_SINGULAR_VALUES, singularValues, 0.05 ) );

        double[] eigenvalues = svd.getEigenvalues();

        double[] actualEigenValues = new double[] { 9.876418, 8.827503, 8.154231, 5.118728, 3.346094, 2.887258, 2.13741,
                2.014482, 1.577620, 1.387581, 1.317728, 1.094936, 0.9668133, 0.8624328, 0.7912195, 0.667791, 0.621372,
                0.5397819, 0.5126067, 0.4719545, 0.4210288, 0.3746118, 0.3121450, 0.3021694, 0.2638054, 0.2457431,
                0.2345975, 0.2321725, 0.1962941, 0.1904161, 0.1813688, 0.1714884, 0.1536916, 0.1415709, 0.1345923,
                0.1325116, 0.1232082, 0.1145964, 0.1103885, 0.09940891, 0.09301872, 0.08847049, 0.0789801, 0.07290015,
                0.07095141, 0.06885395, 0.0636164, 0.06112447, 0.05595763, 0.05388808, 0.04764272, 0.04593939,
                0.04044683, 0.03636818, 0.03350205, 0.02778140, 0.02369217, 0.01771328, 2.365972e-16 };
        assertEquals( 59, eigenvalues.length );
        assertTrue( RegressionTesting.closeEnough( actualEigenValues, eigenvalues, 0.01 ) );
    }

    /*
     * Same double-standardized matrix as testEigenvalues (testdata.s there); fractions are d^2 / sum(d^2):
     *
     * <pre>
     * s <- svd(testdata.s)
     * cat(signif(s$d^2 / sum(s$d^2), 3), sep = ", ")
     * </pre>
     */
    @Test
    public void testVarianceFractions() throws SVDException {
        svd = new ExpressionDataSVD( testData, true );
        Double[] varianceFractions = svd.getVarianceFractions();


        assertEquals( 59, varianceFractions.length );
        // expected values carry 3 significant digits, so the largest rounding error is 0.0005
        assertTrue( RegressionTesting.closeEnough( R_VARIANCE_FRACTIONS, ArrayUtils.toPrimitive( varianceFractions ),
                0.001 ) );
        double total = 0;
        for ( Double f : varianceFractions ) {
            total += f;
        }
        assertEquals( 1.0, total, 1e-12 );
    }

    /**
     * The top-k path for large datasets: the k largest eigenpairs of XᵀX, from MTJ's {@link ArpackSym}, must reproduce
     * the leading components of the full SVD on the same double-standardized matrix, and R's values for this fixture.
     * <p>
     * With X = U S Vᵀ, XᵀX = V S² Vᵀ, so the eigenvalues are the squared singular values, the eigenvectors are the
     * columns of V, and u_i = X v_i / s_i. Variance fractions divide by ‖X‖_F², which equals the sum of all s² without
     * computing them.
     */
    @Test
    public void testTopKEigenpairsMatchFullSvd() throws SVDException {
        svd = new ExpressionDataSVD( testData, true );
        assertEquals( 200, svd.getNumVariables(), "no rows should be filtered from this fixture" );
        double[][] x = MatrixStats.doubleStandardize( testData.getMatrix() ).getRawMatrix();
        assertEquals( 0, maxReconstructionError( x, svd.getU().getRawMatrix(), svd.getSingularValues(),
                svd.getV().getRawMatrix() ), 1e-10, "X must be the matrix the full SVD decomposed" );

        double[] s = assertTopKMatchesFullSvd( x, svd.getSingularValues(), svd.getU().getRawMatrix(),
                svd.getV().getRawMatrix(), TOP_K );

        double totalSq = sumOfSquares( x );
        Double[] fullFractions = svd.getVarianceFractions();
        for ( int i = 0; i < TOP_K; i++ ) {
            // R prints 7 significant digits
            assertEquals( R_SINGULAR_VALUES[i], s[i], 1e-5, "singular value " + ( i + 1 ) + " vs R" );
            double fraction = s[i] * s[i] / totalSq;
            // s² doubles the relative error of s (1e-8, see assertTopKMatchesFullSvd), so components past the used
            // ones, which converge last, are held to that; the used ones agree to rounding
            double tolerance = i < COMPONENTS_USED ? 1e-12 : 2e-8 * fraction;
            assertEquals( fullFractions[i], fraction, tolerance, "variance fraction " + ( i + 1 ) + " vs full SVD" );
            // R values carry 3 significant digits
            assertEquals( R_VARIANCE_FRACTIONS[i], fraction, 5e-4, "variance fraction " + ( i + 1 ) + " vs R" );
        }
    }

    /**
     * The hard case for Lanczos: pure noise, where neighbouring singular values differ by well under 1%. The
     * leading components must still match the full SVD.
     */
    @Test
    public void testTopKEigenpairsMatchFullSvdWithNearTiedComponents() {
        Random r = new Random( 42L );
        int genes = 400, samples = 100;
        DoubleMatrix<Integer, Integer> noise = new DenseDoubleMatrix<>( genes, samples );
        List<Integer> rowNames = new ArrayList<>(), colNames = new ArrayList<>();
        for ( int g = 0; g < genes; g++ ) {
            rowNames.add( g );
            for ( int j = 0; j < samples; j++ ) {
                noise.set( g, j, r.nextGaussian() );
            }
        }
        for ( int j = 0; j < samples; j++ ) {
            colNames.add( j );
        }
        noise.setRowNames( rowNames );
        noise.setColumnNames( colNames );
        DoubleMatrix<Integer, Integer> std = MatrixStats.doubleStandardize( noise );
        SingularValueDecomposition<Integer, Integer> full = new SingularValueDecomposition<>( std );

        assertTopKMatchesFullSvd( std.getRawMatrix(), full.getSingularValues(), full.getU().getRawMatrix(),
                full.getV().getRawMatrix(), TOP_K );
    }

    /**
     * Compare the top-k eigenpairs of XᵀX with the full SVD of X. Singular values must agree for all k; the singular
     * vectors (up to sign) for the {@link #COMPONENTS_USED} components that PCA consumers read.
     *
     * @param vFull samples × components, as Colt returns it
     * @return the top-k singular values from the eigenpairs, largest first
     */
    private static double[] assertTopKMatchesFullSvd( double[][] x, double[] sFull, double[][] uFull, double[][] vFull,
            int k ) {
        int genes = x.length, samples = x[0].length;
        Map<Double, DenseVectorSub> eig = topKEigenpairs( x, k );
        // ArpackSym keys its result by eigenvalue, so tied eigenvalues would collapse into one entry
        assertEquals( k, eig.size(), "distinct eigenvalues returned" );
        List<Double> lambdas = new ArrayList<>( eig.keySet() );
        lambdas.sort( Comparator.reverseOrder() );

        double[] sTop = new double[k];
        for ( int i = 0; i < k; i++ ) {
            double s = Math.sqrt( lambdas.get( i ) );
            sTop[i] = s;
            DenseVectorSub v = eig.get( lambdas.get( i ) );
            double vDot = 0;
            for ( int j = 0; j < samples; j++ ) {
                vDot += v.get( j ) * vFull[j][i];
            }
            double uDot = 0;
            for ( int g = 0; g < genes; g++ ) {
                double u = 0;
                for ( int j = 0; j < samples; j++ ) {
                    u += x[g][j] * v.get( j );
                }
                uDot += u / s * uFull[g][i];
            }
            double sErr = Math.abs( s - sFull[i] ) / sFull[i];
            log.info( String.format( "PC %2d: s = %.6f, relative gap to next %.2e, |Δs|/s = %.2e, 1 - |v·v'| = %.2e, 1 - |u·u'| = %.2e",
                    i + 1, sFull[i], ( sFull[i] - sFull[i + 1] ) / sFull[i], sErr, 1 - Math.abs( vDot ), 1 - Math.abs( uDot ) ) );
            assertEquals( 0, sErr, 1e-8, "singular value " + ( i + 1 ) );
            if ( i < COMPONENTS_USED ) {
                assertEquals( 1, Math.abs( vDot ), 1e-10, "V column " + ( i + 1 ) );
                assertEquals( 1, Math.abs( uDot ), 1e-10, "U column " + ( i + 1 ) );
            }
        }
        return sTop;
    }

    private static Map<Double, DenseVectorSub> topKEigenpairs( double[][] x, int k ) {
        DenseMatrix xm = new DenseMatrix( x );
        DenseMatrix gram = new DenseMatrix( xm.numColumns(), xm.numColumns() );
        xm.transAmult( xm, gram );
        return new ArpackSym( gram ).solve( k, ArpackSym.Ritz.LA );
    }

    private static double sumOfSquares( double[][] x ) {
        double total = 0;
        for ( double[] row : x ) {
            for ( double v : row ) {
                total += v * v;
            }
        }
        return total;
    }

    private static double maxReconstructionError( double[][] x, double[][] u, double[] s, double[][] v ) {
        double max = 0;
        for ( int g = 0; g < x.length; g++ ) {
            for ( int j = 0; j < x[0].length; j++ ) {
                double xr = 0;
                for ( int c = 0; c < s.length; c++ ) {
                    xr += u[g][c] * s[c] * v[j][c];
                }
                max = Math.max( max, Math.abs( x[g][j] - xr ) );
            }
        }
        return max;
    }

    /*
     * Test on full-sized data set.
     */
    @Test
    @Tag("slow")
    public void testMatrixReconstructB() throws Exception {
        GeoConverter gc = new GeoConverterImpl();
        gc.setElementLimitForStrictness( 15000 );
        InputStream is = new GZIPInputStream( new ClassPathResource( "/data/loader/expression/geo/fullSizeTests/GSE1623_family.soft.txt.gz" ).getInputStream() );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE1623" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        @SuppressWarnings("unchecked") Collection<ExpressionExperiment> result = ( Collection<ExpressionExperiment> ) gc
                .convert( series );
        assertNotNull( result );
        assertEquals( 1, result.size() );
        ExpressionExperiment ee = result.iterator().next();

        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, ee.getRawExpressionDataVectors() );

        svd = new ExpressionDataSVD( matrix, false );

        ExpressionDataDoubleMatrix svdNormalize = svd.removeHighestComponents( 1 );
        assertNotNull( svdNormalize );
    }

    @Test
    public void testUMatrixAsExpressionData() throws SVDException {
        svd = new ExpressionDataSVD( testData, true );
        ExpressionDataDoubleMatrix matrixAsExpressionData = svd.uMatrixAsExpressionData();
        assertNotNull( matrixAsExpressionData );
    }

    @Test
    public void testUMatrixAsExpressionDataUnnormalized() {
        try {
            svd.uMatrixAsExpressionData();
            fail( "Should have gotten an exception" );
        } catch ( IllegalStateException e ) {
            //
        }
    }

    @Test
    public void testWinnow() {
        ExpressionDataDoubleMatrix winnow = svd.winnow( 0.5 );
        assertEquals( 100, winnow.rows() );
    }

}
