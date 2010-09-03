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

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.emory.mathcs.backport.java.util.Arrays;

import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * @author keshav, paul
 * @version $Id$
 */
public class DifferentialExpressionAnalyzerServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private ProcessedExpressionDataVectorService processedDataVectorService;

    @Autowired
    protected GeoDatasetService geoService;

    @Autowired
    GenericAncovaAnalyzer analyzer;

    ExpressionExperiment ee = null;

    boolean rReady = true;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {

        try {
            analyzer.connectToR();
        } catch ( Exception e ) {
            this.rReady = false;
        }
        analyzer.disconnectR();

        ee = expressionExperimentService.findByShortName( "GSE1611" );

        if ( ee == null ) {

            String path = getTestFileBasePath();
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                    + "gds994Short" ) );
            Collection<ExpressionExperiment> results = geoService.fetchAndLoad( "GSE1611", false, true, false, false,
                    true );

            ee = results.iterator().next();
            processedDataVectorService.createProcessedDataVectors( ee );

        }

        ee = expressionExperimentService.findByShortName( "GSE1611" );
        expressionExperimentService.thawLite( ee );
        differentialExpressionAnalyzerService.deleteOldAnalyses( ee );

        assertEquals( 2, ee.getExperimentalDesign().getExperimentalFactors().size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( bm + " " + ba, 2, bm.getFactorValues().size() );
            }
        }
    }

    /**
     * This requires that R is set up.
     * 
     * @throws Exception
     */
    @Test
    public void testAnalyzeAndDelete() throws Exception {

        if ( !this.rReady ) {
            log.info( "R is not available for the test" );
            return;
        }

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee );
        assertNotNull( analyses );
        assertTrue( !analyses.isEmpty() );
        assertNotNull( analyses.iterator().next() );

        int numDeleted = differentialExpressionAnalyzerService.deleteOldAnalyses( ee );
        assertTrue( numDeleted > 0 );
    }

    /**
     * This requires that R is set up. Test for bug 2026, not a subsetted analysis.
     * 
     * @throws Exception
     */
    @Test
    public void testAnalyzeAndDeleteSpecificAnalysis() throws Exception {
        if ( !this.rReady ) {
            log.info( "R is not available for the test" );
            return;
        }

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee );
        assertTrue( !analyses.isEmpty() );
        int numDeleted = differentialExpressionAnalyzerService.deleteOldAnalyses( ee, analyses.iterator().next(), ee
                .getExperimentalDesign().getExperimentalFactors() );
        assertTrue( numDeleted > 0 );
    }

    /**
     * Tests running with a subset factor, then deleting.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAnalyzeAndDeleteSpecificAnalysisWithSubset() throws Exception {
        if ( !this.rReady ) {
            log.info( "R is not available for the test" );
            return;
        }

        ExperimentalFactor[] factors = ee.getExperimentalDesign().getExperimentalFactors().toArray(
                new ExperimentalFactor[] {} );

        List<ExperimentalFactor> factorsToUse = Arrays.asList( new ExperimentalFactor[] { factors[0] } );
        ExperimentalFactor subsetFactor = factors[1];

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( factorsToUse );
        config.setSubsetFactor( subsetFactor );

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );

        assertTrue( !analyses.isEmpty() );
        int numDeleted = differentialExpressionAnalyzerService.deleteOldAnalyses( ee, analyses.iterator().next(),
                factorsToUse );
        assertTrue( numDeleted > 0 );
    }

    /**
     * This requires that R is set up.
     */
    @Test
    public void testWritePValuesHistogram() throws Exception {
        if ( !this.rReady ) {
            log.info( "R is not available for the test" );
            return;
        }
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee );
        differentialExpressionAnalyzerService.updateScoreDistributionFiles( ee );

    }
}
