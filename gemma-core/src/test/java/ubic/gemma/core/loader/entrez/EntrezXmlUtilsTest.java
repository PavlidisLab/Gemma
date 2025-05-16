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
import org.xml.sax.SAXException;

import java.io.IOException;
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
    public void testExtractSearchIds() throws Exception {
        Document document;
        try ( InputStream stream = EntrezXmlUtilsTest.class.getResourceAsStream( "/data/esearchresult.xml" ) ) {
            assertNotNull( stream );
            document = EntrezXmlUtils.parse( stream );
        }
        Collection<String> ids = EntrezXmlUtils.extractSearchIds( document );
        assertEquals( 4, ids.size() );
        assertTrue( ids.contains( "15963425" ) );
    }

    @Test
    public void testExtractFetchIds() throws IOException {
        Document document;
        try ( InputStream stream = EntrezXmlUtilsTest.class.getResourceAsStream( "/data/loader/entrez/efetchresult.xml" ) ) {
            assertNotNull( stream );
            document = EntrezXmlUtils.parse( stream );
        }
        Collection<String> ids = EntrezXmlUtils.extractFetchIds( document );
        assertEquals( 1, ids.size() );
        assertTrue( ids.contains( "5557657" ) );
    }

    @Test
    public void testExtractLinkIds() throws Exception {
        Document document;
        try ( InputStream stream = EntrezXmlUtilsTest.class.getResourceAsStream( "/data/loader/entrez/elinkresult.xml" ) ) {
            assertNotNull( stream );
            document = EntrezXmlUtils.parse( stream );
        }
        Collection<String> ids = EntrezXmlUtils.extractLinkIds( document, "protein", "gene" );
        assertEquals( 2, ids.size() );
        assertTrue( ids.contains( "522311" ) );
    }

    @Test
    public void testParseWithInvalidUtf8Characters() throws IOException {
        try ( InputStream stream = EntrezXmlUtilsTest.class.getResourceAsStream( "/data/GSE730_family.xml" ) ) {
            RuntimeException e = assertThrows( RuntimeException.class, () -> EntrezXmlUtils.parse( stream ) );
            assertTrue( e.getCause() instanceof SAXException );
        }
        try ( InputStream stream = EntrezXmlUtilsTest.class.getResourceAsStream( "/data/GSE730_family.xml" ) ) {
            EntrezXmlUtils.parse( stream, "windows-1252" );
        }
    }
}
