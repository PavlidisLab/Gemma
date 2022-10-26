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
package ubic.gemma.web.services;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GemmaEndpointTest {

    /*
     * Test method for 'ubic.gemma.web.services.GemmaEndpointTest.readReport'
     */
    @Test
    public void testReadReport() throws Exception {

        class TestEndpoint extends AbstractGemmaEndpoint {
            public TestEndpoint() {

            }

            public Document readTest() throws IOException {
                try ( InputStream stream = GemmaEndpointTest.class.getResourceAsStream( "/data/DEDVforEE-159-test.xml" ); ) {
                    assert stream != null;
                    return this.readReport( stream );
                }
            }

            @Override
            protected Element invokeInternal( Element requestElement, Document document ) {
                return null;
            }
        }

        TestEndpoint testEp = new TestEndpoint();
        Document doc = testEp.readTest();

        assertNotNull( doc );

        NodeList nl = doc.getElementsByTagName( "dedv" );
        int numActual = nl.getLength();
        assertEquals( 12625, numActual ); // used grep -o "<dedv>" DEDVforEE-159-test.xml | wc -l to verify

    }
}
