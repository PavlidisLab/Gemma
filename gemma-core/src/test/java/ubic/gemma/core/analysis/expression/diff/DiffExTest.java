/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.core.analysis.expression.diff;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.loader.util.TestUtils;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests added to check various cases of differential expression analysis.
 *
 * @author Paul
 */
@Category(SlowTest.class)
public class DiffExTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private DiffExAnalyzer analyzer = null;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    /* fixtures */
    private ExpressionExperiment ee;

    @After
    public void tearDown() {
        if ( ee != null ) {
            eeService.remove( ee );
        }
    }


    /**
     * Test differential expression analysis on RNA-seq data. See bug 3383. R code in voomtest.R
     */
    @Test
    public void testCountData() throws Exception {
        ee = eeService.findByShortName( "GSE29006" );
        Assume.assumeTrue( String.format( "%s was not properly cleaned up by another test.", ee ),
                ee == null );

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE29006", false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AccessDeniedException e ) {
            // see https://github.com/PavlidisLab/Gemma/issues/206
            Assume.assumeNoException( e );
        } catch ( AlreadyExistsInSystemException e ) {
            throw new IllegalStateException( "Need to remove this data set before test is run" );
        }

        assertNotNull( ee );

        ee = eeService.thaw( ee );

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/flatfileload/GSE29006_design.txt" ) ) {
            assertNotNull( is );
            experimentalDesignImporter.importDesign( ee, is );
        }

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();

        ArrayDesign targetArrayDesign;
        try ( InputStream countData = this.getClass()
                .getResourceAsStream( "/data/loader/expression/flatfileload/GSE29006_expression_count.test.txt" ) ) {
            DoubleMatrix<String, String> countMatrix = reader.read( countData );

            Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
            assertEquals( 1, experimentalFactors.size() );

            List<String> probeNames = countMatrix.getRowNames();
            assertEquals( 199, probeNames.size() );

            // we have to find the right generic platform to use.
            targetArrayDesign = this
                    .getTestPersistentArrayDesign( probeNames, taxonService.findByCommonName( "human" ) );
            targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

            Map<BioAssay, SequencingMetadata> sequencingMetadata = new HashMap<>();
            for ( BioAssay ba : ee.getBioAssays() ) {
                sequencingMetadata.put( ba, SequencingMetadata.builder().readLength( 36 ).isPaired( true ).build() );
            }

            // the experiment has 8 samples but the data has 4 columns so allow missing samples
            // GSM718707 GSM718708 GSM718709 GSM718710
            dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, null, sequencingMetadata, true );
        }

        // make sure to do a thawRawAndProcessed() to get the addCountData() updates
        ee = eeService.thaw( ee );

        // verify rows and columns
        Collection<DoubleVectorValueObject> processedDataArrays = processedExpressionDataVectorService
                .getProcessedDataArrays( ee );
        assertEquals( 199, processedDataArrays.size() );
        for ( DoubleVectorValueObject v : processedDataArrays ) {
            assertEquals( 4, v.getBioAssays().size() );
        }

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee, true );
        assertEquals( 199, dmatrix.rows() );
        assertEquals( 4, dmatrix.columns() );

        // I confirmed that log2cpm is working same as voom here; not bothering to test directly.

        TestUtils.assertBAs( ee, targetArrayDesign, "GSM718709", 320383 );

        // DE analysis without weights to assist comparison to R
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setUseWeights( false );
        config.setModerateStatistics( false );
        config.addFactorsToInclude( ee.getExperimentalDesign().getExperimentalFactors() );
        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config );
        assertNotNull( analyses );
        assertEquals( 1, analyses.size() );
        DifferentialExpressionAnalysis results = analyses.iterator().next();
        boolean found = false;
        ExpressionAnalysisResultSet resultSet = results.getResultSets().iterator().next();
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            if ( r.getProbe().getName().equals( "ENSG00000000938" ) ) {
                found = true;
                ContrastResult contrast = r.getContrasts().iterator().next();

                assertEquals( 0.007055717, r.getPvalue(),
                        0.00001 ); // R: 0.006190738; coeff = 2.2695215; t=12.650422;
                // up to sign
                assertEquals( 2.2300049, Math.abs( contrast.getCoefficient() ), 0.001 );
                break;
            }
        }
        assertTrue( found );

        // With weights
        config = new DifferentialExpressionAnalysisConfig();
        config.setUseWeights( true ); // <----
        config.addFactorsToInclude( ee.getExperimentalDesign().getExperimentalFactors() );
        config.setModerateStatistics( false );
        analyses = analyzer.run( ee, dmatrix, config );
        results = analyses.iterator().next();
        resultSet = results.getResultSets().iterator().next();
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            if ( r.getProbe().getName().equals( "ENSG00000000938" ) ) {
                // these are the values computed with *our* weights, which are a tiny bit different (details of lowess)
                // also values changed very slightly with updated library size computation (post-filtering)
                assertEquals( 1, r.getContrasts().size() );
                ContrastResult contrast = r.getContrasts().iterator().next();
                assertNotNull( contrast.getCoefficient() );
                assertEquals( 2.272896, Math.abs( contrast.getCoefficient() ), 0.0001 );
                assertNotNull( contrast.getPvalue() );
                assertEquals( 0.006149004, contrast.getPvalue(), 0.0001 );
                assertNotNull( contrast.getTstat() );
                assertEquals( 12.6937, Math.abs( contrast.getTstat() ), 0.0001 );
                assertEquals( 0.006149003, r.getPvalue(), 0.00001 );
                break;
            }
        }
    }

    /**
     * Test where probes have constant values. See bug 3177.
     */
    @Test
    public void testGSE35930() throws Exception {
        ee = eeService.findByShortName( "GSE35930" );
        Assume.assumeTrue( String.format( "%s was not properly cleaned up by another test.", ee ),
                ee == null );

        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "GSE35930" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE35930", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            // OK.
            if ( e.getData() instanceof List ) {
                ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                ee = ( ExpressionExperiment ) e.getData();
            }
        }

        ee = this.eeService.thaw( ee );

        processedExpressionDataVectorService.createProcessedDataVectors( ee, true );

        if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            ee = eeService.load( ee.getId() );
            assertNotNull( ee );
            ee = this.eeService.thawLite( ee );

            try ( InputStream is = this.getClass()
                    .getResourceAsStream( "/data/loader/expression/geo/GSE35930/design.txt" ) ) {
                experimentalDesignImporter.importDesign( ee, is );
            }

            ee = eeService.load( ee.getId() );
            assertNotNull( ee );
            ee = this.eeService.thawLite( ee );
        }

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.addFactorsToInclude( ee.getExperimentalDesign().getExperimentalFactors() );
        config.setModerateStatistics( false );
        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee, true );
        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, dmatrix, config );
        assertNotNull( analyses );
        assertEquals( 1, analyses.size() );

        DifferentialExpressionAnalysis results = analyses.iterator().next();

        boolean found = false;
        ExpressionAnalysisResultSet resultSet = results.getResultSets().iterator().next();
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            // this probe has a constant value
            if ( r.getProbe().getName().equals( "1622910_at" ) ) {
                fail( "Should not have found a result for constant probe" );
                // found = true;
                // assertTrue( "Got: " + pvalue, pvalue == null || pvalue.equals( Double.NaN ) );
            } else {
                found = true; // got to have something...
            }
        }
        assertTrue( found );
    }
}
