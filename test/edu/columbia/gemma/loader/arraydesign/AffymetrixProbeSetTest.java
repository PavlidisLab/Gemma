package edu.columbia.gemma.loader.arraydesign;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;


import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffymetrixProbeSetTest extends TestCase {

    AffymetrixProbeSet tester = null;
    private InputStream iotest;
    protected static final Log log = LogFactory.getLog( AffymetrixProbeSetTest.class );

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        tester = new AffymetrixProbeSet( "foo" );
        tester.add( new AffymetrixProbe( "afoo", "abar", DNATools.createDNASequence( "AAAAAAAA", "A" ), 1 ) );
        tester.add( new AffymetrixProbe( "bfoo", "bbar", DNATools.createDNASequence( "AAAACCCC", "B" ), 5 ) );
        tester.add( new AffymetrixProbe( "cfoo", "cbar", DNATools.createDNASequence( "CCCCGGGG", "C" ), 9 ) );
        tester.add( new AffymetrixProbe( "dfoo", "dbar", DNATools.createDNASequence( "GGGGCCCC", "D" ), 13 ) );
        tester.add( new AffymetrixProbe( "efoo", "ebar", DNATools.createDNASequence( "CCCCTTTT", "E" ), 17 ) );

        iotest = AffymetrixProbeSetTest.class.getResourceAsStream( "/data/loader/100470_at.probes" );
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
        AffymetrixProbeSet t = ( AffymetrixProbeSet ) apr.get( "100470_at" );

        Sequence m = t.collapse();
        log.debug( m.seqString() );
    }

    public final void testCollapse() throws Exception {
        String actualReturn = tester.collapse().seqString().toLowerCase();
        String expectedReturn = "aaaaaaaaccccggggcccctttt";
        assertEquals( expectedReturn, actualReturn );
    }

}
