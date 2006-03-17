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
package ubic.gemma.loader.util;

import java.util.Collection;

import junit.framework.TestCase;
import ubic.gemma.loader.util.fetcher.HttpFetcher;
import ubic.gemma.model.common.description.LocalFile;

/**
 * @author pavlidis
 * @version $Id$
 */
public class HttpFetcherTest extends TestCase {

    /*
     * Test method for 'ubic.gemma.loader.loaderutils.HttpFetcher.fetch(String)'
     */
    public void testFetch() {
        HttpFetcher hf = new HttpFetcher();
        Collection<LocalFile> results = hf.fetch( "http://www.yahoo.com" );
        results.iterator().next();
    }

}
