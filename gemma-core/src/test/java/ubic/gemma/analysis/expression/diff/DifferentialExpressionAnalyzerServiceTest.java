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
package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.emory.mathcs.backport.java.util.Arrays;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.visualization.ExperimentalDesignVisualizationService;

/**
 * @author keshav, paul
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    @Autowired
    private DifferentialExpressionAnalysisHelperService differentialExpressionAnalyzerHelperService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private ProcessedExpressionDataVectorService processedDataVectorService;

    @Autowired
    ExperimentalDesignVisualizationService experimentalDesignVisualizationService;

    @Autowired
    ExpressionDataFileService expressionDataFileService;

    @Autowired
    protected GeoService geoService;

    @Autowired
    DiffExAnalyzer analyzer;

    @Autowired
    ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    ExperimentalFactorService experimentalFactorService;

    ExpressionExperiment ee = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {

        ee = expressionExperimentService.findByShortName( "GSE1611" );

        if ( ee == null ) {

            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                    getTestFileBasePath( "gds994Short" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE1611", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        }
        processedDataVectorService.createProcessedDataVectors( ee );

        ee = expressionExperimentService.findByShortName( "GSE1611" );
        ee = expressionExperimentService.thawLite( ee );
        differentialExpressionAnalyzerHelperService.deleteAnalyses( ee );
        assertEquals( 2, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( bm + " " + ba, 2, bm.getFactorValues().size() );
            }
        }
    }

    /**
     * @throws Exception
     */
    @Test
    public void testAnalyzeAndDelete() throws Exception {

        assert ee.getId() != null;

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, ee.getExperimentalDesign().getExperimentalFactors() );
        assertNotNull( analyses );
        assertTrue( !analyses.isEmpty() );
        assertNotNull( analyses.iterator().next() );

        /*
         * Exercise the matrix output services.
         */
        Collection<File> outputLocations = expressionDataFileService.writeOrLocateDiffExpressionDataFiles( ee, true );

        assertEquals( 1, outputLocations.size() );

        File outputLocation = outputLocations.iterator().next();

        log.info( outputLocation );
        DoubleMatrixReader r = new DoubleMatrixReader();
        DoubleMatrix<String, String> readIn = r.read( outputLocation.getAbsolutePath() );

        assertEquals( 100, readIn.rows() );
        assertEquals( 6, readIn.columns() ); // interactions will be included by default.

        // / delete the analysis
        int numDeleted = differentialExpressionAnalyzerHelperService.deleteAnalyses( ee );
        assertTrue( numDeleted > 0 );

        // this is kind of thrown in here
        LinkedHashMap<BioAssay, LinkedHashMap<ExperimentalFactor, Double>> layout = experimentalDesignVisualizationService
                .getExperimentalDesignLayout( ee );
        assertEquals( 12, layout.size() );
    }

    /**
     * Test for bug 2026, not a subsetted analysis.
     * 
     * @throws Exception
     */
    @Test
    public void testAnalyzeAndDeleteSpecificAnalysis() throws Exception {

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, ee.getExperimentalDesign().getExperimentalFactors() );
        assertTrue( !analyses.isEmpty() );
        differentialExpressionAnalyzerHelperService.deleteAnalysis( ee, analyses.iterator().next() );

    }

    /**
     * Tests running with a subset factor, then deleting.
     * 
     * @throws Exception
     */
    @Test
    public void testAnalyzeAndDeleteSpecificAnalysisWithSubset() throws Exception {

        ExperimentalFactor[] factors = ee.getExperimentalDesign().getExperimentalFactors()
                .toArray( new ExperimentalFactor[] {} );

        List<ExperimentalFactor> factorsToUse = Arrays.asList( new ExperimentalFactor[] { factors[0] } );
        ExperimentalFactor subsetFactor = factors[1];

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( factorsToUse );
        config.setSubsetFactor( subsetFactor );

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );

        assertTrue( !analyses.isEmpty() );
        differentialExpressionAnalyzerHelperService.deleteAnalysis( ee, analyses.iterator().next() );

    }

    /**
     * Test inspired by bug 2605
     * 
     * @throws Exception
     */
    @Test
    public void testAnalyzeWithSubsetWhenOneIsNotUsableAndWithInteractionInTheOther() throws Exception {
        ee = expressionExperimentService.findByShortName( "GSE32136" );

        if ( ee == null ) {

            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( getTestFileBasePath() ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE32136", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();

        }
        processedDataVectorService.createProcessedDataVectors( ee );

        ee = expressionExperimentService.thawLite( ee );
        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();

        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            experimentalFactorService.delete( experimentalFactor );
        }

        ee = expressionExperimentService.thawLite( ee );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/geo/GSE32136.design.txt" );
        assertNotNull( is );
        experimentalDesignImporter.importDesign( ee, is );

        differentialExpressionAnalyzerHelperService.deleteAnalyses( ee );

        experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertEquals( 3, experimentalFactors.size() );

        // Wshew, done with setting it up.

        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        ExperimentalFactor subsetFactor = null;
        for ( ExperimentalFactor ef : experimentalFactors ) {
            if ( ef.getName().equals( "PooledTreatment" ) ) {
                subsetFactor = ef;
            } else {
                factors.add( ef );
            }
        }

        assertNotNull( subsetFactor );
        assertEquals( 2, factors.size() );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setFactorsToInclude( factors );
        config.setSubsetFactor( subsetFactor );
        HashSet<Collection<ExperimentalFactor>> ifacts = new HashSet<Collection<ExperimentalFactor>>();
        ifacts.add( factors );
        config.setInteractionsToInclude( ifacts );

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );

        assertEquals( "Should have quietly ignored one of the subsets that is not analyzable", 1, analyses.size() );

        DifferentialExpressionAnalysis analysis = analyses.iterator().next();
        assertEquals( "Subsetting as not done correctly", subsetFactor, analysis.getSubsetFactorValue()
                .getExperimentalFactor() );
        assertEquals( "Interaction was not retained in the analyzed subset", 3, analysis.getResultSets().size() );
    }

    /**
     * 
     */
    @Test
    public void testWritePValuesHistogram() throws Exception {

        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee, ee.getExperimentalDesign()
                .getExperimentalFactors() );
        differentialExpressionAnalyzerService.updateScoreDistributionFiles( ee );

    }
}
