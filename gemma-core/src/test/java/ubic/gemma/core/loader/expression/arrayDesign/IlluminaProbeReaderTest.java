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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pavlidis
 */
public class IlluminaProbeReaderTest {

    private IlluminaProbeReader apr;

    private InputStream is;

    /**
     * Class under test for Map read(InputStream)
     *
     * @throws Exception when there is a problem
     */
    @Test
    public final void testReadInputStream() throws Exception {

        assertTrue( apr != null );

        apr.parse( is );

        String expectedValue = "GTGGCTGCCTTCCCAGCAGTCTCTACTTCAGCATATCTGGGAGCCAGAAG";

        assertTrue( apr.containsKey( "GI_42655756-S" ) );

        Reporter r = apr.get( "GI_42655756-S" );

        assertNotNull( r, "Reporter GI_42655756-S not found" );

        BioSequence bs = r.getImmobilizedCharacteristic();

        assertNotNull( bs, "Immobilized characteristic was null" );

        String actualValue = bs.getSequence().toUpperCase();

        assertEquals( expectedValue, actualValue, "Wrong sequence returned" );

    }

    @BeforeEach
    protected void setUp() throws Exception {
        apr = new IlluminaProbeReader();
        is = IlluminaProbeReaderTest.class.getResourceAsStream( "/data/loader/illumina-target-test.txt" );
    }

}
