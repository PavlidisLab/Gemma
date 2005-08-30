package edu.columbia.gemma.tools;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.biosequence.BioSequence;
import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatTest extends TestCase {

    private static Log log = LogFactory.getLog( BlatTest.class.getName() );

    BioSequence bs;
    Blat b;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        bs = BioSequence.Factory.newInstance();
        bs.setSequence( "agtagtaggggattggttatgaggctagcataataatccaggcaagaggtaatcaggtt"
                + "tccatgagattggcaacaaagggactgaagtgaccagcattgcttatgcctggaagatttggaaaatgag"
                + "gcatcaaaacgacaatgtctgaaggagtagcaggttatgggggaagtggatgggtttggaattggacatg"
                + "taagtgttacatgctggttagatatccagggaaagatacctagtaagtagctggaagtatggatctggggcttaaaggagcacttca"
                + "gtaattctttacataaaggcagtacagccatgaaaatagatgatgttgccagagtaagtgtggggagagagaaggtcaaggacaaaa"
                + "atgtggagagtacttgcttagaaagggtggaggggccatcaaaggagtcagaagcagcagttagagaagtagaaagaggagagtagc"
                + "agcgtggtacactgaggtcaCCGGGGGAGGAGAGAGGACGCAGCCAGCCACAGAACAGATGCATCCTCTAGGGCTAGAGGGTCCTG"
                + "AAAGCTCCGAGAGTAATTCTCATGTGCATTTAGGTTTGGGAATAGATCACTGTTAATTCAACAGAGAAATGAAAGAAGAGAAGGTTC"
                + "GGTGGGGTCCAGCCATGCCCTGTTACGTGGAATTTTTTCCCTAAGGGTGTGGTCCCCTCCCCTACAGCTCGTCTTTTGGAGGGCTG"
                + "GTCCAGGCTCCTCTAAGCCATGACGCCGGCTGAGGATCAGCGGTTGGTGTACATGATCTCCTCAGCCTTGCCCGTTGTCC" );
        bs.setName( "Test sequence" );
        File foo = File.createTempFile( "bla", "foo" );
        foo.deleteOnExit();
        b = new Blat();
        b.startServer();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        b.stopServer();
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.Blat.Blat(String, String, String)'
     */
    public void testBlat() throws Exception {
        b.GfClient( bs );
        // FIXME
        // assertEquals( "foo", result );
    }

}
