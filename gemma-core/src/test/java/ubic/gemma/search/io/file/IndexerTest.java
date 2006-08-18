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
import java.io.IOException;

import ubic.basecode.util.FileTools;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * Tests the functionality of a Lucene Indexer.
 * 
 * @author keshav
 * @version $Id$
 */
public class IndexerTest extends BaseTransactionalSpringContextTest {

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

        indexDir = FileTools.createDir( "gemma-core" + sep + "src" + sep + "test" + sep + "resources" + sep + "data"
                + sep + "search" + sep + "io" + sep + "file" + sep + "index" );

        dataDir = new File( "gemma-core" + sep + "src" + sep + "test" + sep + "resources" + sep + "data" + sep
                + "search" + sep + "io" + sep + "file" );

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onTearDownInTransaction()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();

        log.warn( "cleaning up ... removing index directory" );
        indexDir.getAbsoluteFile().deleteOnExit();// FIXME does not delete the directory

    }

    /**
     * Test the indexer of the Indexer class.
     */
    public void testIndex() {

        boolean fail = false;
        try {
            int indexed = Indexer.index( indexDir, dataDir );
            log.warn( "Indexed items: " + indexed );
        } catch ( IOException e ) {
            fail = true;
            log.error( "Test failure.  Stacktrace is: " );
            e.printStackTrace();
        } finally {
            assertFalse( fail );
        }

    }

}
