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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.SlowTest;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

/**
 * Tests command line. This creates an entire new Spring Context so is pretty heavy.
 *
 * @author pavlidis
 */
public class PubMedSearcherIntegrationTest {

    private static final Log log = LogFactory.getLog( PubMedSearcherIntegrationTest.class );
    private final PubMedSearcher p = new PubMedSearcher();

    /**
     * Test method for {@link ubic.gemma.core.loader.entrez.pubmed.PubMedSearcher#executeCommand(String[])}.
     */
    @Test
    @Category(SlowTest.class)
    public final void testMain() {
        try {
            p.executeCommand( new String[]{"-testing", "-v", "3", "hippocampus", "diazepam", "juvenile"} );
        } catch ( Exception e ) {
            assumeFalse( "Test skipped because of UnknownHostException", e instanceof java.net.UnknownHostException );
            assumeFalse( "Test skipped because of a 502 from NCBI", e.getMessage().contains( "code: 503" ) );
            fail( e.getMessage() );
        }
    }

}
