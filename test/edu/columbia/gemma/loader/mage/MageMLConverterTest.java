package edu.columbia.gemma.loader.mage;

import java.io.InputStream;

import org.biomage.BioSequence.BioSequence;

import junit.framework.TestCase;

public class MageMLConverterTest extends TestCase {

    MageMLParser mlp;
    MageMLConverter mlc;

    InputStream bs;
    InputStream cs;
    InputStream rep;
    InputStream qt;
    InputStream desel;
    InputStream bassdat;

    protected void setUp() throws Exception {
        super.setUp();
        mlp = new MageMLParser();

        mlc = new MageMLConverter();

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mlp = null;
        mlc = null;
    }

    public final void testConvert() throws Exception {
        // mlp.parse( MageMLConverterTest.class.getResourceAsStream( "/data/mage/MGP-Biosequence.xml" ) );
        BioSequence bst = new BioSequence();
        Object result = mlc.convert( bst );
        assertTrue( result instanceof edu.columbia.gemma.genome.biosequence.BioSequence );

    }

}
