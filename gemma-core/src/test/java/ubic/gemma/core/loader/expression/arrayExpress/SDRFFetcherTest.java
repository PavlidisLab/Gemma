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
package ubic.gemma.core.loader.expression.arrayExpress;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.LocalFile;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * @author paul
 */
@Category(SlowTest.class)
public class SDRFFetcherTest {

    @Test
    @Category(SlowTest.class)
    public final void testFetch() throws Exception {
        assumeThatResourceIsAvailable( "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/SMDB/E-SMDB-1853/E-SMDB-1853.sdrf.txt" );
        SDRFFetcher f = new SDRFFetcher();
        Collection<LocalFile> fetch = f.fetch( "E-SMDB-1853" );
        assertEquals( 1, fetch.size() );
    }

}
