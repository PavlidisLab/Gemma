package edu.columbia.gemma.tools;

import java.io.InputStream;
import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.loader.arraydesign.AffyProbeReader;
import edu.columbia.gemma.sequence.biosequence.BioSequence;

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

    BioSequence seq;

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

        a.setIdentifier( "a" );
        b.setIdentifier( "b" );
        c.setIdentifier( "c" );
        d.setIdentifier( "d" );
        e.setIdentifier( "e" );

        ar.setImmobilizedCharacteristic( a );
        br.setImmobilizedCharacteristic( b );
        cr.setImmobilizedCharacteristic( c );
        dr.setImmobilizedCharacteristic( d );
        er.setImmobilizedCharacteristic( e );

        ar.setIdentifier( "ar" );
        br.setIdentifier( "br" );
        cr.setIdentifier( "cr" );
        dr.setIdentifier( "dr" );
        er.setIdentifier( "er" );

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
        CompositeSequence t = ( CompositeSequence ) apr.get( "100470_at" );

        BioSequence m = SequenceManipulation.collapse( t );
        log.debug( m.getSequence() );
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

}
