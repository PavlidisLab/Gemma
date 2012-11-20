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

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
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
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisHelperService;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.TableMaintenenceUtil;
import ubic.gemma.util.ConfigUtils;

/**
 * Currently this test requires the 'test' miniGemma DB.
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
    private GeoService geoService;

    private boolean loadedGenes = false;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private TableMaintenenceUtil tableMaintenenceUtil;

    public void after() {

        for ( GeneDifferentialExpressionMetaAnalysisSummaryValueObject vo : geneDiffExMetaAnalysisHelperService
                .getMyMetaAnalyses() ) {
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

    }

    @Before
    public void before() throws Exception {
        after(); // in case.

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
         * Delete the experimental design and reload. the new designs have been modified to have just one factor with
         * two levels. (The data sets have nothing to do with each other, it's just a test)
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

        ds1 = experimentService.thawLite( ds1 );
        ds2 = experimentService.thawLite( ds2 );
        ds3 = experimentService.thawLite( ds3 );

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
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds1, ds1.getExperimentalDesign()
                .getExperimentalFactors() );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds2, ds2.getExperimentalDesign()
                .getExperimentalFactors() );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds3, ds3.getExperimentalDesign()
                .getExperimentalFactors() );

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
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = analyzerService
                .analyze( analysisResultSetIds, null, null );
        assertNotNull( metaAnalysis );
        assertEquals( 3, metaAnalysis.getResultSetsIncluded().size() );

        // for upregulated genes, length(which (p.adjust(apply(tup, 1, function(x) 1 -
        // pchisq(-2*sum(log(x)), 2*length(x)) ), method="BY") < 0.1))

        // get 41.

        int numUp = 0;
        int numDown = 0;
        int foundTests = 0;
        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
            assertTrue( r.getMetaPvalue() <= 1.0 && r.getMetaPvalue() >= 0.0 );

            String gene = r.getGene().getOfficialSymbol();
            // System.err.println( gene + "\t" + r.getMetaPvalue() + "\t" + r.getMetaQvalue() + "\t"
            // + r.getMeanLogFoldChange() );

            // these pvalues are computed in R. For example ... (this doesn't take into account the clipping we do, but
            // that's done to the data before we entered into R)
            /*
             * apply(tdw, 1, function(x) 1 - pchisq(-2*sum(log(x)), 2*length(x)) )["TCEB2"]
             */
            if ( gene.equals( "ACLY" ) ) {

                logComponentResults( r, gene );

                foundTests++;
                assertEquals( 3.25e-6, r.getMetaPvalue(), 0.001 );
                log.debug( "----" );

            } else if ( gene.equals( "ABCF1" ) ) {
                logComponentResults( r, gene );

                foundTests++;
                assertEquals( 0.0006160855, r.getMetaPvalue(), 0.001 );
                log.debug( "----" );

            } else if ( gene.equals( "TCEB2" ) ) {

                logComponentResults( r, gene );
                foundTests++;
                assertTrue( r.getMeanLogFoldChange() < 0 );
                assertEquals( 1.261979e-02, r.getMetaPvalue(), 0.001 );

                log.debug( "----" );

            } else if ( gene.equals( "SLC2A1" ) ) {
                logComponentResults( r, gene );
                foundTests++;
                assertTrue( r.getMeanLogFoldChange() < 0 );
                assertEquals( 0.002368357, r.getMetaPvalue(), 0.001 );

                log.debug( "----" );

            } else if ( gene.equals( "SEPW1" ) ) {
                logComponentResults( r, gene );
                foundTests++;
                assertEquals( 1.038296e-02, r.getMetaPvalue(), 0.001 );

                log.debug( "----" );

            } else if ( gene.equals( "SSR2" ) ) {
                logComponentResults( r, gene );
                foundTests++;
                assertEquals( 0.0006207671, r.getMetaPvalue(), 0.001 );

                log.debug( "----" );
            } else if ( gene.equals( "PDHA1" ) ) {
                logComponentResults( r, gene );
                foundTests++;
                assertEquals( 4.543676e-06, r.getMetaPvalue(), 0.001 );

                log.debug( "----" );

            }

            if ( r.getMeanLogFoldChange() > 0 ) {
                numUp++;

            } else {
                numDown++;
            }
        }

        assertEquals( 7, foundTests );
        assertEquals( 74, numUp ); // R agrees
        assertEquals( 201, numDown ); // R says 202, close enough.
        assertEquals( 275, metaAnalysis.getResults().size() );

        /*
         * Test ancillary methods
         */
        metaAnalysis = analysisService.create( metaAnalysis );

        assertNotNull( metaAnalysis.getId() );

        Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> myMetaAnalyses = geneDiffExMetaAnalysisHelperService
                .getMyMetaAnalyses();
        assertTrue( myMetaAnalyses.size() > 0 );

        GeneDifferentialExpressionMetaAnalysisDetailValueObject mdvo = geneDiffExMetaAnalysisHelperService
                .getMetaAnalysis( metaAnalysis.getId() );
        assertNotNull( mdvo );

    }

    /**
     * @param r
     * @param gene
     */
    private void logComponentResults( GeneDifferentialExpressionMetaAnalysisResult r, String gene ) {
        if ( !log.isDebugEnabled() ) return;
        for ( DifferentialExpressionAnalysisResult rr : r.getResultsUsed() ) {
            log.debug( String.format( "%s  %s fv=%d  p=%.4f t=%.2f", gene, rr.getProbe().getName(), rr.getContrasts()
                    .iterator().next().getFactorValue().getId(), rr.getPvalue(), rr.getContrasts().iterator().next()
                    .getCoefficient() ) );
        }
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

    /**
     * @param shortName
     */
    private void deleteSet( String shortName ) {
        ExpressionExperiment set = experimentService.findByShortName( shortName );
        if ( set != null ) experimentService.delete( set );

    }

    /**
     * @param acc
     * @return
     */
    private Collection<?> loadSet( String acc ) {
        String path = ConfigUtils.getString( "gemma.home" );

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + File.separator + "meta-analysis" ) );

        try {
            return geoService.fetchAndLoad( acc, false, true, false, false );
        } catch ( AlreadyExistsInSystemException e ) {

            return null;
        }

    }
}
