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
package edu.columbia.gemma.loader.expression.geo;

import java.util.Collection;

import edu.columbia.gemma.common.description.LocalFile;

import junit.framework.TestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class RawDataFetcherTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'edu.columbia.gemma.loader.expression.geo.RawDataFetcher.fetch(String)'
     */
    public void testFetch() {
        RawDataFetcher rdf = new RawDataFetcher();
        Collection<LocalFile> result = rdf.fetch( "GSE1105" );
        assert ( result.size() == 8 );
    }

    /*
     * Test method for 'edu.columbia.gemma.loader.expression.geo.RawDataFetcher.fetch(String)'
     */
    public void testFetchNothingThere() {
        RawDataFetcher rdf = new RawDataFetcher();
        Collection<LocalFile> result = rdf.fetch( "GSE1001" );
        assert ( result == null );
    }

}
