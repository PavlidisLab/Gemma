/*
 /*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.apps;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.search.IndexServiceImpl;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.ConfigUtils;

/**
 * Simple command line to test the swapping of two different indexes on the same database.
 * 
 * @author klc
 * @version $Id$
 */
public class SwapIndexesTestCli extends AbstractSpringAwareCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        SwapIndexesTestCli p = new SwapIndexesTestCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Total indexing time: " + watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    SearchService ss;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Index Gemma", args );

        if ( err != null ) {
            return err;
        }

        ss = ( SearchService ) getBean( "searchService" );
        IndexServiceImpl indexS = ( IndexServiceImpl ) this.getBean( "indexService" );

        try {

            testSearch();

            indexS.replaceExperimentIndex( ConfigUtils.getString( "gemma.home" ) + "/expression22/index/" );

            testSearch();

        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    private void show( Map<Class<?>, List<SearchResult>> results ) {
        for ( List<SearchResult> ees : results.values() ) {
            for ( SearchResult result : ees ) {
                log.info( "Result:" + result.getResultObject().toString() );
            }
        }

    }

    private void testSearch() {

        log.info( "Search: Khodursky" );
        show( ss.search( SearchSettings.ExpressionExperimentSearch( "Khodursky" ) ) );

        log.info( "Search: Sherlockr" );
        show( ss.search( SearchSettings.ExpressionExperimentSearch( "Sherlock" ) ) );

    }

}
