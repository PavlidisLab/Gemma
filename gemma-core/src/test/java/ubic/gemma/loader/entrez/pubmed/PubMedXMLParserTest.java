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
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;
import ubic.gemma.model.common.description.BibliographicReference;

/**
 * @author pavlidis
 * @version $Id$
 */
public class PubMedXMLParserTest extends TestCase {

    private static Log log = LogFactory.getLog( PubMedXMLParserTest.class.getName() );

    InputStream testStream;
    PubMedXMLParser testParser;

    protected void setUp() throws Exception {
        super.setUp();
        testStream = PubMedXMLParserTest.class.getResourceAsStream( "/data/pubmed-test.xml" );
        testParser = new PubMedXMLParser();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        testStream.close();
        testParser = null;
        testStream = null;
    }

    public void testParse() throws Exception {
        try {
            Collection<BibliographicReference> brl = testParser.parse( testStream );
            BibliographicReference br = brl.iterator().next();
            assertEquals( "Lee, Homin K; Hsu, Amy K; Sajdak, Jon; Qin, Jie; Pavlidis, Paul", br.getAuthorList() );
            assertEquals( "Genome Res", br.getPublication() );
            assertEquals( "Coexpression analysis of human genes across many microarray data sets.", br.getTitle() );

            SimpleDateFormat f = new SimpleDateFormat( "mm/HH/MM/dd/yyyy" );
            assertEquals( "00/05/06/03/2004", f.format( br.getPublicationDate() ) );
        } catch ( java.net.UnknownHostException e ) {
            log.warn( "Test skipped due to unknown host exception" );
            return;
        }
    }
}
