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
package ubic.gemma.core.loader.entrez.pubmed;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * @author pavlidis
 */
public class ESearchXMLParserTest extends TestCase {

    private static final Log log = LogFactory.getLog( ESearchXMLParserTest.class.getName() );

    /*
     * Test method for 'ubic.gemma.core.loader.entrez.pubmed.ESearchXMLParser.parse(InputStream)'
     */
    public void testParse() throws Exception {
        try (InputStream stream = ESearchXMLParserTest.class.getResourceAsStream( "/data/esearchresult.xml" )) {

            assert stream != null;
            ESearchXMLParser parser = new ESearchXMLParser();
            Collection<String> ids = parser.parse( stream );
            TestCase.assertTrue( ids.size() == 4 );
            TestCase.assertTrue( ids.contains( "15963425" ) );
        } catch ( UnknownHostException e ) {
            ESearchXMLParserTest.log.warn( "Test skipped due to unknown host exception" );
        }
    }
}
