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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseCliIntegrationTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.util.test.Assertions.assertThat;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * Tests command line. This creates an entire new Spring Context so is pretty heavy.
 *
 * @author pavlidis
 */
public class PubMedSearcherIntegrationTest extends BaseCliIntegrationTest {

    @Autowired
    private PubMedSearcher p;

    /**
     * Test method for {@link ubic.gemma.core.loader.entrez.pubmed.PubMedSearcher#executeCommand(ubic.gemma.core.util.CliContext)}.
     */
    @Test
    @Category(SlowTest.class)
    public final void testMain() {
        assumeThatResourceIsAvailable( "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi" );
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThat( p )
                .withArguments( "hippocampus", "diazepam", "juvenile" )
                .withOutputStream( os )
                .succeeds();
        assertThat( os.toString() )
                .matches( "\\d+ references found\n" );
    }
}
