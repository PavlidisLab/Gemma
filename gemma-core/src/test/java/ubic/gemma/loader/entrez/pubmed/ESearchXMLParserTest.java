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
package ubic.gemma.loader.entrez.pubmed;

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ESearchXMLParserTest extends TestCase {

    private static Log log = LogFactory.getLog( ESearchXMLParserTest.class.getName() );

    /*
     * Test method for 'ubic.gemma.loader.entrez.pubmed.ESearchXMLParser.parse(InputStream)'
     */
    public void testParse() throws Exception {
        try (InputStream stream = ESearchXMLParserTest.class.getResourceAsStream( "/data/esearchresult.xml" );) {

            assert stream != null;
            ESearchXMLParser parser = new ESearchXMLParser();
            Collection<String> ids = parser.parse( stream );
            assertTrue( ids.size() == 4 );
            assertTrue( ids.contains( "15963425" ) );
        } catch ( UnknownHostException e ) {
            log.warn( "Test skipped due to unknown host exception" );
            return;
        }
    }
}
