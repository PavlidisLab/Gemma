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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import edu.columbia.gemma.tools.ProbeThreePrimeLocator.LocationData;
import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeThreePrimeLocatorTest extends TestCase {

    ProbeThreePrimeLocator ptpl;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        ptpl = new ProbeThreePrimeLocator();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'edu.columbia.gemma.tools.ProbeThreePrimeLocator.run(InputStream, Writer)'
     */
    public void testRun() throws Exception {
        InputStream f = this.getClass().getResourceAsStream( "/data/loader/genome/blatResult.wheader.txt" );
        File o = File.createTempFile( "probemappertest.", ".txt" );
        o.deleteOnExit();
        Map<String, Collection<LocationData>> results = ptpl.run( f, new BufferedWriter( new FileWriter( o ) ) );

        File b = File.createTempFile( "probemapperBesttest.", ".txt" );
        b.deleteOnExit();
        ptpl.getBest( results, new BufferedWriter( new FileWriter( b ) ) );

        assertTrue( b.length() != 0 );
        assertTrue( o.length() != 0 );

        o.delete();
        b.delete();
    }

}
