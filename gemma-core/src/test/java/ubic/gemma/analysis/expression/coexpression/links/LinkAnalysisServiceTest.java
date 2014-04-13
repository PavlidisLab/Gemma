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
package ubic.gemma.analysis.expression.coexpression.links;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig.SingularThreshold;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.association.coexpression.CoexpressionCache;
import ubic.gemma.model.association.coexpression.CoexpressionService;
import ubic.gemma.model.association.coexpression.CoexpressionValueObject;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.TableMaintenenceUtil;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.EntityUtils;

/**
 * @author paul
 * @version $Id$
 */
public class LinkAnalysisServiceTest extends BaseSpringContextTest {

    @Autowired
    private BasicDataSource dataSource;

    private ExpressionExperiment ee;

    @Autowired
    private ExpressionExperimentService eeService;

    private FilterConfig filterConfig = new FilterConfig();

    @Autowired
    private CoexpressionCache gene2GeneCoexpressionCache;

    @Autowired
    private CoexpressionService geneCoexpressionService;

    @Autowired
    private GeneService geneService;

    private LinkAnalysisConfig linkAnalysisConfig = new LinkAnalysisConfig();

    @Autowired
    private LinkAnalysisPersister linkAnalysisPersisterService;

    @Autowired
    private LinkAnalysisService linkAnalysisService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private TableMaintenenceUtil tableMaintenenceUtil;

    /**
     * 
     */
    public void checkUnsupportedLinksHaveNoSupport() {
        JdbcTemplate jt = new JdbcTemplate( dataSource );

        // see SupportDetailsTest for validation that these strings represent empty byte arrays. I think the 1 at
        // position 12 is important.
        final Collection<Long> checkme = new HashSet<Long>();
        // maybe these patterns aren't this reproducible.
        jt.query(
                // "SELECT ID from MOUSE_LINK_SUPPORT_DETAILS WHERE HEX(BYTES) in ('0000000200000001000000000000000200000000',"
                // + " '000006AA00000001000000000000003600000000', '0000000000000001000000000000000000000000',"
                // + "'0000003E00000001000000000000000200000000','0000003F00000001000000000000000200000000',"
                // + "'0000000500000001000000000000000200000000')", new RowCallbackHandler() {

                "SELECT ID from MOUSE_LINK_SUPPORT_DETAILS WHERE HEX(BYTES) LIKE '00000___00000001000000000000000%'",
                new RowCallbackHandler() {

                    @Override
                    public void processRow( ResultSet rs ) throws SQLException {
                        Long id = rs.getLong( 1 );
                        checkme.add( id );
                    }
                } );

        // we should definitely have some of these
        assertTrue( checkme.size() > 0 );

        jt.query( "SELECT SUPPORT FROM MOUSE_GENE_COEXPRESSION WHERE SUPPORT_DETAILS_FK in (?) AND SUPPORT > 0",
                new Object[] { checkme.toArray() }, new RowCallbackHandler() {
                    @Override
                    public void processRow( ResultSet rs ) throws SQLException {
                        fail( "Should not have had any rows" );
                    }
                } );

    }

    @Before
    public void setup() {
        super.setTestCollectionSize( 100 );
    }

    @After
    public void tearDown() {
        super.resetTestCollectionSize();
    }

    @Test
    public void testLoadAnalyzeSaveAndCoexpSearch() {
        ee = this.getTestPersistentCompleteExpressionExperimentWithSequences();
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        tableMaintenenceUtil.disableEmail();
        tableMaintenenceUtil.updateGene2CsEntries();
        linkAnalysisConfig.setCdfCut( 0.1 );
        linkAnalysisConfig.setSingularThreshold( SingularThreshold.cdfcut );
        linkAnalysisConfig.setProbeDegreeThreshold( 25 );
        linkAnalysisConfig.setCheckCorrelationDistribution( false );
        filterConfig.setIgnoreMinimumSampleThreshold( true );

        // first time.
        LinkAnalysis la = linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );

        // test delete is clean; to check this properly requires checking the db.
        linkAnalysisPersisterService.deleteAnalyses( ee );
        checkUnsupportedLinksHaveNoSupport();
        assertEquals( 0, geneCoexpressionService.getCoexpression( ee, true ).size() );

        la = linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );

        CoexpressionAnalysis analysisObj = la.getAnalysisObj();
        assertEquals( 151, analysisObj.getNumberOfElementsAnalyzed().intValue() );
        assertTrue( analysisObj.getNumberOfLinks().intValue() > 0 );

        assertNotNull( analysisObj.getCoexpCorrelationDistribution() );

        Collection<BioAssaySet> ees = new HashSet<>();
        ees.add( ee );

        updateNodeDegree();
        int totalLinksFirstPass = checkResults( ees, 1 );

        // should be ~1140.
        assertTrue( totalLinksFirstPass > 1000 );

        // test redo
        linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );
        updateNodeDegree();

        int totalLinksRedo = checkResults( ees, 1 );
        assertEquals( totalLinksFirstPass, totalLinksRedo );

        // now add another experiment that has overlapping links (same data...
        Map<CompositeSequence, byte[]> dataMap = new HashMap<>();
        for ( RawExpressionDataVector v : ee.getRawExpressionDataVectors() ) {
            dataMap.put( v.getDesignElement(), v.getData() );
        }

        ExpressionExperiment ee2 = this.getTestPersistentCompleteExpressionExperimentWithSequences( ee );
        for ( RawExpressionDataVector v : ee2.getRawExpressionDataVectors() ) {
            assert dataMap.get( v.getDesignElement() ) != null;
            v.setData( dataMap.get( v.getDesignElement() ) );
        }

        eeService.update( ee2 );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee2 );
        linkAnalysisService.process( ee2, filterConfig, linkAnalysisConfig );

        updateNodeDegree();

        gene2GeneCoexpressionCache.clearCache();

        // expect to get at least one links with support >1
        ees.add( ee2 );
        checkResults( ees, 2 );

    }

    /**
     * @param coex
     */
    private void checkResult( CoexpressionValueObject coex ) {
        assertNotNull( coex.toString(), coex.getQueryGeneId() );
        assertNotNull( coex.toString(), coex.getCoexGeneId() );
        assertNotNull( coex.toString(), coex.getSupportDetailsId() );
        assertNotNull( coex.toString(), coex.getSupportingDatasets() );
        assertTrue( coex.toString(), coex.getNumDatasetsSupporting() > 0 );
        assertTrue( coex.toString(), coex.getNumDatasetsTestedIn() != 0 );

        // assertNotNull( coex.toString(), coex.getTestedInDatasets() );

        if ( coex.getNumDatasetsTestedIn() > 0 ) {
            assertEquals( coex.toString(), coex.getNumDatasetsTestedIn().intValue(), coex.getTestedInDatasets().size() );
            assertTrue(
                    coex.toString() + " testedin: " + coex.getTestedInDatasets() + " supportedin: "
                            + coex.getSupportingDatasets(),
                    coex.getNumDatasetsSupporting() <= coex.getNumDatasetsTestedIn() );
        }

        assertEquals( coex.toString(), coex.getSupportingDatasets().size(), coex.getNumDatasetsSupporting().intValue() );

        assertTrue( coex.toString(), !coex.getSupportingDatasets().isEmpty() );
    }

    /**
     * @param ees
     * @param expectedMinimumMaxSupport
     */
    private int checkResults( Collection<BioAssaySet> ees, int expectedMinimumMaxSupport ) {
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
            checkResult( coex );
        }

        Map<Long, GeneCoexpressionNodeDegreeValueObject> nodeDegrees = geneCoexpressionService
                .getNodeDegrees( EntityUtils.getIds( geneService.loadAll() ) );
        assertTrue( !nodeDegrees.isEmpty() );

        // experiment-major query
        Map<Long, List<CoexpressionValueObject>> allLinks = geneCoexpressionService.findCoexpressionRelationships(
                mouse, new HashSet<Long>(), EntityUtils.getIds( ees ), ees.size(), 10, false );
        assertTrue( !allLinks.isEmpty() );

        for ( Long g : allLinks.keySet() ) {
            for ( CoexpressionValueObject coex : allLinks.get( g ) ) {
                checkResult( coex );
            }
        }

        for ( Gene gene : geneService.loadAll() ) {

            Collection<CoexpressionValueObject> links = geneCoexpressionService.findCoexpressionRelationships( gene,
                    EntityUtils.getIds( ees ), 1, 0, false );

            if ( links == null || links.isEmpty() ) {
                continue;
            }

            assertEquals(
                    geneCoexpressionService.findCoexpressionRelationships( gene, EntityUtils.getIds( ee ), 0, false )
                            .size(), geneCoexpressionService.countLinks( ee, gene ).intValue() );

            GeneCoexpressionNodeDegreeValueObject nodeDegree = geneCoexpressionService.getNodeDegree( gene );

            if ( links.size() != nodeDegree.getLinksWithMinimumSupport( 1 ).intValue() ) {

                assertEquals( "Node degree check failed for gene " + gene, links.size(), nodeDegree
                        .getLinksWithMinimumSupport( 1 ).intValue() );
            }

            assertTrue( nodeDegree.getLinksWithMinimumSupport( 1 ) >= nodeDegree.getLinksWithMinimumSupport( 2 ) );

            totalLinks += links.size();
            log.debug( links.size() + " hits for " + gene );
            for ( CoexpressionValueObject coex : links ) {
                checkResult( coex );
                if ( coex.getNumDatasetsSupporting() > maxSupport ) {
                    maxSupport = coex.getNumDatasetsSupporting();
                }
            }
            foundOne = true;

            if ( genesWithLinks.size() == 5 ) {

                // without specifying stringency
                Map<Long, List<CoexpressionValueObject>> multiGeneResults = geneCoexpressionService
                        .findCoexpressionRelationships( mouse, EntityUtils.getIds( genesWithLinks ),
                                EntityUtils.getIds( ees ), 100, false );
                if ( multiGeneResults.isEmpty() ) {
                    assertTrue( !multiGeneResults.isEmpty() );
                }

                for ( Long id : multiGeneResults.keySet() ) {
                    for ( CoexpressionValueObject coex : multiGeneResults.get( id ) ) {
                        checkResult( coex );
                    }
                }

                // with stringency specified, quick.
                Map<Long, List<CoexpressionValueObject>> multiGeneResults2 = geneCoexpressionService
                        .findCoexpressionRelationships( mouse, EntityUtils.getIds( genesWithLinks ),
                                EntityUtils.getIds( ees ), ees.size(), 100, true );
                if ( multiGeneResults.size() != multiGeneResults2.size() ) {
                    assertEquals( multiGeneResults.size(), multiGeneResults2.size() );
                }

                for ( Long id : multiGeneResults2.keySet() ) {
                    for ( CoexpressionValueObject coex : multiGeneResults2.get( id ) ) {
                        checkResult( coex );
                    }
                }
            }
            genesWithLinks.add( gene );
        }

        assertTrue( foundOne );

        Map<Long, List<CoexpressionValueObject>> mygeneresults = geneCoexpressionService
                .findInterCoexpressionRelationships( mouse, EntityUtils.getIds( genesWithLinks ),
                        EntityUtils.getIds( ees ), 1, false );
        if ( mygeneresults.isEmpty() ) {
            assertTrue( !mygeneresults.isEmpty() );
        }
        for ( Long id : mygeneresults.keySet() ) {
            for ( CoexpressionValueObject coex : mygeneresults.get( id ) ) {
                checkResult( coex );
            }
        }

        assertTrue( maxSupport >= expectedMinimumMaxSupport );

        return totalLinks;
    }

    private void updateNodeDegree() {

        geneCoexpressionService.updateNodeDegrees( this.getTaxon( "mouse" ) );

    }
}
