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

package ubic.gemma.loader.expression.arrayDesign;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeReaderTest extends TestCase {
    protected static final Log log = LogFactory.getLog( AffyProbeReaderTest.class );
    AffyProbeReader apr;
    InputStream is;

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

    /*
     * Class under test for Map read(InputStream)
     */
    public final void testReadInputStream() throws Exception {

        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-probes-test.txt" );
        apr.setSequenceField( 5 );
        apr.parse( is );

        String expectedValue = "GCCCCCGTGAGGATGTCACTCAGAT"; // 10
        CompositeSequence cs = getProbeMatchingName( "1004_at" );

        assertNotNull( "CompositeSequence was null", cs );

        boolean foundIt = false;
        for ( Iterator<Reporter> iter = apr.get( cs ).iterator(); iter.hasNext(); ) {
            Reporter element = iter.next();
            if ( element.getName().equals( "1004_at#2:557:275" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();

                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( "Didn't find the probe ", foundIt );
    }

    public final void testReadInputStreamNew() throws Exception {
        is = AffyProbeReaderTest.class.getResourceAsStream( "/data/loader/affymetrix-newprobes-example.txt" );
        apr.setSequenceField( 4 );
        apr.parse( is );

        String expectedValue = "AGCTCAGGTGGCCCCAGTTCAATCT"; // 4
        CompositeSequence cs = getProbeMatchingName( "1000_at" );
        assertNotNull( "CompositeSequence was null", cs );

        boolean foundIt = false;
        for ( Iterator<Reporter> iter = apr.get( cs ).iterator(); iter.hasNext(); ) {
            Reporter element = iter.next();
            if ( element.getName().equals( "1000_at:617:349" ) ) {
                String actualValue = element.getImmobilizedCharacteristic().getSequence();

                assertEquals( expectedValue, actualValue );
                foundIt = true;
                break;
            }
        }
        assertTrue( "Didn't find the probe ", foundIt );
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        apr = new AffyProbeReader();

    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        apr = null;
        if ( is != null ) is.close();
    }

}
