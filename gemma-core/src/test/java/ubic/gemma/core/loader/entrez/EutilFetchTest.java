/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.loader.entrez;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.core.util.test.category.SlowTest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * @author paul
 */
@Category(GeoTest.class)
public class EutilFetchTest {

    @Test
    @Category(SlowTest.class)
    public void testFetch() throws Exception {
        assumeThatResourceIsAvailable( EntrezUtils.ESEARCH );
        String result = new EutilFetch( Settings.getString( "ncbi.efetch.apikey" ) ).fetch( "gds", "GSE4595", 2 );
        assertNotNull( result );
        assertTrue( "Got " + result, result.startsWith( "<?xml" ) );
    }
}
