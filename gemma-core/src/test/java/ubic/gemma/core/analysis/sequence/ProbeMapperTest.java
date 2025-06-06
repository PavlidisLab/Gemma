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
package ubic.gemma.core.analysis.sequence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import ubic.gemma.core.goldenpath.GoldenPathSequenceAnalysis;
import ubic.gemma.core.loader.genome.BlatResultParser;
import ubic.gemma.core.util.test.category.GoldenPathTest;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

/**
 * Unaware of the Gemma database but uses the hg19 and mm10 databases (tests will not work with hg38)
 *
 * @author pavlidis
 */
@Category(GoldenPathTest.class)
public class ProbeMapperTest {

    private static final Log log = LogFactory.getLog( ProbeMapperTest.class.getName() );

    private static GoldenPathSequenceAnalysis mousegp = null;
    private static GoldenPathSequenceAnalysis humangp = null;
    private static Collection<BlatResult> blatres;
    private static List<Double> tester;

    @BeforeClass
    public static void setUp() throws Exception {
        Taxon mouseTaxon = Taxon.Factory.newInstance( "mouse" );
        Taxon humanTaxon = Taxon.Factory.newInstance( "human" );

        try {
            mousegp = new GoldenPathSequenceAnalysis( mouseTaxon );
            humangp = new GoldenPathSequenceAnalysis( humanTaxon );
        } catch ( CannotGetJdbcConnectionException e ) {
            assumeNoException( e );
        }

        tester = new ArrayList<>();
        tester.add( 400d );
        tester.add( 200d );
        tester.add( 100d );
        tester.add( 50d );

        try ( InputStream is = ProbeMapperTest.class.getResourceAsStream( "/data/loader/genome/col8a1.blatresults.txt" ) ) {
            BlatResultParser brp = new BlatResultParser();
            brp.setTaxon( mouseTaxon );
            brp.parse( is );
            blatres = brp.getResults();
            assertTrue( blatres != null && !blatres.isEmpty() );
        }
    }

    @AfterClass
    public static void tearDown() {
        if ( mousegp != null ) {
            mousegp.close();
        }
        if ( humangp != null ) {
            humangp.close();
        }
    }

    @Test
    public void testComputeSpecificityA() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 400 );
        Double expected = 400 / 750.0;
        Assert.assertEquals( expected, actual, 0.0001 );
    }

    @Test
    public void testComputeSpecificityB() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 200 );
        Double expected = 200 / 750.0;
        Assert.assertEquals( expected, actual, 0.0001 );
    }

    @Test
    public void testComputeSpecificityC() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 50 );
        Double expected = 50 / 750.0;
        Assert.assertEquals( expected, actual, 0.0001 );
    }

    @Test
    public void testComputeSpecificityD() {
        Double actual = BlatAssociationScorer.computeSpecificity( tester, 395 );
        Double expected = 395 / 750.0;
        Assert.assertEquals( expected, actual, 0.0001 );
    }

    /*
     * Test based on U83843, should bring up CCT7 (NM_006429 and NM_001009570). Valid locations as of 2/2011 for hg19.
     * <a href="http://genome.ucsc.edu/cgi-bin/hgTracks?hgsid=79741184&hgt.out1=1.5x&position=chr2%3A73320308-73331929">
     * here</a>
     * 73,461,405-73,480,144)
     */
    @Test
    public void testLocateGene() {
        Collection<GeneProduct> products = humangp.findRefGenesByLocation( "2", 73461505L, 73462405L, "+" );
        Assert.assertEquals( 6, products.size() );
        GeneProduct gprod = products.iterator().next();
        Assert.assertEquals( "CCT7", gprod.getGene().getOfficialSymbol() ); // okay as of 1/2008.
    }

    /*
     * Tests a sequence alignment that hits a gene, but the alignment is on the wrong strand; show that ignoring the
     * strand works.
     */
    @Test
    public void testLocateGeneOnWrongStrand() {
        Collection<GeneProduct> products = humangp.findRefGenesByLocation( "6", 32916471L, 32918445L, null );
        Assert.assertEquals( 1, products.size() );
        GeneProduct gprod = products.iterator().next();
        Assert.assertEquals( "HLA-DMA", gprod.getGene().getOfficialSymbol() ); // oka 2/2011
    }

    @Test
    @Ignore
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

    @Test
    public void testIntronIssues() {
        ProbeMapperConfig config = new ProbeMapperConfig();
        Collection<BlatAssociation> results = humangp
                .findAssociations( "chr1", 145517370L, 145518088L, "145517370,145518070", "18,18", null,
                        ThreePrimeDistanceMethod.RIGHT, config );

        assertTrue( results != null && !results.isEmpty() );
        for ( BlatAssociation blatAssociation : results ) {
            ProbeMapperTest.log.debug( blatAssociation );
            if ( blatAssociation.getGeneProduct().getGene().getOfficialSymbol().equals( "NBPF10" ) ) {
                Assert.fail( "Should not have gotten NBPF10" );
            }
        }
    }
}
