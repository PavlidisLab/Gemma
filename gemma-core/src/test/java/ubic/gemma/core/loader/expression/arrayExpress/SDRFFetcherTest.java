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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author paul
 */
@Tag("slow")
@Category(SlowTest.class)
@Disabled("This test is broken due to a missing remote file. See https://github.com/PavlidisLab/Gemma/issues/766 for details.")
@ExtendWith(NetworkAvailableExtension.class)
public class SDRFFetcherTest {

    @Test
    @Tag("slow")
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/SMDB/E-SMDB-1853/E-SMDB-1853.sdrf.txt")
    public final void testFetch() throws Exception {
        SDRFFetcher f = new SDRFFetcher();
        Collection<File> fetch = f.fetch( "E-SMDB-1853" );
        assertEquals( 1, fetch.size() );
    }
}
