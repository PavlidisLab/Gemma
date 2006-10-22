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
package ubic.gemma.loader.description;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import ubic.gemma.model.common.description.OntologyEntry;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeneOntologyEntryParserTest extends TestCase {

    InputStream is;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/go_daily-termdb.small_test.rdf-xml.gz" ) );
        assert is != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        is.close();
    }

    /**
     * Test method for {@link ubic.gemma.loader.description.GeneOntologyEntryParser#parse(java.io.InputStream)}.
     */
    public final void testParseInputStream() throws Exception {
        GeneOntologyEntryParser parser = new GeneOntologyEntryParser();
        parser.parse( is );
        Collection<OntologyEntry> results = parser.getResults();
        assertEquals( 171, results.size() );
        // check parents and children.
    }

}
