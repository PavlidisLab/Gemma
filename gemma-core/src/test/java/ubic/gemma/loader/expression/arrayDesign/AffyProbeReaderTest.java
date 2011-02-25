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

package ubic.gemma.loader.expression.arrayDesign;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.SequenceManipulation;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeReaderTest extends TestCase {
    protected static final Log log = LogFactory.getLog( AffyProbeReaderTest.class );
    AffyProbeReader apr;
    InputStream is;

    /*
     * Test of human exon array
     */
    public final void testReadExonArray() throws Exception {

        InputStream ist = AffyProbeReaderTest.class
                .getResourceAsStream( "/data/loader/expression/arrayDesign/HuEx1_0SampleProbe.txt" );

        apr.parse( ist );

        ist.close();

        // reverse complement of "CGGTGCTGGGTCAGGGATCGACTGA";
        String expectedValue = "TCAGTCGATCCCTGACCCAGCACCG";

        CompositeSequence cs = getProbeMatchingName( "2315108" );

        assertNotNull( cs );

        boolean foundIt = false;
        for ( Iterator<Reporter> iter = apr.get( cs ).iterator(); iter.hasNext(); ) {
            Reporter element = iter.next();
            if ( element.getName().equals( "2315108:814:817" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();
                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( "Didn't find the probe ", foundIt );

        for ( CompositeSequence c : apr.getKeySet() ) {
            BioSequence collapsed = SequenceManipulation.collapse( apr.get( c ) );

            if ( c.getName().equals( "2315357" ) ) {

                /*
                 * On + genomic strand. Note that Affy says the target sequence is
                 * "ttttaattgatgataagctggaataatattaatacacacaaagcacgtgttgtaactttcatt", which slightly differs from our
                 * assembly,we resolve an TA ... TA to TA, while they have TATA. The latter is correct based on the
                 * targeted alignment, but given we only have the probes, it's reasonable. See
                 * https://www.affymetrix.com/analysis/netaffx/exon/probe_set.affx?pk=1:2315357
                 */
                assertEquals( "TTTTAATTGATGATAAGCTGGAATAATATTACACACAAAGCACGTGTTGTAACTTTCATT", collapsed.getSequence() );
            }

            if ( c.getName().equals( "2390604" ) ) {
                /*
                 * On - genomic strand As for the above example, the precise assembly might be different than the affy
                 * target
                 */
                assertEquals(
                        "ACTTCTCTGACACCGCGTTGGGTTCGGGCTCCGAGCACTTCGAAAGTATAACCGCGGTCCCAAAGAGGCGTGCTCCTGGGAGTGCACGGTTTACATTCT",
                        collapsed.getSequence() );

            }

        }

    }

    private CompositeSequence getProbeMatchingName( String name ) {
        Collection<CompositeSequence> keySet = apr.getKeySet();

        CompositeSequence cs = null;
        for ( CompositeSequence compositeSequence : keySet ) {
            if ( compositeSequence.getName().equals( name ) ) {
                cs = compositeSequence;
                break;
            }
        }
        return cs;
    }

    /*
     * Test of mouse gene exon array
     */
    public final void testReadExonArray2() throws Exception {

        InputStream ist = AffyProbeReaderTest.class
                .getResourceAsStream( "/data/loader/expression/arrayDesign/MoGeneSampleProbe.txt" );

        apr.parse( ist );

        ist.close();

        // reverse complement of "CGTTCAAAATTTAGTGTATGTGTTG";
        String expectedValue = "CAACACATACACTAAATTTTGAACG";
        CompositeSequence cs = getProbeMatchingName( "10344616" );

        assertTrue( "CompositeSequence was null", cs != null );

        boolean foundIt = false;
        for ( Iterator<Reporter> iter = apr.get( cs ).iterator(); iter.hasNext(); ) {
            Reporter element = iter.next();
            if ( element.getName().equals( "10344616:975:165" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();
                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( "Didn't find the probe ", foundIt );

        boolean found = false;
        for ( CompositeSequence c : apr.getKeySet() ) {
            BioSequence collapsed = SequenceManipulation.collapse( apr.get( c ) );

            if ( c.getName().equals( "10344614" ) ) {
                // on + strand

                String expected = "AGTTAACACAGGTGAATTTGAGCCCCCTCCCTGGAGAGCGCATAGCCCAAGCCTTA"
                        + "TATGAAAGAGGAGCGAGGACCTTCCTCCCTGGGATCTAACTGATTCATGGCCCATGCCGACTTT"
                        + "GCTCCTGGATACCAGGGCAGATGTGACAGTAATTTCCTCAACACATTGGCCTGCAGCCTGCCCT"
                        + "TACAGCCCACTAAGGGATGGTGTAATCCACTGATCATAAAATTTACTTCTAAGGGGAGAAACTTAGAGGCTTGA";

                assertEquals( expected, collapsed.getSequence() );
            }

            if ( c.getName().equals( "10353672" ) ) {
                found = true;
                // on - genomic strand, confirmed in blat. Affy target sequence is
                // "gtctagggccataccaccctgaacgcgcccaatctcgtctgttctcagaagctaagcagggttgggcctggttagtacttggatgggagactgtccaggattaccgggtgctgtaggat".
                String expected = "ACGCGCCCAATCTCGTCTGTTCTCAGAAGCTAACTTGGATGGGAGACTGTCCAGGATTACCGGGTGCTGTAG";
                assertEquals( expected, collapsed.getSequence() );

            }

        }

        assertTrue( found );

    }

    /*
     * Class under test for Map read(InputStream)
     */
    public final void testReadInputStream() throws Exception {

        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-probes-test.txt" );
        apr.setSequenceField( 5 );
        apr.parse( is );

        String expectedValue = "GCCCCCGTGAGGATGTCACTCAGAT"; // 10
        CompositeSequence cs = getProbeMatchingName( "1004_at" );

        assertNotNull( "CompositeSequence was null", cs );

        boolean foundIt = false;
        for ( Iterator<Reporter> iter = apr.get( cs ).iterator(); iter.hasNext(); ) {
            Reporter element = iter.next();
            if ( element.getName().equals( "1004_at#2:557:275" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();

                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( "Didn't find the probe ", foundIt );
    }

    public final void testReadInputStreamNew() throws Exception {
        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-newprobes-example.txt" );
        apr.setSequenceField( 4 );
        apr.parse( is );

        String expectedValue = "AGCTCAGGTGGCCCCAGTTCAATCT"; // 4
        CompositeSequence cs = getProbeMatchingName( "1000_at" );
        assertNotNull( "CompositeSequence was null", cs );

        boolean foundIt = false;
        for ( Iterator<Reporter> iter = apr.get( cs ).iterator(); iter.hasNext(); ) {
            Reporter element = iter.next();
            if ( element.getName().equals( "1000_at:617:349" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();

                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( "Didn't find the probe ", foundIt );
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        apr = new AffyProbeReader();

    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        apr = null;
        if ( is != null ) is.close();
    }

}
