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
package ubic.gemma.search.io.expression.experiment;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * Tests the functionality of Lucene indexing Gemma expression experiments.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentIndexerTest extends BaseTransactionalSpringContextTest {

    /* directories to index and out index results */
    private String sep = null;
    private File indexDir = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private Collection<ExpressionExperiment> expressionExperiments = null;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        sep = System.getProperty( "file.separator" );

        indexDir = FileTools.createDir( ConfigUtils.getString( "gemma.download.dir" ) + sep
                + "expression/experiment/index" );

        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        expressionExperiments = expressionExperimentService.loadAll();

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

        if ( expressionExperiments.size() == 0 ) {
            log.warn( "Expression Experiments do not exist.  Skipping test execution." );
            assertEquals( 0, expressionExperiments.size() );
            return;
        }

        boolean fail = false;
        try {
            int indexed = ExpressionExperimentIndexer.index( indexDir, expressionExperiments );
            log.warn( "Indexed items: " + indexed );

            ExpressionExperimentSearcher.search( indexDir, "second" );
        } catch ( Exception e ) {
            fail = true;
            log.error( "Test failure.  Stacktrace is: " );
            e.printStackTrace();
        } finally {
            assertFalse( fail );
        }
    }
}
