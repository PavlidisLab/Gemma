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
package ubic.gemma.analysis.sequence;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperTest extends TestCase {

    private static Log log = LogFactory.getLog( ProbeMapperTest.class.getName() );
    Collection<BlatResult> blatres;
    private String databaseHost;
    private String databaseUser;
    private String databasePassword;
    List<Double> tester;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        tester = new ArrayList<Double>();
        tester.add( new Double( 400 ) );
        tester.add( new Double( 200 ) );
        tester.add( new Double( 100 ) );
        tester.add( new Double( 50 ) );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/col8a1.blatresults.txt" );
        BlatResultParser brp = new BlatResultParser();
        brp.parse( is );
        blatres = brp.getResults();

        assert blatres != null && blatres.size() > 0;

        databaseHost = ConfigUtils.getString( "gemma.testdb.host" );
        databaseUser = ConfigUtils.getString( "gemma.testdb.user" );
        databasePassword = ConfigUtils.getString( "gemma.testdb.password" );

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testProcessBlatResults() throws Exception {
        ProbeMapper pm = new ProbeMapper();

        try {
            GoldenPathSequenceAnalysis gp = new GoldenPathSequenceAnalysis( 3306, "mm8", databaseHost, databaseUser,
                    databasePassword );

            Map<String, Collection<BlatAssociation>> res = pm.processBlatResults( gp, blatres );

            // This test will fail if the database changes :)
            assertTrue( "No results", res.values().size() > 0 );
            assertTrue( "No results", res.values().iterator().next().size() > 0 );
            assertEquals( "Col8a1", res.values().iterator().next().iterator().next().getGeneProduct().getGene()
                    .getOfficialSymbol() );
        } catch ( java.sql.SQLException e ) {
            if ( e.getMessage().contains( "Unknown database" ) ) {
                log.warn( "Test skipped due to missing mm8 database" );
                return;
            } else if ( e.getMessage().contains( "Access denied" ) ) {
                log.warn( "Test skipped due to database authentication problem - check username and password in test" );
                return;
            }
            throw e;
        }

    }

    /**
     * Test based on U83843, should bring up CCT7 (NM_006429 and NM_001009570). Valid locations as of 10/31/2006.
     * {@link http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=79741184&hgt.out1=1.5x&position=chr2%3A73320308-73331929}
     */
    public void testLocateGene() throws Exception {
        GoldenPathSequenceAnalysis gp = new GoldenPathSequenceAnalysis( 3306, "hg18", databaseHost, databaseUser,
                databasePassword );

        Collection<GeneProduct> products = gp.findRefGenesByLocation( "2", new Long( 73320308 ), new Long( 73331929 ),
                "+" );
        assertEquals( 2, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "CCT7", gprod.getGene().getOfficialSymbol() );
    }

    /**
     * @throws Exception
     */
    public void testLocateMiRNA() throws Exception {
        GoldenPathSequenceAnalysis gp = new GoldenPathSequenceAnalysis( 3306, "hg18", databaseHost, databaseUser,
                databasePassword );

        Collection<GeneProduct> products = gp.findMicroRNAGenesByLocation( "X", new Long( 133131074 ), new Long(
                133131148 ), "-" );
        assertEquals( 1, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "hsa-mir-363", gprod.getGene().getOfficialSymbol() );
    }

    public void testLocateAcembly() throws Exception {
        GoldenPathSequenceAnalysis gp = new GoldenPathSequenceAnalysis( 3306, "hg18", databaseHost, databaseUser,
                databasePassword );

        Collection<GeneProduct> products = gp.findAcemblyGenesByLocation( "7", new Long( 80145000 ),
                new Long( 80146000 ), "+" );
        assertEquals( 1, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "CD36.cAug05", gprod.getGene().getOfficialSymbol() );
        assertEquals( "Predicted gene imported from Golden Path. Acembly gene, class=main", gprod.getGene()
                .getDescription() );
    }

    public void testLocateNscan() throws Exception {
        GoldenPathSequenceAnalysis gp = new GoldenPathSequenceAnalysis( 3306, "hg18", databaseHost, databaseUser,
                databasePassword );

        Collection<GeneProduct> products = gp.findNscanGenesByLocation( "3", new Long( 181237455 ),
                new Long( 181318731 ), "+" );
        assertEquals( 1, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "chr3.182.002.a", gprod.getGene().getOfficialSymbol() );
    }

    /**
     * Tests a sequence alignment that hits a gene, but the alignment is on the wrong strand; show that ignoring the
     * strand works.
     */
    public void testLocateGeneOnWrongStrand() throws Exception {
        GoldenPathSequenceAnalysis gp = new GoldenPathSequenceAnalysis( 3306, "hg18", databaseHost, databaseUser,
                databasePassword );

        Collection<GeneProduct> products = gp.findRefGenesByLocation( "6", new Long( 32916471 ), new Long( 32918445 ),
                null );
        assertEquals( 2, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "PSMB8", gprod.getGene().getOfficialSymbol() );
    }

    public void testComputeSpecificityA() throws Exception {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 400 );
        Double expected = 0.5;
        assertEquals( expected, actual, 0.0001 );
    }

    public void testComputeSpecificityB() throws Exception {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 200 );
        Double expected = 0.5;
        assertEquals( expected, actual, 0.0001 );
    }

    public void testComputeSpecificityC() throws Exception {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 50 );
        Double expected = 0.25;
        assertEquals( expected, actual, 0.0001 );
    }

    public void testComputeSpecificityD() throws Exception {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 395 );
        Double expected = 0.4936;
        assertEquals( expected, actual, 0.0001 );
    }

}
