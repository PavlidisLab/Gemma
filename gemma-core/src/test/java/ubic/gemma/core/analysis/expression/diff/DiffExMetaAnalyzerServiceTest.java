/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.genome.gene.ExternalFileGeneLoaderService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.persistence.service.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.*;

/**
 * This is a test that requires complex setup: loading several data sets, information on genes, array design
 * annotations, conducting differential expression, and finally the meta-analysis. It's somewhat of a kitchensink test -
 * some non-meta-analysis related tests are included.
 *
 * @author Paul
 */
public class DiffExMetaAnalyzerServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private GeneDiffExMetaAnalysisService analysisService;

    @Autowired
    private DiffExMetaAnalyzerService analyzerService;

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private ExperimentalDesignImporter designImporter;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private ExternalDatabaseService edService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private ExternalFileGeneLoaderService externalFileGeneLoaderService;

    @Autowired
    private GeneDiffExMetaAnalysisHelperService geneDiffExMetaAnalysisHelperService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeoService geoService;

    private boolean loadedGenes = false;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Before
    public void before() throws Exception {
        this.cleanup();

        /*
         * Add genes.
         */
        if ( !loadedGenes ) {
            try ( InputStream geneFile = this.getClass().getResourceAsStream(
                    "/data/loader/expression/geo/meta-analysis/human.genes.subset.for.import.txt" ) ) {
                externalFileGeneLoaderService.load( geneFile, "human" );
                loadedGenes = true;
            }
        }

        // load three experiments; all have GDS's so they also get experimental designs.
        this.loadSet( "GSE2018" );
        this.loadSet( "GSE2111" );
        this.loadSet( "GSE6344" );

        this.addGenes();
    }

    @After
    public void teardown() {
        this.cleanup();
    }

    @Test
    @Ignore
    @Category(SlowTest.class)
    public void testAnalyze() throws Exception {

        ExpressionExperiment ds1 = experimentService.findByShortName( "GSE2018" );
        ExpressionExperiment ds2 = experimentService.findByShortName( "GSE6344" );
        ExpressionExperiment ds3 = experimentService.findByShortName( "GSE2111" );

        assertNotNull( ds1 );
        assertNotNull( ds2 );
        assertNotNull( ds3 );

        ds1 = experimentService.thaw( ds1 );
        ds2 = experimentService.thaw( ds2 );
        ds3 = experimentService.thaw( ds3 );

        processedExpressionDataVectorService.createProcessedDataVectors( ds1, true );
        processedExpressionDataVectorService.createProcessedDataVectors( ds2, true );
        processedExpressionDataVectorService.createProcessedDataVectors( ds3, true );

        /*
         * Delete the experimental design (which came with the GEO import) and reload. the new designs have been
         * modified to have just one factor with two levels. (The data sets have nothing to do with each other, it's
         * just a test)
         */
        for ( ExperimentalFactor ef : requireNonNull( ds1.getExperimentalDesign() ).getExperimentalFactors() ) {
            experimentalFactorService.remove( ef );

        }
        for ( ExperimentalFactor ef : requireNonNull( ds2.getExperimentalDesign() ).getExperimentalFactors() ) {
            experimentalFactorService.remove( ef );

        }
        for ( ExperimentalFactor ef : requireNonNull( ds3.getExperimentalDesign() ).getExperimentalFactors() ) {
            experimentalFactorService.remove( ef );
        }
        ds1.getExperimentalDesign().getExperimentalFactors().clear();
        ds2.getExperimentalDesign().getExperimentalFactors().clear();
        ds3.getExperimentalDesign().getExperimentalFactors().clear();

        experimentService.update( ds1 );
        experimentService.update( ds2 );
        experimentService.update( ds3 );

        ds1 = experimentService.load( ds1.getId() );
        assertNotNull( ds1 );
        ds1 = experimentService.thawLite( ds1 );
        ds2 = experimentService.load( ds2.getId() );
        assertNotNull( ds2 );
        ds2 = experimentService.thawLite( ds2 );
        ds3 = experimentService.load( ds3.getId() );
        assertNotNull( ds3 );
        ds3 = experimentService.thawLite( ds3 );

        designImporter.importDesign( ds1,
                requireNonNull( this.getClass().getResourceAsStream( "/data/loader/expression/geo/meta-analysis/gse2018.design.txt" ) ) );

        designImporter.importDesign( ds2,
                requireNonNull( this.getClass().getResourceAsStream( "/data/loader/expression/geo/meta-analysis/gse6344.design.txt" ) ) );

        designImporter.importDesign( ds3,
                requireNonNull( this.getClass().getResourceAsStream( "/data/loader/expression/geo/meta-analysis/gse2111.design.txt" ) ) );

        ds1 = experimentService.thawLite( ds1 );
        ds2 = experimentService.thawLite( ds2 );
        ds3 = experimentService.thawLite( ds3 );

        /*
         * Run differential analyses.
         */

        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds1, this.getConfig( ds1 ) );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds2, this.getConfig( ds2 ) );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds3, this.getConfig( ds3 ) );

        /*
         * Prepare for meta-analysis.
         */
        Collection<DifferentialExpressionAnalysis> ds1Analyses = differentialExpressionAnalysisService
                .findByExperiment( ds1, true );
        Collection<DifferentialExpressionAnalysis> ds2Analyses = differentialExpressionAnalysisService
                .findByExperiment( ds2, true );
        Collection<DifferentialExpressionAnalysis> ds3Analyses = differentialExpressionAnalysisService
                .findByExperiment( ds3, true );

        assertFalse( ds1Analyses.isEmpty() );
        assertFalse( ds2Analyses.isEmpty() );
        assertFalse( ds3Analyses.isEmpty() );

        ds1Analyses = differentialExpressionAnalysisService.thaw( ds1Analyses );
        ds2Analyses = differentialExpressionAnalysisService.thaw( ds2Analyses );
        ds3Analyses = differentialExpressionAnalysisService.thaw( ds3Analyses );

        ExpressionAnalysisResultSet rs1 = ds1Analyses.iterator().next().getResultSets().iterator().next();
        ExpressionAnalysisResultSet rs2 = ds2Analyses.iterator().next().getResultSets().iterator().next();
        ExpressionAnalysisResultSet rs3 = ds3Analyses.iterator().next().getResultSets().iterator().next();

        Collection<Long> analysisResultSetIds = new HashSet<>();
        analysisResultSetIds.add( rs1.getId() );
        analysisResultSetIds.add( rs2.getId() );
        analysisResultSetIds.add( rs3.getId() );

        /*
         * Perform the meta-analysis without saving it.
         */
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = analyzerService.analyze( analysisResultSetIds );
        assertNotNull( metaAnalysis );
        assertEquals( 3, metaAnalysis.getResultSetsIncluded().size() );
        this.testGenes( metaAnalysis );

        /*
         * Test ancillary methods
         */
        metaAnalysis.setName( RandomStringUtils.insecure().next( 10 ) );
        metaAnalysis = analyzerService.persist( metaAnalysis );

        assertNotNull( metaAnalysis.getId() );

        /*
         * Test validity of stored analysis.
         */
        this.testAnalysis( metaAnalysis );

        // bug 3722
        analysisService.remove( metaAnalysis );

        /*
         * Kitchen sink extra tests.
         */
        this.extraTests1( ds1 );

        /*
         * More tests since we have a bunch of stuff loaded.
         */
        this.extraTests2( ds1, ds2, ds3 );

    }

    private void testGenes( GeneDifferentialExpressionMetaAnalysis metaAnalysis ) {
        int numUp = 0;
        int numDown = 0;
        int foundTests = 0;
        boolean[] found = new boolean[9];

        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
            assertTrue( r.getMetaPvalue() <= 1.0 && r.getMetaPvalue() >= 0.0 );

            String gene = r.getGene().getOfficialSymbol();

            switch ( gene ) {
                case "CAPRIN1":
                    foundTests++;
                    assertTrue( r.getUpperTail() );
                    assertEquals( this.logComponentResults( r, gene ), 0.003375654, r.getMetaPvalue(), 0.00001 );
                    found[0] = true;
                    break;
                case "ABCF1":
                    fail( "Should have gotten removed due to conflicting results" );
                    break;
                case "ACLY":
                    foundTests++;
                    found[1] = true;
                    assertEquals( this.logComponentResults( r, gene ), 1.505811e-06, r.getMetaPvalue(), 0.00001 );
                    break;
                case "ACTA2":
                    foundTests++;
                    found[2] = true;
                    assertEquals( this.logComponentResults( r, gene ), 0.0002415006, r.getMetaPvalue(), 0.00001 );
                    break;
                case "ACO2":
                    foundTests++;
                    found[3] = true;
                    assertEquals( this.logComponentResults( r, gene ), 0.003461225, r.getMetaPvalue(), 0.00001 );
                    break;
                case "THRA":
                    foundTests++;
                    found[4] = true;
                    assertFalse( r.getUpperTail() );
                    assertEquals( this.logComponentResults( r, gene ), 0.008188016, r.getMetaPvalue(), 0.00001 );
                    break;
                case "PPM1G":
                    foundTests++;
                    found[5] = true;
                    assertFalse( r.getUpperTail() );
                    assertEquals( this.logComponentResults( r, gene ), 0.001992656, r.getMetaPvalue(), 0.00001 );
                    break;
                case "SEPW1":
                    foundTests++;
                    found[6] = true;
                    assertTrue( r.getUpperTail() );
                    assertEquals( this.logComponentResults( r, gene ), 0.006142644, r.getMetaPvalue(), 0.0001 );
                    break;
                case "GUK1":
                    found[7] = true;
                    foundTests++;
                    assertEquals( this.logComponentResults( r, gene ), 1.65675107E-6, r.getMetaPvalue(), 1e-8 );
                    break;
                case "KXD1":
                    foundTests++;
                    found[8] = true;
                    assertTrue( r.getUpperTail() );
                    assertEquals( this.logComponentResults( r, gene ), 4.027476e-06, r.getMetaPvalue(), 1e-8 );
                    break;
                default:
            }

            assertNotNull( r.getUpperTail() );

            if ( r.getUpperTail() ) {
                numUp++;
            } else {
                numDown++;
            }
        }
        assertTrue( "Failed to find caprin1", found[0] );
        assertTrue( "Failed to find acly", found[1] );
        assertTrue( "Failed to find acta2", found[2] );
        assertTrue( "Failed to find aco2", found[3] );
        assertTrue( "Failed to find thra", found[4] );
        assertTrue( "Failed to find ppm1g", found[5] );
        assertTrue( "Failed to find sepw1", found[6] );
        assertTrue( "Failed to find guk1", found[7] );
        assertTrue( "Failed to find kxd1", found[8] );

        assertEquals( 230, numUp ); // R gives 235; minus 5 that we skip due to conflicting results.
        assertEquals( 91, numDown ); // R gives 96, minus 5

        assertEquals( 321, metaAnalysis.getResults().size() );

        assertEquals( 9, foundTests );
    }

    private void testAnalysis( GeneDifferentialExpressionMetaAnalysis metaAnalysis ) {
        Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> myMetaAnalyses = geneDiffExMetaAnalysisHelperService
                .loadAllMetaAnalyses();
        assertFalse( myMetaAnalyses.isEmpty() );
        for ( GeneDifferentialExpressionMetaAnalysisSummaryValueObject mvo : myMetaAnalyses ) {
            assertEquals( 3, mvo.getNumResultSetsIncluded().intValue() );
        }

        GeneDifferentialExpressionMetaAnalysisDetailValueObject mdvo = geneDiffExMetaAnalysisHelperService
                .findDetailMetaAnalysisById( metaAnalysis.getId() );
        assertNotNull( mdvo );

        for ( IncludedResultSetInfoValueObject gdemairsivo : mdvo.getIncludedResultSetsInfo() ) {
            this.differentialExpressionAnalysisService
                    .thawFully( requireNonNull( this.differentialExpressionAnalysisService.load( gdemairsivo.getAnalysisId() ) ) );
        }

        for ( GeneDifferentialExpressionMetaAnalysisResultValueObject vo : mdvo.getResults() ) {
            assertNotNull( vo.getMetaPvalue() );
            assertNotNull( vo.getGeneSymbol() );
        }
    }

    private void extraTests2( ExpressionExperiment ds1, ExpressionExperiment ds2, ExpressionExperiment ds3 ) {
        Collection<Gene> geneCollection = geneService.findByOfficialSymbol( "ACTA2" );
        assertFalse( geneCollection.isEmpty() );
        Gene g = geneCollection.iterator().next();

        assertNotNull( differentialExpressionResultService.findByGene( g, true ) );
        assertNotNull(
                differentialExpressionResultService.findByGeneAndExperimentAnalyzed( g, true, Arrays.asList( ds1, ds2, ds3 ), false ) );
        assertNotNull( differentialExpressionResultService
                .findByExperimentAnalyzed( Arrays.asList( ds1, ds2, ds3 ), false, 0.05, 10 ) );
        assertNotNull( differentialExpressionResultService.findByGene( g, true, 0.05, 10 ) );

        assertFalse( differentialExpressionResultService.findByGene( g, true ).isEmpty() );
        assertFalse( differentialExpressionResultService.findByGeneAndExperimentAnalyzed( g, true, Arrays.asList( ds1, ds2, ds3 ), false )
                .isEmpty() );
        assertFalse( differentialExpressionResultService
                .findByExperimentAnalyzed( Arrays.asList( ds1, ds2, ds3 ), false, 0.05, 10 ).isEmpty() );
        assertFalse( differentialExpressionResultService.findByGene( g, true, 0.05, 10 ).isEmpty() );

        Map<ExpressionExperimentDetailsValueObject, List<DifferentialExpressionAnalysisValueObject>> analysesByExperiment = differentialExpressionAnalysisService
                .getAnalysesByExperiment( IdentifiableUtils.getIds( Arrays.asList( ds1, ds2, ds3 ) ) );

        Collection<DiffExResultSetSummaryValueObject> resultSets = new HashSet<>();
        for ( ExpressionExperimentDetailsValueObject evo : analysesByExperiment.keySet() ) {
            for ( DifferentialExpressionAnalysisValueObject deavo : analysesByExperiment.get( evo ) ) {
                resultSets.addAll( deavo.getResultSets() );
            }
        }

        Map<Long, Map<Long, DiffExprGeneSearchResult>> ffResultSets = differentialExpressionResultService
                .findDiffExAnalysisResultIdsInResultSets( resultSets, Collections.singletonList( g.getId() ) );
        assertNotNull( ffResultSets );
        assertFalse( ffResultSets.isEmpty() );
    }

    private void extraTests1( ExpressionExperiment ds1 ) {
        Collection<Gene> geneCollection = geneService.findByOfficialSymbol( "ACTA2" );
        assertFalse( geneCollection.isEmpty() );
        Gene g = geneCollection.iterator().next();
        assertNotNull( g );
        long count = geneService.getCompositeSequenceCount( g, true );
        assertTrue( count != 0 );

        Collection<CompositeSequence> compSequences = geneService.getCompositeSequences( g, true );
        assertFalse( compSequences.isEmpty() );

        Collection<CompositeSequence> collection = compositeSequenceService.findByGene( g );
        assertEquals( 1, collection.size() );

        ArrayDesign ad = experimentService.getArrayDesignsUsed( ds1 ).iterator().next();
        collection = compositeSequenceService.findByGene( g, ad );
        assertEquals( 1, collection.size() );

        Collection<CompositeSequence> css = compositeSequenceService.findByName( "200974_at" );
        assertFalse( css.isEmpty() );
        CompositeSequence cs = css.iterator().next();
        Collection<Gene> genes = compositeSequenceService.getGenes( cs );
        assertEquals( 1, genes.size() );
        assertEquals( g, genes.iterator().next() );

        tableMaintenanceUtil.disableEmail();
        tableMaintenanceUtil.updateGene2CsEntries();

        Map<CompositeSequence, Collection<Gene>> gm = compositeSequenceService.getGenes( css );
        assertEquals( 1, gm.size() );
        assertEquals( g, gm.values().iterator().next().iterator().next() );
    }

    /**
     * Add gene annotations. Requires removing old sequence associations.
     */
    private void addGenes() throws Exception {
        // fill this in with whatever.
        ExternalDatabase genbank = edService.findByName( "genbank" );
        assert genbank != null;

        Taxon human = taxonService.findByCommonName( "human" );
        assert human != null;

        File annotationFile = new ClassPathResource( "/data/loader/expression/geo/meta-analysis/human.probes.for.import.txt" )
                .getFile();

        ArrayDesign gpl96 = arrayDesignService.findByShortName( "GPL96" );
        assertNotNull( gpl96 );

        ArrayDesign gpl97 = arrayDesignService.findByShortName( "GPL97" );
        assertNotNull( gpl97 );

        arrayDesignService.removeBiologicalCharacteristics( gpl96 );
        arrayDesignProbeMapperService.processArrayDesign( gpl96, human, annotationFile, genbank, false );

        arrayDesignService.removeBiologicalCharacteristics( gpl97 );
        arrayDesignProbeMapperService.processArrayDesign( gpl97, human, annotationFile, genbank, false );

        tableMaintenanceUtil.updateGene2CsEntries();
    }

    private void cleanup() {

        for ( GeneDifferentialExpressionMetaAnalysisSummaryValueObject vo : geneDiffExMetaAnalysisHelperService
                .loadAllMetaAnalyses() ) {
            analysisService.delete( vo.getId() );
        }

        this.deleteSet( "GSE2018" );
        this.deleteSet( "GSE2111" );
        this.deleteSet( "GSE6344" );

        ArrayDesign gpl96 = arrayDesignService.findByShortName( "GPL96" );
        ArrayDesign gpl97 = arrayDesignService.findByShortName( "GPL97" );
        if ( gpl96 != null ) {
            for ( ExpressionExperiment ee : arrayDesignService.getExpressionExperiments( gpl96 ) ) {
                experimentService.remove( ee );
            }

            arrayDesignService.remove( gpl96 );
        }

        if ( gpl97 != null ) {
            for ( ExpressionExperiment ee : arrayDesignService.getExpressionExperiments( gpl97 ) ) {
                experimentService.remove( ee );
            }
            arrayDesignService.remove( gpl97 );
        }

        geneService.removeAll();
    }

    private void deleteSet( String shortName ) {
        ExpressionExperiment set = experimentService.findByShortName( shortName );
        if ( set != null )
            experimentService.remove( set );

    }

    private DifferentialExpressionAnalysisConfig getConfig( ExpressionExperiment ee ) {
        DifferentialExpressionAnalysisConfig config1 = new DifferentialExpressionAnalysisConfig();
        assertNotNull( ee.getExperimentalDesign() );
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();
        config1.addFactorsToInclude( factors );
        config1.setModerateStatistics( false );
        return config1;
    }

    private void loadSet( String acc ) throws Exception {

        String path = new ClassPathResource( "/data/loader/expression/geo/meta-analysis" ).getFile()
                .getAbsolutePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path ) );

        try {
            geoService.fetchAndLoad( acc, false, true, false );
        } catch ( AlreadyExistsInSystemException ignored ) {
            log.info( "Set already exists in system." );
        }
    }

    private String logComponentResults( GeneDifferentialExpressionMetaAnalysisResult r, String gene ) {
        StringBuilder buf = new StringBuilder( "\n" );
        for ( DifferentialExpressionAnalysisResult rr : r.getResultsUsed() ) {
            buf.append( String.format( "%s  %s fv=%d  p=%.4g t=%.2f id=%d", gene, rr.getProbe().getName(),
                    rr.getContrasts().stream().findAny().map( ContrastResult::getFactorValue ).map( FactorValue::getId ).orElse( null ),
                    rr.getPvalue(), rr.getContrasts().iterator().next().getCoefficient(), rr.getId() ) ).append( "\n" );
        }
        return buf.toString();
    }

}
