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
package ubic.gemma.core.loader.entrez;

import org.junit.Test;
import org.w3c.dom.Document;
import ubic.gemma.core.util.test.Assumptions;

import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
public class EntrezXmlUtilsTest {

    /*
     * Test method for 'ubic.gemma.core.loader.entrez.pubmed.ESearchXMLParser.parse(InputStream)'
     */
    @Test
    public void testExtractIds() throws Exception {
        Assumptions.assumeThatResourceIsAvailable( EntrezUtils.ESEARCH );
        Document document;
        try ( InputStream stream = EntrezXmlUtilsTest.class.getResourceAsStream( "/data/esearchresult.xml" ) ) {
            assertNotNull( stream );
            document = EntrezXmlUtils.parse( stream );
        }
        Collection<String> ids = EntrezXmlUtils.extractIds( document );
        assertEquals( 4, ids.size() );
        assertTrue( ids.contains( "15963425" ) );
    }
}
