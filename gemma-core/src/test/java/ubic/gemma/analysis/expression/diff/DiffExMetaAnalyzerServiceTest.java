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
package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.genome.gene.ExternalFileGeneLoaderService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResultValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.TableMaintenenceUtil;

/**
 * This is a test that requires complex setup: loading several data sets, information on genes, array design
 * annotations, conducting differential expression, and finally the meta-analysis.
 * 
 * @author Paul
 * @version $Id$
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
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private TableMaintenenceUtil tableMaintenenceUtil;

    @Before
    public void before() throws Exception {
        cleanup();

        /*
         * Add genes.
         */
        if ( !loadedGenes ) {
            InputStream geneFile = this.getClass().getResourceAsStream(
                    "/data/loader/expression/geo/meta-analysis/human.genes.subset.for.import.txt" );
            externalFileGeneLoaderService.load( geneFile, "human" );
            loadedGenes = true;
        }

        // load three experiments; all have GDS's so they also get experimental designs.
        loadSet( "GSE2018" );
        loadSet( "GSE2111" );
        loadSet( "GSE6344" );

        addGenes();
    }

    @After
    public void teardown() throws Exception {
        cleanup();
    }

    /**
     * @throws Exception
     */
    @Test
    public void testAnalyze() throws Exception {

        ExpressionExperiment ds1 = experimentService.findByShortName( "GSE2018" );
        ExpressionExperiment ds2 = experimentService.findByShortName( "GSE6344" );
        ExpressionExperiment ds3 = experimentService.findByShortName( "GSE2111" );

        assertNotNull( ds1 );
        assertNotNull( ds2 );
        assertNotNull( ds3 );

        ds1 = experimentService.thawLite( ds1 );
        ds2 = experimentService.thawLite( ds2 );
        ds3 = experimentService.thawLite( ds3 );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ds1 );
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ds2 );
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ds3 );

        /*
         * Delete the experimental design (which came with the GEO import) and reload. the new designs have been
         * modified to have just one factor with two levels. (The data sets have nothing to do with each other, it's
         * just a test)
         */
        for ( ExperimentalFactor ef : ds1.getExperimentalDesign().getExperimentalFactors() ) {
            experimentalFactorService.delete( ef );

        }
        for ( ExperimentalFactor ef : ds2.getExperimentalDesign().getExperimentalFactors() ) {
            experimentalFactorService.delete( ef );

        }
        for ( ExperimentalFactor ef : ds3.getExperimentalDesign().getExperimentalFactors() ) {
            experimentalFactorService.delete( ef );
        }
        ds1.getExperimentalDesign().getExperimentalFactors().clear();
        ds2.getExperimentalDesign().getExperimentalFactors().clear();
        ds3.getExperimentalDesign().getExperimentalFactors().clear();

        experimentService.update( ds1 );
        experimentService.update( ds2 );
        experimentService.update( ds3 );

        ds1 = experimentService.thawLite( experimentService.load( ds1.getId() ) );
        ds2 = experimentService.thawLite( experimentService.load( ds2.getId() ) );
        ds3 = experimentService.thawLite( experimentService.load( ds3.getId() ) ); // pain! fails sometimes.

        designImporter.importDesign( ds1,
                this.getClass().getResourceAsStream( "/data/loader/expression/geo/meta-analysis/gse2018.design.txt" ) );

        designImporter.importDesign( ds2,
                this.getClass().getResourceAsStream( "/data/loader/expression/geo/meta-analysis/gse6344.design.txt" ) );

        designImporter.importDesign( ds3,
                this.getClass().getResourceAsStream( "/data/loader/expression/geo/meta-analysis/gse2111.design.txt" ) );

        ds1 = experimentService.thawLite( ds1 );
        ds2 = experimentService.thawLite( ds2 );
        ds3 = experimentService.thawLite( ds3 );

        /*
         * Run differential analyses.
         */

        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds1, getConfig( ds1 ) );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds2, getConfig( ds2 ) );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds3, getConfig( ds3 ) );

        /*
         * Prepare for meta-analysis.
         */
        Collection<DifferentialExpressionAnalysis> ds1Analyses = differentialExpressionAnalysisService
                .findByInvestigation( ds1 );
        Collection<DifferentialExpressionAnalysis> ds2Analyses = differentialExpressionAnalysisService
                .findByInvestigation( ds2 );
        Collection<DifferentialExpressionAnalysis> ds3Analyses = differentialExpressionAnalysisService
                .findByInvestigation( ds3 );

        assertTrue( !ds1Analyses.isEmpty() );
        assertTrue( !ds2Analyses.isEmpty() );
        assertTrue( !ds3Analyses.isEmpty() );

        differentialExpressionAnalysisService.thaw( ds1Analyses );
        differentialExpressionAnalysisService.thaw( ds2Analyses );
        differentialExpressionAnalysisService.thaw( ds3Analyses );

        ExpressionAnalysisResultSet rs1 = ds1Analyses.iterator().next().getResultSets().iterator().next();
        ExpressionAnalysisResultSet rs2 = ds2Analyses.iterator().next().getResultSets().iterator().next();
        ExpressionAnalysisResultSet rs3 = ds3Analyses.iterator().next().getResultSets().iterator().next();

        Collection<Long> analysisResultSetIds = new HashSet<Long>();
        analysisResultSetIds.add( rs1.getId() );
        analysisResultSetIds.add( rs2.getId() );
        analysisResultSetIds.add( rs3.getId() );

        /*
         * Perform the meta-analysis without saving it.
         */
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = analyzerService.analyze( analysisResultSetIds );
        assertNotNull( metaAnalysis );
        assertEquals( 3, metaAnalysis.getResultSetsIncluded().size() );

        // for upregulated genes, length(which (p.adjust(apply(tup, 1, function(x) 1 -
        // pchisq(-2*sum(log(x)), 2*length(x)) ), method="BH") < 0.1))

        int numUp = 0;
        int numDown = 0;
        int foundTests = 0;
        boolean foundcaprin1 = false, foundacly = false, foundacta2 = false, foundaco2 = false, foundthra = false, foundppm1g = false, foundSep21 = false, foundGuk1 = false, foundKxd1 = false;

        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
            assertTrue( r.getMetaPvalue() <= 1.0 && r.getMetaPvalue() >= 0.0 );

            String gene = r.getGene().getOfficialSymbol();
            // log.info( logComponentResults( r, gene ));
            /*
             * GSE6575, GSE7329
             * 
             * experimentId=377&analysisId=17300&factorName=Sex
             * experimentId=772&analysisId=18935&factorName=PooledDiseaseState
             */

            // these pvalues are computed in R. For example:
            /*
             * apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["TCEB2"]
             */

            if ( gene.equals( "CAPRIN1" ) ) {
                foundTests++;
                assertTrue( r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 0.003375654, r.getMetaPvalue(), 0.00001 );
                foundcaprin1 = true;
            } else if ( gene.equals( "ABCF1" ) ) {
                fail( "Should have gotten removed due to conflicting results" );
            } else if ( gene.equals( "ACLY" ) ) {
                foundTests++;
                foundacly = true;
                assertEquals( logComponentResults( r, gene ), 1.505811e-06, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "ACTA2" ) ) {
                foundTests++;
                foundacta2 = true;
                assertEquals( logComponentResults( r, gene ), 0.0002415006, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "ACO2" ) ) {
                foundTests++;
                foundaco2 = true;
                assertEquals( logComponentResults( r, gene ), 0.003461225, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "THRA" ) ) {
                foundTests++;
                foundthra = true;
                assertTrue( !r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 0.008188016, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "PPM1G" ) ) {
                foundTests++;
                foundppm1g = true;
                assertTrue( !r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 0.001992656, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "SEPW1" ) ) {
                foundTests++;
                foundSep21 = true;
                assertTrue( r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 0.006142644, r.getMetaPvalue(), 0.0001 );
            } else if ( gene.equals( "GUK1" ) ) {
                foundGuk1 = true;
                foundTests++;
                assertEquals( logComponentResults( r, gene ), 2.820089e-06, r.getMetaPvalue(), 1e-8 );
            } else if ( gene.equals( "KXD1" ) ) {
                foundTests++;
                foundKxd1 = true;
                assertTrue( r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 4.027476e-06, r.getMetaPvalue(), 1e-8 );
            }

            assertNotNull( r.getUpperTail() );

            if ( r.getUpperTail() ) {
                numUp++;
            } else {
                numDown++;
            }
        }
        assertTrue( "Failed to find caprin1", foundcaprin1 );
        assertTrue( "Failed to find acly", foundacly );
        assertTrue( "Failed to find acta2", foundacta2 );
        assertTrue( "Failed to find guk1", foundGuk1 );
        assertTrue( "Failed to find kxd1", foundKxd1 );
        assertTrue( "Failed to find sep21", foundSep21 );
        assertTrue( "Failed to find ppm1g", foundppm1g );
        assertTrue( "Failed to find thra", foundthra );
        assertTrue( "Failed to find aco2", foundaco2 );

        assertEquals( 230, numUp ); // R gives 235; minus 5 that we skip due to conflicting results.
        assertEquals( 91, numDown ); // R gives 96, minus 5

        assertEquals( 321, metaAnalysis.getResults().size() );

        assertEquals( 9, foundTests );

        /*
         * Test ancillary methods
         */
        metaAnalysis.setName( RandomStringUtils.random( 10 ) );
        metaAnalysis = analyzerService.persist( metaAnalysis );

        assertNotNull( metaAnalysis.getId() );

        /*
         * Test validity of stored analysis.
         */
        Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> myMetaAnalyses = geneDiffExMetaAnalysisHelperService
                .loadAllMetaAnalyses();
        assertTrue( myMetaAnalyses.size() > 0 );
        for ( GeneDifferentialExpressionMetaAnalysisSummaryValueObject mvo : myMetaAnalyses ) {
            assertEquals( 3, mvo.getNumResultSetsIncluded().intValue() );
        }

        GeneDifferentialExpressionMetaAnalysisDetailValueObject mdvo = geneDiffExMetaAnalysisHelperService
                .findDetailMetaAnalysisById( metaAnalysis.getId() );
        assertNotNull( mdvo );

        for ( GeneDifferentialExpressionMetaAnalysisIncludedResultSetInfoValueObject gdemairsivo : mdvo
                .getIncludedResultSetsInfo() ) {
            DifferentialExpressionAnalysis thawedAnalysis = this.differentialExpressionAnalysisService
                    .thawFully( this.differentialExpressionAnalysisService.load( gdemairsivo.getAnalysisId() ) );
            // see bug 3365 for policy on storing results that are part of a meta-analysis.
            for ( ExpressionAnalysisResultSet rs : thawedAnalysis.getResultSets() ) {
                assertEquals( 1.0, rs.getQvalueThresholdForStorage(), 0.0001 );
            }
        }

        for ( GeneDifferentialExpressionMetaAnalysisResultValueObject vo : mdvo.getResults() ) {
            assertNotNull( vo.getMetaPvalue() );
            assertNotNull( vo.getGeneSymbol() );
        }

        // this is a little extra test, related to bug 3341
        differentialExpressionResultService.thaw( rs1 );

        Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> factorsByResultMap = differentialExpressionResultService
                .getExperimentalFactors( rs1.getResults() );
        assertTrue( factorsByResultMap.keySet().containsAll( rs1.getResults() ) );

    }

    /**
     * Add gene annotations. Requires removing old sequence associations.
     * 
     * @throws Exception
     */
    private void addGenes() throws Exception {
        // fill this in with whatever.
        ExternalDatabase genbank = edService.find( "genbank" );
        assert genbank != null;

        Taxon human = taxonService.findByCommonName( "human" );
        assert human != null;

        File annotationFile = new File( this.getClass()
                .getResource( "/data/loader/expression/geo/meta-analysis/human.probes.for.import.txt" ).toURI() );

        ArrayDesign gpl96 = arrayDesignService.findByShortName( "GPL96" );
        assertNotNull( gpl96 );

        ArrayDesign gpl97 = arrayDesignService.findByShortName( "GPL97" );
        assertNotNull( gpl97 );

        arrayDesignService.removeBiologicalCharacteristics( gpl96 );
        arrayDesignProbeMapperService.processArrayDesign( gpl96, human, annotationFile, genbank, false );

        arrayDesignService.removeBiologicalCharacteristics( gpl97 );
        arrayDesignProbeMapperService.processArrayDesign( gpl97, human, annotationFile, genbank, false );

        tableMaintenenceUtil.updateGene2CsEntries();
    }

    private void cleanup() {

        for ( GeneDifferentialExpressionMetaAnalysisSummaryValueObject vo : geneDiffExMetaAnalysisHelperService
                .loadAllMetaAnalyses() ) {
            analysisService.delete( vo.getId() );
        }

        deleteSet( "GSE2018" );
        deleteSet( "GSE2111" );
        deleteSet( "GSE6344" );

        ArrayDesign gpl96 = arrayDesignService.findByShortName( "GPL96" );
        ArrayDesign gpl97 = arrayDesignService.findByShortName( "GPL97" );
        if ( gpl96 != null ) {
            for ( ExpressionExperiment ee : arrayDesignService.getExpressionExperiments( gpl96 ) ) {
                experimentService.delete( ee );
            }

            arrayDesignService.remove( gpl96 );
        }

        if ( gpl97 != null ) {
            for ( ExpressionExperiment ee : arrayDesignService.getExpressionExperiments( gpl97 ) ) {
                experimentService.delete( ee );
            }
            arrayDesignService.remove( gpl97 );
        }

        Collection<Gene> genes = geneService.loadAll();
        for ( Gene gene : genes ) {
            try {
                geneService.remove( gene );
            } catch ( Exception e ) {

            }
        }

    }

    /**
     * @param shortName
     */
    private void deleteSet( String shortName ) {
        ExpressionExperiment set = experimentService.findByShortName( shortName );
        if ( set != null ) experimentService.delete( set );

    }

    /**
     * @param ee
     * @return
     */
    private DifferentialExpressionAnalysisConfig getConfig( ExpressionExperiment ee ) {
        DifferentialExpressionAnalysisConfig config1 = new DifferentialExpressionAnalysisConfig();
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();
        config1.setFactorsToInclude( factors );
        config1.setQvalueThreshold( 0.05 ); // realistic
        return config1;
    }

    /**
     * @param acc
     * @return
     */
    private Collection<?> loadSet( String acc ) throws Exception {

        String path = new File( this.getClass().getResource( "/data/loader/expression/geo/meta-analysis" ).toURI() )
                .getAbsolutePath();

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path ) );

        try {
            return geoService.fetchAndLoad( acc, false, true, false, false );
        } catch ( AlreadyExistsInSystemException e ) {

            return null;
        }

    }

    /**
     * @param r
     * @param gene
     * @return details
     */
    private String logComponentResults( GeneDifferentialExpressionMetaAnalysisResult r, String gene ) {
        StringBuilder buf = new StringBuilder( "\n" );
        for ( DifferentialExpressionAnalysisResult rr : r.getResultsUsed() ) {
            buf.append( String.format( "%s  %s fv=%d  p=%.4g t=%.2f id=%d", gene, rr.getProbe().getName(), rr
                    .getContrasts().iterator().next().getFactorValue().getId(), rr.getPvalue(), rr.getContrasts()
                    .iterator().next().getCoefficient(), rr.getId() )
                    + "\n" );
        }
        return buf.toString();
    }

    // private String logFailure( GeneDifferentialExpressionMetaAnalysis metaAnalysis ) {
    // StringBuilder buf = new StringBuilder();
    // for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
    // buf.append( "----" );
    // String gene = r.getGene().getOfficialSymbol();
    // buf.append( logComponentResults( r, gene ) );
    // }
    //
    // return buf.toString();
    // }
}
