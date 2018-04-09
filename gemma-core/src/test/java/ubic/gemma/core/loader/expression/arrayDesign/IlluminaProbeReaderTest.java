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
package ubic.gemma.core.loader.expression.arrayDesign;

import junit.framework.TestCase;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.InputStream;

/**
 * @author pavlidis
 */
public class IlluminaProbeReaderTest extends TestCase {

    private IlluminaProbeReader apr;

    private InputStream is;

    /**
     * Class under test for Map read(InputStream)
     *
     * @throws Exception when there is a problem
     */
    public final void testReadInputStream() throws Exception {

        TestCase.assertTrue( apr != null );

        apr.parse( is );

        String expectedValue = "GTGGCTGCCTTCCCAGCAGTCTCTACTTCAGCATATCTGGGAGCCAGAAG";

        TestCase.assertTrue( apr.containsKey( "GI_42655756-S" ) );

        Reporter r = apr.get( "GI_42655756-S" );

        TestCase.assertNotNull( "Reporter GI_42655756-S not found", r );

        BioSequence bs = r.getImmobilizedCharacteristic();

        TestCase.assertNotNull( "Immobilized characteristic was null", bs );

        String actualValue = bs.getSequence().toUpperCase();

        TestCase.assertEquals( "Wrong sequence returned", expectedValue, actualValue );

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        apr = new IlluminaProbeReader();
        is = IlluminaProbeReaderTest.class.getResourceAsStream( "/data/loader/illumina-target-test.txt" );
    }

}
