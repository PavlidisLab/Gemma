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
package ubic.gemma.apps;

import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.diff.DifferentialExpressionAnalysis;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisCli extends AbstractGeneExpressionExperimentManipulatingCLI {

    // private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        /*
         * These options from the super class support: runing on one data set, running on list of data sets from a file,
         * running on all data sets.
         */
        super.buildOptions();

        /* Supports: runing on all data sets that have not been run since a given date. */
        super.addDateOption();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }

        // TODO add a DifferentialExpressionAnalysisService
        // differentialExpressionAnalysisService = ( DifferentialExpressionAnalysisService ) this
        // .getBean( "differentialExpressionAnalysisService" );

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        DifferentialExpressionAnalysisCli analysis = new DifferentialExpressionAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysis.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
