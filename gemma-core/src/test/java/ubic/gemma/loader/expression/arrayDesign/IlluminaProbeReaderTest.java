/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class IlluminaProbeReaderTest extends TestCase {
    protected static final Log log = LogFactory.getLog( IlluminaProbeReaderTest.class );

    IlluminaProbeReader apr;

    InputStream is;

    /*
     * Class under test for Map read(InputStream)
     */
    public final void testReadInputStream() throws Exception {

        assertTrue( apr != null );

        apr.parse( is );

        String expectedValue = "GTGGCTGCCTTCCCAGCAGTCTCTACTTCAGCATATCTGGGAGCCAGAAG";

        assertTrue( apr.containsKey( "GI_42655756-S" ) );

        Reporter r = apr.get( "GI_42655756-S" );

        assertNotNull( "Reporter GI_42655756-S not found", r );

        BioSequence bs = r.getImmobilizedCharacteristic();

        assertNotNull( "Immobilized characteristic was null", bs );

        String actualValue = bs.getSequence().toUpperCase();

        assertEquals( "Wrong sequence returned", expectedValue, actualValue );

    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        apr = new IlluminaProbeReader();
        is = IlluminaProbeReaderTest.class.getResourceAsStream( "/data/loader/illumina-target-test.txt" );
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
