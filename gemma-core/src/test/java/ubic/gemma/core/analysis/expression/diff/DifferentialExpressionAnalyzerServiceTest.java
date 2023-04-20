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
package ubic.gemma.core.analysis.expression.diff;

import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeNoException;

/**
 * @author keshav, paul
 */
@Category(SlowTest.class)
public class DifferentialExpressionAnalyzerServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    @Autowired
    private ExperimentalDesignImporter experimentalDesignImporter;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private ProcessedExpressionDataVectorService processedDataVectorService;

    @Autowired
    private DifferentialExpressionResultService resultService;

    @Autowired
    private ArrayDesignAnnotationService arrayDesignAnnotationService;

    @Autowired
    private AclTestUtils aclTestUtils;

    /* fixtures */
    private ExpressionExperiment ee;

    @After
    public void tearDown() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    /**
     * Test for bug 2026, not a subsetted analysis.
     */
    @Test
    public void testAnalyzeAndDeleteSpecificAnalysis() throws Exception {
        prepareGSE1611();

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();
        config.setFactorsToInclude( factors );
        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );
        assertFalse( analyses.isEmpty() );

        Collection<Long> experimentsWithAnalysis = differentialExpressionAnalysisService
                .getExperimentsWithAnalysis( Collections.singleton( ee.getId() ) );
        assertTrue( experimentsWithAnalysis.contains( ee.getId() ) );

        assertTrue( differentialExpressionAnalysisService
                .getExperimentsWithAnalysis( taxonService.findByCommonName( "mouse" ) ).contains( ee.getId() ) );

        differentialExpressionAnalyzerService.deleteAnalysis( ee, analyses.iterator().next() );

    }

    /**
     * Tests running with a subset factor, then deleting.
     */
    @Test
    public void testAnalyzeAndDeleteSpecificAnalysisWithSubset() throws Exception {
        prepareGSE1611();

        ExperimentalFactor[] factors = ee.getExperimentalDesign().getExperimentalFactors()
                .toArray( new ExperimentalFactor[] {} );

        List<ExperimentalFactor> factorsToUse = Collections.singletonList( factors[0] );
        ExperimentalFactor subsetFactor = factors[1];

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( factorsToUse );
        config.setSubsetFactor( subsetFactor );
        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );

        assertFalse( analyses.isEmpty() );

        differentialExpressionAnalyzerService.deleteAnalysis( ee, analyses.iterator().next() );

    }

    @Test
    public void testAnalyzeAndDelete() throws Exception {
        prepareGSE1611();

        assertNotNull( ee.getId() );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();
        config.setFactorsToInclude( factors );
        config.addInteractionToInclude( factors );
        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );
        assertNotNull( analyses );
        assertFalse( analyses.isEmpty() );
        assertNotNull( analyses.iterator().next() );

        DifferentialExpressionAnalysis analysis = differentialExpressionAnalysisService
                .thawFully( analyses.iterator().next() );

        aclTestUtils.checkHasAcl( analysis );
        aclTestUtils.checkLacksAces( analysis );
        aclTestUtils.checkHasAclParent( analysis, ee );

        long numVectors = expressionExperimentService.getDesignElementDataVectorCount( ee );
        assertEquals( 100L, numVectors );

        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
            assertFalse( rs.getResults().isEmpty() );
            assertTrue( rs.getResults().size() > 0 ); // for unclear reasons sometimes this is 100, 10 or 99. It's something test-specific.
        }

        /*
         * Exercise the matrix output services.
         */
        // avoid adding annotations for genes, it confuses the reader.
        for ( ArrayDesign ad : expressionExperimentService.getArrayDesignsUsed( ee ) ) {
            this.arrayDesignAnnotationService.deleteExistingFiles( ad );
        }
        Collection<File> outputLocations = expressionDataFileService.writeOrLocateDiffExpressionDataFiles( ee, true );

        assertEquals( 1, outputLocations.size() );

        File outputLocation = outputLocations.iterator().next();

        // NOte that this reader generally won't work for experiment files because of the gene annotations.
        DoubleMatrixReader r = new DoubleMatrixReader();

        assertTrue( outputLocation.canRead() );

        DoubleMatrix<String, String> readIn = r.read( outputLocation.getAbsolutePath() );

        assertTrue( readIn.rows() > 0 );
        assertEquals( 9, readIn.columns() );

        expressionDataFileService.deleteAllFiles( ee );

        // / remove the analysis
        int numDeleted = differentialExpressionAnalyzerService.deleteAnalyses( ee );
        assertTrue( numDeleted > 0 );

    }

    /**
     * Test inspired by bug 2605
     */
    @Test
    public void testAnalyzeWithSubsetWhenOneIsNotUsableAndWithInteractionInTheOther() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE32136", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            //noinspection unchecked
            ee = ( ( Collection<ExpressionExperiment> ) e.getData() ).iterator().next();
            assumeNoException( e );
        }
        processedDataVectorService.createProcessedDataVectors( ee );

        ee = expressionExperimentService.thawLite( ee );
        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();

        for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
            experimentalFactorService.delete( experimentalFactor );
        }

        ee = expressionExperimentService.thawLite( ee );

        try ( InputStream is = this.getClass()
                .getResourceAsStream( "/data/loader/expression/geo/GSE32136.design.txt" ) ) {
            assertNotNull( is );
            experimentalDesignImporter.importDesign( ee, is );
        }
        experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        assertEquals( 3, experimentalFactors.size() );
        differentialExpressionAnalyzerService.deleteAnalyses( ee );

        // Done with setting it up.

        Collection<ExperimentalFactor> factors = new HashSet<>();
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

        HashSet<Collection<ExperimentalFactor>> ifacts = new HashSet<>();
        ifacts.add( factors );
        config.setInteractionsToInclude( ifacts );

        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );

        assertEquals( "Should have quietly ignored one of the subsets that is not analyzable", 1, analyses.size() );

        DifferentialExpressionAnalysis analysis = analyses.iterator().next();
        assertEquals( "Subsetting was not done correctly", subsetFactor,
                analysis.getSubsetFactorValue().getExperimentalFactor() );
        // FIXME: use an assertion here, see https://github.com/PavlidisLab/Gemma/issues/419
        assertEquals( "Interaction was not retained in the analyzed subset", 3, analysis.getResultSets().size() );

        ExpressionExperimentSubSet eeset = ( ExpressionExperimentSubSet ) analysis.getExperimentAnalyzed();

        aclTestUtils.checkEESubSetAcls( eeset );
        aclTestUtils.checkHasAcl( analysis );
        aclTestUtils.checkLacksAces( eeset );
        aclTestUtils.checkLacksAces( analysis );

        // check that we read it back correctly.
        {
            Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> vos = differentialExpressionAnalysisService
                    .getAnalysesByExperiment( Collections.singleton( ee.getId() ) );
            // it will retrieve the analysis of the subset.
            assertEquals( 1, vos.size() );
        }

        // retrieve the analysis of the subset directly.
        {
            Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> vos = differentialExpressionAnalysisService
                    .getAnalysesByExperiment( Collections.singleton( eeset.getId() ) );
            assertEquals( 1, vos.size() );
            for ( DifferentialExpressionAnalysisValueObject vo : vos.entrySet().iterator().next().getValue() ) {
                assertNotNull( vo.getSubsetFactorValue() );
            }
        }

    }

    @Test
    public void testWritePValuesHistogram() throws Exception {
        prepareGSE1611();
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();
        config.setFactorsToInclude( factors );
        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );
        for ( DifferentialExpressionAnalysis analysis : analyses ) {
            analysis = differentialExpressionAnalysisService.thaw( analysis );
            for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
                Histogram hist = resultService.loadPvalueDistribution( resultSet.getId() );
                assertNotNull( hist );
            }
        }

    }

    private void prepareGSE1611() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gds994Short" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE1611", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            //noinspection unchecked
            ee = ( ( Collection<ExpressionExperiment> ) e.getData() ).iterator().next();
            throw e;
        }

        assertEquals( 2, ee.getExperimentalDesign().getExperimentalFactors().size() );

        Set<ProcessedExpressionDataVector> vectors = processedDataVectorService.createProcessedDataVectors( ee );
        assertEquals( 100, vectors.size() );
        ee = expressionExperimentService.thawLite( ee );
        assertEquals( 100, ee.getNumberOfDataVectors().intValue() );
        differentialExpressionAnalyzerService.deleteAnalyses( ee );

        for ( BioAssay ba : ee.getBioAssays() ) {
            BioMaterial bm = ba.getSampleUsed();
            assertEquals( bm + " " + ba, 2, bm.getFactorValues().size() );
        }
    }
}
