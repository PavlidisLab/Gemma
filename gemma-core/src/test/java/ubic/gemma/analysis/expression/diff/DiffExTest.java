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

package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Tests added to check various cases of differential expression analysis.
 * 
 * @author Paul
 * @version $Id$
 */
public class DiffExTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private DiffExAnalyzer analyzer = null;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private ArrayDesign targetArrayDesign;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ProcessedExpressionDataVectorService dataVectorService;

    /**
     * Test differential expression analysis on count data. See bug 3383.
     */
    @Test
    public void testCountData() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        ExpressionExperiment ee = eeService.findByShortName( "GSE29006" );
        if ( ee != null ) {
            eeService.delete( ee );
        }

        assertTrue( eeService.findByShortName( "GSE29006" ) == null );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE29006", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            throw new IllegalStateException( "Need to delete this data set before test is run" );
        }

        ee = eeService.thaw( ee );

        InputStream is = this.getClass().getResourceAsStream(
                "/data/loader/expression/flatfileload/GSE29006_design.txt" );
        assertNotNull( is );
        experimentalDesignImporter.importDesign( ee, is );

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();

        InputStream countData = this.getClass().getResourceAsStream(
                "/data/loader/expression/flatfileload/GSE29006_expression_count.test.txt" );
        DoubleMatrix<String, String> countMatrix = reader.read( countData );

        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertEquals( 1, experimentalFactors.size() );

        List<String> probeNames = countMatrix.getRowNames();
        assertEquals( 199, probeNames.size() );

        // we have to find the right generic platform to use.
        targetArrayDesign = this.getTestPersistentArrayDesign( probeNames, taxonService.findByCommonName( "human" ) );
        targetArrayDesign = arrayDesignService.thaw( targetArrayDesign );

        // the experiment has 8 samples but the data has 4 columns so allow missing samples
        // GSM718707 GSM718708 GSM718709 GSM718710
        boolean allMissingSamples = true;
        dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, null, 36, true, allMissingSamples );

        // make sure to do a thaw() to get the addCountData() updates
        ExpressionExperiment updatedee = eeService.thaw( ee );

        // verify rows and columns
        Collection<DoubleVectorValueObject> processedDataArrays = dataVectorService.getProcessedDataArrays( updatedee );
        assertEquals( 199, processedDataArrays.size() );
        for ( DoubleVectorValueObject v : processedDataArrays ) {
            assertEquals( 4, v.getBioAssays().size() );
        }

        // check that the samples are there
        boolean found = false;
        for ( BioAssay ba : updatedee.getBioAssays() ) {
            assertEquals( targetArrayDesign, ba.getArrayDesignUsed() );

            assertEquals( 36, ba.getSequenceReadLength().intValue() );

            if ( ba.getDescription().contains( "GSM718709" ) ) {
                assertEquals( 320383, ba.getSequenceReadCount().intValue() );
                found = true;
            }
        }
        assertTrue( found );

        // DE analysis
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( updatedee.getExperimentalDesign().getExperimentalFactors() );
        config.setQvalueThreshold( null );
        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( updatedee, config );
        assertNotNull( analyses );
        assertEquals( 1, analyses.size() );

        DifferentialExpressionAnalysis results = analyses.iterator().next();

        found = false;
        ExpressionAnalysisResultSet resultSet = results.getResultSets().iterator().next();
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            if ( r.getProbe().getName().equals( "ENSG00000000938" ) ) {
                // this is one of the top DE probe based on p-value
                found = true;
                // slightly smaller pvalue when using weighted least squares
                assertEquals( 0.007914245, r.getPvalue(), 0.00001 );
                break;
            }
        }
        assertTrue( found );
    }

    /**
     * Test where probes have constant values. See bug 3177.
     */
    @Test
    public void testGSE35930() throws Exception {

        ExpressionExperiment ee;
        // eeService.delete( eeService.findByShortName( "GSE35930" ) );
        try {
            geoService
                    .setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "GSE35930" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE35930", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            // OK.
            if ( e.getData() instanceof List ) {
                ee = ( ExpressionExperiment ) ( ( List<?> ) e.getData() ).iterator().next();
            } else {
                ee = ( ExpressionExperiment ) e.getData();
            }
        }

        ee = eeService.thawLite( ee );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        if ( ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            ee = eeService.load( ee.getId() );
            ee = eeService.thawLite( ee );

            InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/geo/GSE35930/design.txt" );
            experimentalDesignImporter.importDesign( ee, is, false );

            ee = eeService.load( ee.getId() );
            ee = eeService.thawLite( ee );
        }

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( ee.getExperimentalDesign().getExperimentalFactors() );
        config.setQvalueThreshold( null );
        Collection<DifferentialExpressionAnalysis> analyses = analyzer.run( ee, config );
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
