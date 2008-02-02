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
package ubic.gemma.loader.genome.llnl;

import java.util.Collection;

import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesFetcher;
import ubic.gemma.model.common.description.LocalFile;
import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ImageCumulativePlatesFetcherTest extends TestCase {

    /**
     * See {@link ftp://image.llnl.gov/image/outgoing/arrayed_plate_data/cumulative/} for appropriate test files.
     * <p>
     * FIXME this test isn't great because the files get retired from time to time.
     * 
     * @throws Exception
     */
    public void testFetch() throws Exception {
        ImageCumulativePlatesFetcher fetcher = new ImageCumulativePlatesFetcher();
        Collection<LocalFile> files = fetcher.fetch( "20080101" );
        assertEquals( 1, files.size() );
    }

}
