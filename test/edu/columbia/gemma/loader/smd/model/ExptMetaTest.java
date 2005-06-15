package edu.columbia.gemma.loader.smd.model;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import edu.columbia.gemma.loader.expression.smd.model.SMDExperiment;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ExptMetaTest extends TestCase {

    InputStream testStream;
    SMDExperiment emtest;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        testStream = ExptMetaTest.class.getResourceAsStream( "/data/smd.expt-meta.test.txt" );
        emtest = new SMDExperiment();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void read(InputStream)
     */
    public void testReadInputStreamName() throws IOException, SAXException {
        emtest.read( testStream );
        String expectedReturn = "Unfolded Protein Response";
        String actualReturn = emtest.getName();
        assertEquals( expectedReturn, actualReturn );
    }

}
