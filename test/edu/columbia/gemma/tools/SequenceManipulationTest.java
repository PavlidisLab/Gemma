package edu.columbia.gemma.tools;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.genome.PhysicalLocation;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.gene.GeneProduct;
import edu.columbia.gemma.loader.expression.arraydesign.AffyProbeReader;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SequenceManipulationTest extends TestCase {

    CompositeSequence tester = null;
    private InputStream iotest;
    protected static final Log log = LogFactory.getLog( SequenceManipulationTest.class );

    Reporter[] testProbes;
    int[] expectedResults;

    BioSequence seq = null;
    GeneProduct gp = null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        Reporter ar = Reporter.Factory.newInstance();
        Reporter br = Reporter.Factory.newInstance();
        Reporter cr = Reporter.Factory.newInstance();
        Reporter dr = Reporter.Factory.newInstance();
        Reporter er = Reporter.Factory.newInstance();

        BioSequence a = BioSequence.Factory.newInstance();
        BioSequence b = BioSequence.Factory.newInstance();
        BioSequence c = BioSequence.Factory.newInstance();
        BioSequence d = BioSequence.Factory.newInstance();
        BioSequence e = BioSequence.Factory.newInstance();

        a.setSequence( "AAAAAAAA" );
        b.setSequence( "AAAACCCC" );
        c.setSequence( "CCCCGGGG" );
        d.setSequence( "GGGGCCCC" );
        e.setSequence( "CCCCTTTT" );

        ar.setImmobilizedCharacteristic( a );
        br.setImmobilizedCharacteristic( b );
        cr.setImmobilizedCharacteristic( c );
        dr.setImmobilizedCharacteristic( d );
        er.setImmobilizedCharacteristic( e );

        ar.setStartInBioChar( 1 );
        br.setStartInBioChar( 5 );
        cr.setStartInBioChar( 9 );
        dr.setStartInBioChar( 13 );
        er.setStartInBioChar( 17 );

        tester = CompositeSequence.Factory.newInstance();
        tester.setReporters( new HashSet() );
        tester.getReporters().add( ar );
        tester.getReporters().add( br );
        tester.getReporters().add( cr );
        tester.getReporters().add( dr );
        tester.getReporters().add( er );

        iotest = SequenceManipulationTest.class.getResourceAsStream( "/data/loader/100470_at.probes" );

        testProbes = new Reporter[] { Reporter.Factory.newInstance(), Reporter.Factory.newInstance(),
                Reporter.Factory.newInstance(), Reporter.Factory.newInstance(), Reporter.Factory.newInstance() };

        for ( int i = 0; i < testProbes.length; i++ ) {
            testProbes[i].setImmobilizedCharacteristic( BioSequence.Factory.newInstance() );
        }

        String[] seqs = new String[] { "ccc", "cccccccaaaaaaaaa", "atttttttttt", "aaaaaaaaaaccc",
                "ccaaacccccccccccaaa", "ggg", "ggggggg" };

        for ( int i = 0; i < testProbes.length; i++ ) {
            testProbes[i].getImmobilizedCharacteristic().setSequence( seqs[i] );
        }

        expectedResults = new int[] { 3, 5, 0, 0, 2, 0, 0 };

        seq = BioSequence.Factory.newInstance();
        seq.setSequence( "ccccccgccccc" );

        // this is human APRIN, NM_015032 (the shorter transcript)
        gp = GeneProduct.Factory.newInstance();

        int[] exonStarts = new int[] { 32058623, 32120890, 32123940, 32130375, 32130570, 32131290, 32139900, 32145352,
                32147980, 32150971, 32156014, 32159270, 32160592, 32166359, 32168990, 32171866, 32173459, 32179070,
                32182082, 32204237, 32207308, 32213217, 32214728, 32218114, 32225469, 32227979, 32230224, 32230671,
                32231765, 32232712, 32236626, 32242258, 32242791, 32245326, 32247154 };
        int[] exonEnds = new int[] { 32058730, 32121017, 32124144, 32130462, 32130668, 32131417, 32139981, 32145493,
                32148096, 32151066, 32156160, 32159422, 32160706, 32166441, 32169039, 32172006, 32173575, 32179176,
                32182243, 32204361, 32207467, 32213286, 32214865, 32218238, 32225674, 32228094, 32230357, 32230791,
                32231828, 32232858, 32236732, 32242698, 32242899, 32245462, 32250157 };

        Collection exons = new ArrayList();

        for ( int i = 0; i < exonStarts.length; i++ ) {
            PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
            pl.setStrand( "+" );
            pl.setNucleotide( new Integer( exonStarts[i] ) );
            pl.setNucleotideLength( new Integer( exonEnds[i] - exonStarts[i] ) );
            exons.add( pl );
        }
        gp.setPhysicalLocation( PhysicalLocation.Factory.newInstance() );
        gp.setExons( exons );

    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testCollapseTrouble() throws Exception {
        AffyProbeReader apr = new AffyProbeReader();
        apr.setSequenceField( 5 );
        apr.parse( iotest );
        assertTrue( "Not found", apr.containsKey( "100470_at" ) );
        CompositeSequence t = ( CompositeSequence ) apr.get( "100470_at" );
        assertTrue( "Null", t != null );
        BioSequence m = SequenceManipulation.collapse( t );
    }

    public final void testCollapse() throws Exception {
        String actualReturn = SequenceManipulation.collapse( tester ).getSequence().toLowerCase();
        String expectedReturn = "aaaaaaaaccccggggcccctttt";
        assertEquals( expectedReturn, actualReturn );
    }

    public final void testOverlapSequence() throws Exception {

        for ( int i = 0; i < testProbes.length; i++ ) {
            int actualReturn = SequenceManipulation
                    .rightHandOverlap( seq, testProbes[i].getImmobilizedCharacteristic() );

            int expectedReturn = expectedResults[i];
            assertEquals( expectedReturn, actualReturn );
        }

    }

    public final void testExonOverlapA() throws Exception {

        String starts = "32242963,"; // should have zero overlap with the gp.
        String sizes = "50,";
        String strand = "+";

        int actualReturn = SequenceManipulation.getGeneProductExonOverlap( starts, sizes, strand, gp );
        int expectedReturn = 0;

        assertEquals( expectedReturn, actualReturn );
    }

    public final void testExonOverlapB() throws Exception {

        String starts = "32247695,";
        String sizes = "50,";
        String strand = "+";

        int actualReturn = SequenceManipulation.getGeneProductExonOverlap( starts, sizes, strand, gp );
        int expectedReturn = 50;

        assertEquals( expectedReturn, actualReturn );
    }

    public final void testExonOverlapC() throws Exception {

        String starts = "32242893,"; // partly in an inton.
        String sizes = "50,";
        String strand = "+";

        int actualReturn = SequenceManipulation.getGeneProductExonOverlap( starts, sizes, strand, gp );
        int expectedReturn = 6;

        assertEquals( expectedReturn, actualReturn );
    }

    public final void testFindCenter() throws Exception {
        String starts = "100,200,300,400";
        String sizes = "10,10,10,10";
        int actualReturn = SequenceManipulation.findCenter( starts, sizes );
        int expectedReturn = 210;
        assertEquals( expectedReturn, actualReturn );
    }

}
