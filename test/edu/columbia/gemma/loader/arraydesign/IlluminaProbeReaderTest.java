package edu.columbia.gemma.loader.arraydesign;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.designElement.Reporter;

import baseCode.io.reader.BasicLineMapParser;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class IlluminaProbeReaderTest extends TestCase {
    protected static final Log log = LogFactory.getLog( IlluminaProbeReaderTest.class );
    BasicLineMapParser apr;
    InputStream is;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        apr = new IlluminaProbeReader();
        is = IlluminaProbeReaderTest.class.getResourceAsStream( "/data/loader/illumina-target-test.txt" );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for Map read(InputStream)
     */
    public final void testReadInputStream() throws Exception {
        apr.parse( is );

        String expectedValue = "GTGGCTGCCTTCCCAGCAGTCTCTACTTCAGCATATCTGGGAGCCAGAAG";

        String actualValue = ( ( Reporter ) apr.get( "GI_42655756-S" ) ).getImmobilizedCharacteristic().getSequence()
                .toUpperCase();

        assertEquals( expectedValue, actualValue );

    }

}
