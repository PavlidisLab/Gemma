/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.tools;

import java.io.File;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResult;
import edu.columbia.gemma.tools.Blat.BlattableGenome;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BlatTest extends TestCase {

    @SuppressWarnings("unused")
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
        b.startServer( b.getHumanServerPort() );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        b.stopServer( b.getHumanServerPort() );
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.Blat.Blat(String, String, String)'
     */
    public void testBlat() throws Exception {
        Collection<Object> results = b.GfClient( bs, BlattableGenome.HUMAN );
        assertTrue( results.size() == 1 );
        BlatResult br = ( BlatResult ) results.iterator().next();
        assertEquals( 789, br.getMatches() );
    }

}
