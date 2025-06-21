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

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.RegressionTesting;
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
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.category.SlowTest;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author paul
 */
@ContextConfiguration
public class ExpressionDataSVDTest extends BaseTest {

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

    @Autowired
    private SimpleExpressionDataLoaderService service;

    @Autowired
    private TaxonService taxonService;

    private ExpressionDataDoubleMatrix testData = null;
    private ExpressionDataSVD svd = null;

    @Before
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

        double[] expectedSingularValues = new double[] { 44.33292, 41.91284, 40.28428, 31.91613, 25.80453, 23.97011,
                20.62388, 20.02206, 17.71859, 16.61712, 16.19350, 14.76118, 13.87069, 13.10054, 12.54802, 11.52781,
                11.11996, 10.36422, 10.09994, 9.691179, 9.153403, 8.634105, 7.881425, 7.754466, 7.245502, 6.993059,
                6.832636, 6.797229, 6.250005, 6.155713, 6.007695, 5.841762, 5.530337, 5.307789, 5.175314, 5.135153,
                4.951609, 4.775425, 4.68693, 4.447738, 4.302409, 4.195906, 3.964473, 3.808823, 3.75757, 3.701613,
                3.558042, 3.48766, 3.337000, 3.274710, 3.079108, 3.023564, 2.837062, 2.690218, 2.582036, 2.351277,
                2.171346, 1.877483, 2.745043e-15 };

        assertEquals( 59, singularValues.length );

        assertTrue( RegressionTesting.closeEnough( expectedSingularValues, singularValues, 0.05 ) );

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

    /*
     * Test on full-sized data set.
     */
    @Test
    @Category(SlowTest.class)
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

        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee.getRawExpressionDataVectors(),
                ee.getQuantitationTypes().iterator().next() );

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
