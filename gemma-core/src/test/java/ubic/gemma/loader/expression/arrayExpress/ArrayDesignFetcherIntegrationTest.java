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
package ubic.gemma.loader.expression.arrayExpress;

import java.util.Collection;

import junit.framework.TestCase;
import ubic.gemma.model.common.description.LocalFile;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignFetcherIntegrationTest extends TestCase {

    /**
     * Test method for {@link ubic.gemma.loader.expression.arrayExpress.ArrayDesignFetcher#fetch(java.lang.String)}.
     */
    public final void testFetch() {
        ArrayDesignFetcher fetcher = new ArrayDesignFetcher();
        Collection<LocalFile> results = fetcher.fetch( "A-AFFY-6" );
        assertEquals( 2, results.size() );
    }

    /**
     * This has no composite sequences - just reporters.
     */
    public final void testFetchNoCompositeSequences() {
        ArrayDesignFetcher fetcher = new ArrayDesignFetcher();
        Collection<LocalFile> results = fetcher.fetch( "A-FPMI-3" );
        assertEquals( 1, results.size() );
    }

}
