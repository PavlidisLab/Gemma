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

package ubic.gemma.core.loader.expression.arrayDesign;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.InputStream;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author pavlidis
 */
public class AffyProbeReaderTest {
    private AffyProbeReader apr;
    private InputStream is;

    /*
     * Class under test for Map read(InputStream)
     */
    @Test
    public final void testReadInputStream() throws Exception {

        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-probes-test.txt" );
        apr.setSequenceField( 5 );
        apr.parse( is );

        String expectedValue = "GCCCCCGTGAGGATGTCACTCAGAT"; // 10
        CompositeSequence cs = this.getProbeMatchingName( "1004_at" );

        assertNotNull( cs, "CompositeSequence was null" );

        boolean foundIt = false;
        for ( Reporter element : apr.get( cs ) ) {
            if ( element.getName().equals( "1004_at#2:557:275" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();

                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( foundIt, "Didn't find the probe " );
    }

    @Test
    public final void testReadInputStreamNew() throws Exception {
        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-newprobes-example.txt" );
        apr.setSequenceField( 4 );
        apr.parse( is );

        String expectedValue = "AGCTCAGGTGGCCCCAGTTCAATCT"; // 4
        CompositeSequence cs = this.getProbeMatchingName( "1000_at" );
        assertNotNull( cs, "CompositeSequence was null" );

        boolean foundIt = false;
        for ( Reporter element : apr.get( cs ) ) {
            if ( element.getName().equals( "1000_at:617:349" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();

                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( foundIt, "Didn't find the probe " );
    }

    @BeforeEach
    protected void setUp() throws Exception {
        apr = new AffyProbeReader();

    }

    @AfterEach
    protected void tearDown() throws Exception {
        apr = null;
        if ( is != null )
            is.close();
    }

    private CompositeSequence getProbeMatchingName( String name ) {
        Collection<CompositeSequence> keySet = apr.getKeySet();

        CompositeSequence cs = null;
        for ( CompositeSequence compositeSequence : keySet ) {
            if ( compositeSequence.getName().equals( name ) ) {
                cs = compositeSequence;
                break;
            }
        }
        return cs;
    }

}
