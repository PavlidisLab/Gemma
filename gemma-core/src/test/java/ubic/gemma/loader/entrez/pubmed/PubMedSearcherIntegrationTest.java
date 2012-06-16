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

import ubic.gemma.apps.AbstractCLITestCase;

/**
 * Tests command line. This creates an entire new Spring Context so is pretty heavy.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PubMedSearcherIntegrationTest extends AbstractCLITestCase {
    PubMedSearcher p = new PubMedSearcher();

    /**
     * Test method for {@link ubic.gemma.loader.entrez.pubmed.PubMedSearcher#main(java.lang.String[])}.
     */
    public final void testMain() {

        Exception result = p.doWork( new String[] { "-testing", "-v", "3", "hippocampus", "diazepam", "juvenile" } );
        if ( result != null ) {
            if ( result instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped because of UnknownHostException" );
                return;
            } else if ( result.getMessage().contains( "code: 503" ) ) {
                log.warn( "Test skipped because of a 502 from NCBI" );
                return;
            }
            fail( result.getMessage() );
        }

    }

}
