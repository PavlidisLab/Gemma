/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein.string;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import ubic.gemma.model.common.description.LocalFile;

/**
 * Test to check that files can be fetched from STRING website. These files are big and take a few hours to download. So
 * for testing purpose chose to test downloading small file: species.mappings.v8.2.txt.gz Current version tested with is
 * 8.2 for string.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class StringProteinFetcherIntegrationTest {

    StringProteinFileFetcher stringProteinLinksDetailedFetcher = null;
    // String testFileName = "http://string.embl.de/newstring_download/species.mappings.v8.2.txt.gz";
    String testFileName = "http://string82.embl.de/newstring_download/species.mappings.v8.2.txt.gz";
    String archiveMethod = "gz";

    /**
     * Set up the fetcher passing in the file to test
     * 
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        stringProteinLinksDetailedFetcher = new StringProteinFileFetcher();
        stringProteinLinksDetailedFetcher.setStringProteinLinksDetailedFileName( testFileName );

    }

    /**
     * Test fetching a small file from string in this case a taxon file.
     */
    @Test
    public void testDownloadProteinAliasFile() {
        Collection<LocalFile> localFile = stringProteinLinksDetailedFetcher.fetch( testFileName );
        String outputfilenamegzip = "";
        String outputfilename = "";

        stringProteinLinksDetailedFetcher.unPackFile( localFile );

        for ( LocalFile file : localFile ) {
            outputfilenamegzip = file.getLocalURL().getFile();
            assertNotNull( outputfilenamegzip );

            int lastIndexOf = outputfilenamegzip.lastIndexOf( "." + archiveMethod );
            if ( lastIndexOf != -1 ) {
                outputfilename = outputfilenamegzip.substring( 0, lastIndexOf );
                // check that the file has been downloaded and unzipped
                assertTrue( new File( outputfilename ).canRead() );
            } else {
                fail();
            }
        }

    }

}
