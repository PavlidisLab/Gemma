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
import ubic.gemma.util.Settings;

/**
 * Unaware of the database.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperTest extends TestCase {

    private static Log log = LogFactory.getLog( ProbeMapperTest.class.getName() );
    private Collection<BlatResult> blatres;
    private String databaseHost;
    private String databaseUser;
    private String databasePassword;
    private List<Double> tester;
    private GoldenPathSequenceAnalysis mousegp = null;
    private GoldenPathSequenceAnalysis humangp = null;

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

    /**
     * Test based on U83843, should bring up CCT7 (NM_006429 and NM_001009570). Valid locations as of 2/2011 for hg19.
     * {@link http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=79741184&hgt.out1=1.5x&position=chr2%3A73320308-73331929}
     * 73,461,405-73,480,144)
     */
    public void testLocateGene() {

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

        Collection<GeneProduct> products = humangp.findRefGenesByLocation( "6", new Long( 32916471 ), new Long(
                32918445 ), null );
        assertEquals( 1, products.size() );
        GeneProduct gprod = products.iterator().next();
        assertEquals( "HLA-DMA", gprod.getGene().getOfficialSymbol() ); // oka 2/2011
    }

    public void testProcessBlatResults() {

        ProbeMapperConfig config = new ProbeMapperConfig();
        config.setMinimumExonOverlapFraction( 0 ); // test is sensitive to this.

        ProbeMapper pm = new ProbeMapperImpl();
        Map<String, Collection<BlatAssociation>> res = pm.processBlatResults( mousegp, blatres, config );

        assertTrue( "No results", res.values().size() > 0 );
        assertTrue( "No results", res.values().iterator().next().size() > 0 );

        boolean found = false;
        for ( Collection<BlatAssociation> r : res.values() ) {
            for ( BlatAssociation blatAssociation : r ) {
                if ( "Filip1l".equals( blatAssociation.getGeneProduct().getGene().getOfficialSymbol() ) ) {
                    found = true;
                }
            }
        }

        assertTrue( found );
    }

    public void testIntronIssues() {

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

        try (InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/col8a1.blatresults.txt" );) {
            BlatResultParser brp = new BlatResultParser();
            Taxon m = Taxon.Factory.newInstance();
            m.setCommonName( "mouse" );
            brp.setTaxon( m );
            brp.parse( is );
            blatres = brp.getResults();

            assert blatres != null && blatres.size() > 0;
        }
        databaseHost = Settings.getString( "gemma.testdb.host" );
        databaseUser = Settings.getString( "gemma.testdb.user" );
        databasePassword = Settings.getString( "gemma.testdb.password" );

        mousegp = new GoldenPathSequenceAnalysis( 3306, Settings.getString( "gemma.goldenpath.db.mouse" ),
                databaseHost, databaseUser, databasePassword );

        humangp = new GoldenPathSequenceAnalysis( 3306, Settings.getString( "gemma.goldenpath.db.human" ),
                databaseHost, databaseUser, databasePassword );

    }

}
