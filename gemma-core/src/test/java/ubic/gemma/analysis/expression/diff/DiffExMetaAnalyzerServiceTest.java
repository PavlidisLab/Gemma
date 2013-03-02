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
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
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
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
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
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = analyzerService.analyze( analysisResultSetIds );
        assertNotNull( metaAnalysis );
        assertEquals( 3, metaAnalysis.getResultSetsIncluded().size() );

        // for upregulated genes, length(which (p.adjust(apply(tup, 1, function(x) 1 -
        // pchisq(-2*sum(log(x)), 2*length(x)) ), method="BH") < 0.1))

        int numUp = 0;
        int numDown = 0;
        int foundTests = 0;

        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
            assertTrue( r.getMetaPvalue() <= 1.0 && r.getMetaPvalue() >= 0.0 );

            String gene = r.getGene().getOfficialSymbol();

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
            } else if ( gene.equals( "ABCF1" ) ) {
                foundTests++;
                assertEquals( logComponentResults( r, gene ), 0.01664992, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "ACLY" ) ) {
                foundTests++;
                assertEquals( logComponentResults( r, gene ), 1.505811e-06, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "ACTA2" ) ) {
                foundTests++;
                assertEquals( logComponentResults( r, gene ), 0.0002415006, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "ACO2" ) ) {
                foundTests++;
                assertEquals( logComponentResults( r, gene ), 0.002218716, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "THRA" ) ) {
                foundTests++;
                assertTrue( !r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 0.007901338, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "PPM1G" ) ) {
                foundTests++;
                assertTrue( !r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 0.001611389, r.getMetaPvalue(), 0.00001 );
            } else if ( gene.equals( "SEPW1" ) ) {
                foundTests++;
                assertTrue( r.getUpperTail() );
                assertEquals( logComponentResults( r, gene ), 0.006142644, r.getMetaPvalue(), 0.0001 );
            } else if ( gene.equals( "GUK1" ) ) {
                foundTests++;
                assertEquals( logComponentResults( r, gene ), 2.866101e-06, r.getMetaPvalue(), 1e-8 );
            } else if ( gene.equals( "KXD1" ) ) {
                foundTests++;
                assertTrue( r.getUpperTail() );
                assertEquals( 3.78401e-06, r.getMetaPvalue(), 1e-8 );
            }

            assertNotNull( r.getUpperTail() );

            if ( r.getUpperTail() ) {
                numUp++;
            } else {
                numDown++;
            }
        }

        // assertEquals( 10, foundTests );
        // assertEquals( 215, numUp ); // R, minus 4 that we skip as they are duplicated genes.
        // assertEquals( 101, numDown ); // R
        // assertEquals( 316, metaAnalysis.getResults().size() );

        /*
         * Test ancillary methods
         */
        metaAnalysis.setName( RandomStringUtils.random( 10 ) );
        metaAnalysis = analyzerService.persist( metaAnalysis );

        assertNotNull( metaAnalysis.getId() );

        Collection<GeneDifferentialExpressionMetaAnalysisSummaryValueObject> myMetaAnalyses = geneDiffExMetaAnalysisHelperService
                .loadAllMetaAnalyses();
        assertTrue( myMetaAnalyses.size() > 0 );

        GeneDifferentialExpressionMetaAnalysisDetailValueObject mdvo = geneDiffExMetaAnalysisHelperService
                .findDetailMetaAnalysisById( metaAnalysis.getId() );
        assertNotNull( mdvo );

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
        StringBuilder buf = new StringBuilder();
        for ( DifferentialExpressionAnalysisResult rr : r.getResultsUsed() ) {
            buf.append( String.format( "%s  %s fv=%d  p=%.4f t=%.2f", gene, rr.getProbe().getName(), rr.getContrasts()
                    .iterator().next().getFactorValue().getId(), rr.getPvalue(), rr.getContrasts().iterator().next()
                    .getCoefficient() )
                    + "\n" );
        }
        return buf.toString();
    }

    private String logFailure( GeneDifferentialExpressionMetaAnalysis metaAnalysis ) {
        StringBuilder buf = new StringBuilder();
        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
            buf.append( "----" );
            String gene = r.getGene().getOfficialSymbol();
            buf.append( logComponentResults( r, gene ) );
        }

        return buf.toString();
    }
}
