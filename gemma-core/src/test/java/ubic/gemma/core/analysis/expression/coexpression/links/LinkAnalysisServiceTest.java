/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.analysis.expression.coexpression.links;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisConfig.SingularThreshold;
import ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionException;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionCache;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionValueObject;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author paul
 */
public class LinkAnalysisServiceTest extends BaseSpringContextTest {

    private final FilterConfig filterConfig = new FilterConfig();
    private final LinkAnalysisConfig linkAnalysisConfig = new LinkAnalysisConfig();

    private ExpressionExperiment ee;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private CoexpressionCache gene2GeneCoexpressionCache;

    @Autowired
    private CoexpressionService geneCoexpressionService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private LinkAnalysisPersister linkAnalysisPersisterService;

    @Autowired
    private LinkAnalysisService linkAnalysisService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Before
    public void setUp() throws Exception {
        super.setTestCollectionSize( 100 );
        gene2GeneCoexpressionCache.shutdown();
    }

    @After
    public void tearDown() {
        super.resetTestCollectionSize();
    }

    @Test
    @Category(SlowTest.class)
    public void testLoadAnalyzeSaveAndCoexpSearch() throws QuantitationTypeConversionException {
        ee = this.getTestPersistentCompleteExpressionExperimentWithSequences();
        processedExpressionDataVectorService.createProcessedDataVectors( ee, true );

        tableMaintenanceUtil.disableEmail();
        tableMaintenanceUtil.updateGene2CsEntries();
        linkAnalysisConfig.setCdfCut( 0.1 );
        linkAnalysisConfig.setSingularThreshold( SingularThreshold.cdfcut );
        linkAnalysisConfig.setProbeDegreeThreshold( 25 );
        linkAnalysisConfig.setCheckCorrelationDistribution( false );
        linkAnalysisConfig.setCheckForBatchEffect( false );
        filterConfig.setIgnoreMinimumSampleThreshold( true );

        // first time.
        //noinspection UnusedAssignment // we still want to do this for the testing sake
        LinkAnalysis la = linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );

        // test remove is clean; to check this properly requires checking the db.
        linkAnalysisPersisterService.deleteAnalyses( ee );

        this.checkUnsupportedLinksHaveNoSupport();
        assertEquals( 0, geneCoexpressionService.getCoexpression( ee, true ).size() );

        la = linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );

        CoexpressionAnalysis analysisObj = la.getAnalysisObj();
        assertEquals( 161, analysisObj.getNumberOfElementsAnalyzed().intValue() );
        assertTrue( analysisObj.getNumberOfLinks() > 0 );

        assertNotNull( analysisObj.getCoexpCorrelationDistribution() );

        Collection<ExpressionExperiment> ees = new HashSet<>();
        ees.add( ee );

        this.updateNodeDegree();
        int totalLinksFirstPass = this.checkResults( ees, 1 );

        // should be ~1140.
        assertTrue( totalLinksFirstPass > 1000 );

        // test redo
        linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );
        this.updateNodeDegree();

        int totalLinksRedo = this.checkResults( ees, 1 );
        assertEquals( totalLinksFirstPass, totalLinksRedo );

        // now add another experiment that has overlapping links (same data...
        Map<CompositeSequence, byte[]> dataMap = new HashMap<>();
        ee = eeService.thaw( ee );
        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            dataMap.put( v.getDesignElement(), v.getData() );
        }

        ExpressionExperiment ee2 = this.getTestPersistentCompleteExpressionExperimentWithSequences( ee );
        //eeService.thawRawAndProcessed( ee2 );
        for ( RawExpressionDataVector v : ee2.getRawExpressionDataVectors() ) {
            assert dataMap.get( v.getDesignElement() ) != null;
            v.setData( dataMap.get( v.getDesignElement() ) );
        }

        eeService.update( ee2 );

        processedExpressionDataVectorService.createProcessedDataVectors( ee2, true );
        linkAnalysisService.process( ee2, filterConfig, linkAnalysisConfig );

        this.updateNodeDegree();

        // expect to get at least one links with support >1
        ees.add( ee2 );
        this.checkResults( ees, 2 );

    }

    private void checkUnsupportedLinksHaveNoSupport() {
        JdbcTemplate jt = getJdbcTemplate();

        // see SupportDetailsTest for validation that these strings represent empty byte arrays. I think the 1 at
        // position 12 is important.
        final Collection<Long> checkme = new HashSet<>();
        // maybe these patterns aren't this reproducible.
        jt.query(
                // "SELECT ID from MOUSE_LINK_SUPPORT_DETAILS WHERE HEX(BYTES) in ('0000000200000001000000000000000200000000',"
                // + " '000006AA00000001000000000000003600000000', '0000000000000001000000000000000000000000',"
                // + "'0000003E00000001000000000000000200000000','0000003F00000001000000000000000200000000',"
                // + "'0000000500000001000000000000000200000000')", new RowCallbackHandler() {

                // 000002BB00000001000000000000001600000000
                "SELECT ID FROM MOUSE_LINK_SUPPORT_DETAILS WHERE HEX(BYTES) LIKE '00000___0000000100000000000000%'",
                new RowCallbackHandler() {

                    @Override
                    public void processRow( ResultSet rs ) throws SQLException {
                        Long id = rs.getLong( 1 );
                        checkme.add( id );
                    }
                } );

        // we should definitely have some of these
        assertTrue( checkme.size() > 0 );

        jt.query( "SELECT SUPPORT FROM MOUSE_GENE_COEXPRESSION WHERE SUPPORT_DETAILS_FK IN (?) AND SUPPORT > 0",
                new Object[] { checkme.toArray() }, new RowCallbackHandler() {
                    @Override
                    public void processRow( ResultSet rs ) {
                        fail( "Should not have had any rows" );
                    }
                } );

    }

    private void checkResult( CoexpressionValueObject coex ) {
        assertNotNull( coex.toString(), coex.getQueryGeneId() );
        assertNotNull( coex.toString(), coex.getCoexGeneId() );
        assertNotNull( coex.toString(), coex.getSupportDetailsId() );
        assertNotNull( coex.toString(), coex.getSupportingDatasets() );
        assertTrue( coex.toString(), coex.getNumDatasetsSupporting() > 0 );
        assertTrue( coex.toString(), coex.getNumDatasetsTestedIn() != 0 );

        // assertNotNull( coex.toString(), coex.getTestedInDatasets() );

        if ( coex.getNumDatasetsTestedIn() > 0 ) {
            assertEquals( coex.toString(), coex.getNumDatasetsTestedIn().intValue(),
                    coex.getTestedInDatasets().size() );
            assertTrue( coex.toString() + " testedin: " + coex.getTestedInDatasets() + " supportedin: " + coex
                    .getSupportingDatasets(), coex.getNumDatasetsSupporting() <= coex.getNumDatasetsTestedIn() );
        }

        assertEquals( coex.toString(), coex.getSupportingDatasets().size(),
                coex.getNumDatasetsSupporting().intValue() );

        assertTrue( coex.toString(), !coex.getSupportingDatasets().isEmpty() );
    }

    private int checkResults( Collection<ExpressionExperiment> ees, int expectedMinimumMaxSupport ) {
        boolean foundOne = false;

        int maxSupport = 0;
        Taxon mouse = taxonService.findByCommonName( "mouse" );
        Collection<Gene> genesWithLinks = new ArrayList<>();
        int totalLinks = 0;

        // numdatasetstesting will not be set so we won't bother checking.
        assertTrue( !geneCoexpressionService.getCoexpression( ee, true ).isEmpty() );

        Collection<CoexpressionValueObject> eeResults = geneCoexpressionService.getCoexpression( ee, false );
        assertTrue( !eeResults.isEmpty() );
        for ( CoexpressionValueObject coex : eeResults ) {
            this.checkResult( coex );
        }

        Map<Long, GeneCoexpressionNodeDegreeValueObject> nodeDegrees = geneCoexpressionService
                .getNodeDegrees( IdentifiableUtils.getIds( geneService.loadAll() ) );
        assertTrue( !nodeDegrees.isEmpty() );

        // experiment-major query
        Map<Long, List<CoexpressionValueObject>> allLinks = geneCoexpressionService
                .findCoexpressionRelationships( mouse, new HashSet<Long>(), IdentifiableUtils.getIds( ees ), ees.size(), 10,
                        false );
        assertTrue( !allLinks.isEmpty() );

        for ( Long g : allLinks.keySet() ) {
            for ( CoexpressionValueObject coex : allLinks.get( g ) ) {
                this.checkResult( coex );
            }
        }

        for ( Gene gene : geneService.loadAll( mouse ) ) {

            Collection<CoexpressionValueObject> links = geneCoexpressionService
                    .findCoexpressionRelationships( gene, IdentifiableUtils.getIds( ees ), 1, 0, false );

            if ( links == null || links.isEmpty() ) {
                continue;
            }

            assertEquals( geneCoexpressionService
                            .findCoexpressionRelationships( gene, Collections.singleton( ee.getId() ), 0, false ).size(),
                    geneCoexpressionService.countLinks( ee, gene ).intValue() );

            GeneCoexpressionNodeDegreeValueObject nodeDegree = geneCoexpressionService.getNodeDegree( gene );

            if ( links.size() != nodeDegree.getLinksWithMinimumSupport( 1 ) ) {
                log.info( nodeDegree );
                assertEquals( "Node degree check failed for gene " + gene, links.size(),
                        nodeDegree.getLinksWithMinimumSupport( 1 ).intValue() );
            }

            assertTrue( nodeDegree.getLinksWithMinimumSupport( 1 ) >= nodeDegree.getLinksWithMinimumSupport( 2 ) );

            totalLinks += links.size();
            log.debug( links.size() + " hits for " + gene );
            for ( CoexpressionValueObject coex : links ) {
                this.checkResult( coex );
                if ( coex.getNumDatasetsSupporting() > maxSupport ) {
                    maxSupport = coex.getNumDatasetsSupporting();
                }
            }
            foundOne = true;

            if ( genesWithLinks.size() == 5 ) {

                // without specifying stringency
                Map<Long, List<CoexpressionValueObject>> multiGeneResults = geneCoexpressionService
                        .findCoexpressionRelationships( mouse, IdentifiableUtils.getIds( genesWithLinks ),
                                IdentifiableUtils.getIds( ees ), 100, false );

                if ( !multiGeneResults.isEmpty() ) {

                    for ( Long id : multiGeneResults.keySet() ) {
                        for ( CoexpressionValueObject coex : multiGeneResults.get( id ) ) {
                            this.checkResult( coex );
                        }
                    }

                    // with stringency specified, quick.
                    Map<Long, List<CoexpressionValueObject>> multiGeneResults2 = geneCoexpressionService
                            .findCoexpressionRelationships( mouse, IdentifiableUtils.getIds( genesWithLinks ), IdentifiableUtils.getIds( ees ), ees.size(), 100, true );
                    if ( multiGeneResults.size() != multiGeneResults2.size() ) {
                        assertEquals( multiGeneResults.size(), multiGeneResults2.size() );
                    }

                    for ( Long id : multiGeneResults2.keySet() ) {
                        for ( CoexpressionValueObject coex : multiGeneResults2.get( id ) ) {
                            this.checkResult( coex );
                        }
                    }
                }
            }
            genesWithLinks.add( gene );
        }

        assertTrue( foundOne );

        Map<Long, List<CoexpressionValueObject>> mygeneresults = geneCoexpressionService
                .findInterCoexpressionRelationships( mouse, IdentifiableUtils.getIds( genesWithLinks ),
                        IdentifiableUtils.getIds( ees ), 1, false );
        if ( mygeneresults.isEmpty() ) {
            //noinspection ConstantConditions // these strange structures are to help with debugger.
            assertTrue( !mygeneresults.isEmpty() );
        }
        for ( Long id : mygeneresults.keySet() ) {
            for ( CoexpressionValueObject coex : mygeneresults.get( id ) ) {
                this.checkResult( coex );
            }
        }

        assertTrue( maxSupport >= expectedMinimumMaxSupport );

        return totalLinks;
    }

    private void updateNodeDegree() {

        geneCoexpressionService.updateNodeDegrees( this.getTaxon( "mouse" ) );

    }
}
