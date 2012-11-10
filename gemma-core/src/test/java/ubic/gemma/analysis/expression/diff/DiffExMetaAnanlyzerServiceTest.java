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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.genome.gene.ExternalFileGeneLoaderService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
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
public class DiffExMetaAnanlyzerServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private DiffExMetaAnalyzerService analyzerService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService;

    @Autowired
    private ExternalFileGeneLoaderService externalFileGeneLoaderService;

    @Autowired
    private ExternalDatabaseService edService;

    @Autowired
    private TableMaintenenceUtil tableMaintenenceUtil;

    private boolean loadedGenes = false;

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

        // fill this in with whatever.
        ExternalDatabase genbank = edService.find( "genbank" );
        assert genbank != null;

        Taxon human = taxonService.findByCommonName( "human" );
        assert human != null;

        /*
         * Add gene annotations. Requires removing old sequence associations.
         */

        File annotationFile = new File( this.getClass()
                .getResource( "/data/loader/expression/geo/meta-analysis/human.probes.for.import.txt" ).toURI() );

        arrayDesignService.removeBiologicalCharacteristics( arrayDesignService.findByShortName( "GPL96" ) );

        arrayDesignService.removeBiologicalCharacteristics( arrayDesignService.findByShortName( "GPL97" ) );

        arrayDesignProbeMapperService.processArrayDesign( arrayDesignService.findByShortName( "GPL96" ), human,
                annotationFile, genbank, false );
        arrayDesignProbeMapperService.processArrayDesign( arrayDesignService.findByShortName( "GPL97" ), human,
                annotationFile, genbank, false );

        tableMaintenenceUtil.updateGene2CsEntries();
    }

    @After
    public void after() {
        deleteSet( "GSE2018" );
        deleteSet( "GSE2111" );
        deleteSet( "GSE6344" );
    }

    private void deleteSet( String shortName ) {
        ExpressionExperiment set = experimentService.findByShortName( shortName );
        if ( set != null ) experimentService.delete( set );

    }

    @Test
    public void testAnalyze() {

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
         * Add experimental designs if needed
         */
        //

        /*
         * Run differential analyses.
         */
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds1, ds1.getExperimentalDesign()
                .getExperimentalFactors() );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds2, ds2.getExperimentalDesign()
                .getExperimentalFactors() );
        differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ds3, ds3.getExperimentalDesign()
                .getExperimentalFactors() );

        // ready for test.
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
		GeneDifferentialExpressionMetaAnalysis metaAnalysis = analyzerService.analyze( analysisResultSetIds, null, null );
        assertNotNull( metaAnalysis );
        assertEquals( 3, metaAnalysis.getResultSetsIncluded().size() );

        // not checked by hand
        assertEquals( 48, metaAnalysis.getResults().size() );

        for ( GeneDifferentialExpressionMetaAnalysisResult r : metaAnalysis.getResults() ) {
            assertTrue( r.getMetaPvalue() <= 1.0 && r.getMetaPvalue() >= 0.0 );
        }

    }

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
