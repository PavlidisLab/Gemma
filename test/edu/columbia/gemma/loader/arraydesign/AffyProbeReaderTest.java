package edu.columbia.gemma.loader.arraydesign;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
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

    /*
     * Class under test for Map read(InputStream)
     */
    public final void testReadInputStream() throws Exception {
         apr.parse( is );

        String expectedValue = "TCACGGCAGGACAACGAGAAAGCCC";
        String actualValue = ( ( AffymetrixProbeSet ) apr.get( "1004_at" ) ).getProbeSequence( "10" ).toUpperCase();

        assertEquals( expectedValue, actualValue );
    }

}
