package edu.columbia.gemma.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.io.reader.DoubleMatrixReader;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MArrayRawTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMakeMArrayRaw() throws Exception {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrixNamed maGb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maGf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGf.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRf.sample.txt.gz" ) ) );
        MArrayRaw r = new MArrayRaw();
        r.makeMArrayLayout( 4, 4, 22, 24 );
        r.makeMArrayRaw( maRf, maGf, maRb, maGb, null );
    }

    public void testMakeMArrayLayout() throws Exception {
        MArrayRaw r = new MArrayRaw();
        r.makeMArrayLayout( 4, 4, 22, 24 );
    }

    public void testMakeMarrayInfo() throws Exception {
        MArrayRaw r = new MArrayRaw();
        List<String> l = new ArrayList<String>();
        l.add( "foo" );
        l.add( "bar" );
        r.makeMArrayInfo( l );
    }

}
