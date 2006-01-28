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
package edu.columbia.gemma.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.dataStructure.matrix.DoubleMatrixNamed;
import baseCode.io.reader.DoubleMatrixReader;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MArrayRawTest extends TestCase {
    private static Log log = LogFactory.getLog( MArrayRawTest.class.getName() );
    MArrayRaw r;
    private boolean connected = false;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        try {
            r = new MArrayRaw();
            connected = true;
        } catch ( RuntimeException e ) {
            connected = false;
        }

    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        r.cleanup();
    }

    public void testMakeMArrayRaw() throws Exception {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        DoubleMatrixReader reader = new DoubleMatrixReader();
        DoubleMatrixNamed maGb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maGf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maGf.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRb = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRb.sample.txt.gz" ) ) );
        DoubleMatrixNamed maRf = ( DoubleMatrixNamed ) reader.read( new GZIPInputStream( this.getClass()
                .getResourceAsStream( "/data/swirldata/maRf.sample.txt.gz" ) ) );

        r.makeMArrayLayout( 4, 4, 22, 24 );
        r.makeMArrayRaw( maRf, maGf, maRb, maGb, null );
    }

    public void testMakeMArrayLayout() throws Exception {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        r.makeMArrayLayout( 4, 4, 22, 24 );
    }

    public void testMakeMarrayInfo() throws Exception {
        if ( !connected ) {
            log.warn( "Could not connect to RServe, skipping test." );
            return;
        }
        List<String> l = new ArrayList<String>();
        l.add( "foo" );
        l.add( "bar" );
        r.makeMArrayInfo( l );
    }

}
