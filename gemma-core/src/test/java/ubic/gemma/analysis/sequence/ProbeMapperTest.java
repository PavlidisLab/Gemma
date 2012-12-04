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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;
import ubic.gemma.util.ConfigUtils;

/**
 * Note that some of the tests here are dependent on the content of the mm9 and hg19 database.
 * 
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
    GoldenPathSequenceAnalysis mousegp = null;
    GoldenPathSequenceAnalysis humangp = null;
    private boolean hasMousegp = true;
    private boolean hasHumangp = true;

    public void testComputeSpecificityA() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 400 );
        Double expected = 400 / 750.0;
        assertEquals( expected, actual, 0.0001 );
    }

    public void testComputeSpecificityB() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 200 );
        Double expected = 200 / 750.0;
        assertEquals( expected, actual, 0.0001 );
    }

    public void testComputeSpecificityC() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 50 );
        Double expected = 50 / 750.0;
        assertEquals( expected, actual, 0.0001 );
    }

    public void testComputeSpecificityD() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 395 );
        Double expected = 395 / 750.0;
        assertEquals( expected, actual, 0.0001 );
    }

    // public void testLocateAcembly() {
    // if ( !hasHumangp ) {
    // log.warn( "Skipping test because hg18 could not be configured" );
    // return;
    // }
    //
    // Collection<GeneProduct> products = humangp.findAcemblyGenesByLocation( "7", new Long( 80145000 ), new Long(
    // 80146000 ), "+" );
    // assertTrue( products.size() > 0 ); // This is 2 as of Jan 2008.
    // }

    /**
     * Test based on U83843, should bring up CCT7 (NM_006429 and NM_001009570). Valid locations as of 2/2011 for hg19.
     * {@link http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=79741184&hgt.out1=1.5x&position=chr2%3A73320308-73331929}
     * 73,461,405-73,480,144)
     */
    public void testLocateGene() {
        if ( !hasHumangp ) {
            log.warn( "Skipping test because  human db could not be configured" );
            return;
        }
        Collection<GeneProduct> products = humangp.findRefGenesByLocation( "2", new Long( 73461505 ), new Long(
                73462405 ), "+" );
        assertEquals( 6, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "CCT7", gprod.getGene().getOfficialSymbol() ); // okay as of 1/2008.
    }

    /**
     * Tests a sequence alignment that hits a gene, but the alignment is on the wrong strand; show that ignoring the
     * strand works.
     */
    public void testLocateGeneOnWrongStrand() {
        if ( !hasHumangp ) {
            log.warn( "Skipping test because  human db could not be configured" );
            return;
        }
        Collection<GeneProduct> products = humangp.findRefGenesByLocation( "6", new Long( 32916471 ), new Long(
                32918445 ), null );
        assertEquals( 1, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "HLA-DMA", gprod.getGene().getOfficialSymbol() ); // oka 2/2011
    }

    public void testProcessBlatResults() {
        if ( !hasMousegp ) {
            log.warn( "Skipping test because mm could not be configured" );
            return;
        }
        ProbeMapperConfig config = new ProbeMapperConfig();
        config.setMinimumExonOverlapFraction( 0 ); // test is sensitive to this.

        ProbeMapper pm = new ProbeMapperImpl();
        Map<String, Collection<BlatAssociation>> res = pm.processBlatResults( mousegp, blatres, config );
        // This test will fail if the database changes :)
        assertTrue( "No results", res.values().size() > 0 );
        assertTrue( "No results", res.values().iterator().next().size() > 0 );
        assertEquals( "Filip1l", res.values().iterator().next().iterator().next().getGeneProduct().getGene()
                .getOfficialSymbol() );

    }

    public void testIntronIssues() {
        if ( !hasHumangp ) {
            log.warn( "Skipping test because hg could not be configured" );
            return;
        }

        ProbeMapperConfig config = new ProbeMapperConfig();
        Collection<BlatAssociation> results = humangp.findAssociations( "chr1", 145517370L, 145518088L,
                "145517370,145518070", "18,18", null, ThreePrimeDistanceMethod.RIGHT, config );

        assertTrue( !results.isEmpty() );
        for ( BlatAssociation blatAssociation : results ) {
            log.debug( blatAssociation );
            if ( blatAssociation.getGeneProduct().getGene().getOfficialSymbol().equals( "NBPF10" ) ) {
                fail( "Should not have gotten NBPF10" );
            }
        }
    }

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
        Taxon m = Taxon.Factory.newInstance();
        m.setCommonName( "mouse" );
        brp.setTaxon( m );
        brp.parse( is );
        blatres = brp.getResults();

        assert blatres != null && blatres.size() > 0;

        databaseHost = ConfigUtils.getString( "gemma.testdb.host" );
        databaseUser = ConfigUtils.getString( "gemma.testdb.user" );
        databasePassword = ConfigUtils.getString( "gemma.testdb.password" );

        try {
            mousegp = new GoldenPathSequenceAnalysis( 3306, ConfigUtils.getString( "gemma.goldenpath.db.mouse" ),
                    databaseHost, databaseUser, databasePassword );
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "Unknown database" ) ) {
                hasMousegp = false;
            } else if ( e.getMessage().contains( "Access denied" ) ) {
                hasMousegp = false;
            } else {
                throw e;
            }
        }
        try {
            humangp = new GoldenPathSequenceAnalysis( 3306, ConfigUtils.getString( "gemma.goldenpath.db.human" ),
                    databaseHost, databaseUser, databasePassword );
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "Unknown database" ) ) {
                hasHumangp = false;
            } else if ( e.getMessage().contains( "Access denied" ) ) {
                hasHumangp = false;
            } else {
                throw e;
            }
        }
    }

}
