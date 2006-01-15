package edu.columbia.gemma.loader.expression.arrayDesign;

import java.io.InputStream;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeReaderTest extends TestCase {
    protected static final Log log = LogFactory.getLog( AffyProbeReaderTest.class );
    AffyProbeReader apr;
    InputStream is;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        apr = new AffyProbeReader();
        apr.setSequenceField( 5 );
        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-probes-test.txt" );

    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        apr = null;
        is.close();
    }

    public final void testReadInputStreamNew() throws Exception {
        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-newprobes-example.txt" );
        apr.setSequenceField( 4 );
        apr.parse( is );

        String expectedValue = "AGCTCAGGTGGCCCCAGTTCAATCT"; // 4
        CompositeSequence cs = ( ( CompositeSequence ) apr.get( "1000_at" ) );

        assertTrue( "CompositeSequence was null", cs != null );

        boolean foundIt = false;
        for ( Iterator iter = cs.getReporters().iterator(); iter.hasNext(); ) {
            Reporter element = ( Reporter ) iter.next();
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
     * Class under test for Map read(InputStream)
     */
    public final void testReadInputStream() throws Exception {

        assertTrue( "InputStream was null", is != null );

        apr.parse( is );

        String expectedValue = "TCACGGCAGGACAACGAGAAAGCCC"; // 10
        CompositeSequence cs = ( ( CompositeSequence ) apr.get( "1004_at" ) );

        assertTrue( "CompositeSequence was null", cs != null );

        boolean foundIt = false;
        for ( Iterator iter = cs.getReporters().iterator(); iter.hasNext(); ) {
            Reporter element = ( Reporter ) iter.next();
            if ( element.getName().equals( "1004_at:265:573" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();

                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( "Didn't find the probe ", foundIt );
    }

}
