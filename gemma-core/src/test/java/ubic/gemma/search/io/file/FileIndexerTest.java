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
package ubic.gemma.search.io.file;

import java.io.File;
import java.util.Arrays;

import ubic.basecode.util.FileTools;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * Tests the functionality of a Lucene Indexer.
 * 
 * @author keshav
 * @version $Id$
 */
public class FileIndexerTest extends BaseTransactionalSpringContextTest {

    /* directories to index and out index results */
    private String sep = null;
    private File indexDir = null;
    private File dataDir = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        sep = System.getProperty( "file.separator" );

        indexDir = FileTools.createDir( ConfigUtils.getString( "gemma.download.dir" ) + sep + "index" );

        dataDir = new File( ConfigUtils.getString( "gemma.home" ) + sep
                + "gemma-core/src/test/resources/data/search/io/file/" );

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onTearDownInTransaction()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();

        FileTools.deleteFiles( Arrays.asList( indexDir.listFiles() ) );
        FileTools.deleteDir( indexDir );
    }

    /**
     * Test the indexer of the Indexer class.
     */
    public void testIndex() {

        boolean fail = false;
        try {
            int indexed = FileIndexer.index( indexDir, dataDir );
            FileSearcher.search( indexDir, "YOL109W" );
            log.warn( "Indexed items: " + indexed );
        } catch ( Exception e ) {
            fail = true;
            log.error( "Test failure.  Stacktrace is: " );
            e.printStackTrace();
        } finally {
            assertFalse( fail );
        }

    }

}
