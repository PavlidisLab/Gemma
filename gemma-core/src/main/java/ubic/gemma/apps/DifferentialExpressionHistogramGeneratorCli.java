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

import org.apache.commons.lang3.time.StopWatch;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A command line interface to create histograms for datasets that have had the differential expression analysis run on
 * them.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionHistogramGeneratorCli extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        DifferentialExpressionHistogramGeneratorCli analysisCli = new DifferentialExpressionHistogramGeneratorCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysisCli.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Generates histograms for datasets that have had differential expression run on them.";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {

        /*
         * These options from the super class support: running on one or more data sets from the command line, running
         * on list of data sets from a file, running on all data sets.
         */
        super.buildOptions();

        /* Supports: running on all data sets that have not been run since a given date. */
        super.addDateOption();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Generate Histograms", args );
        if ( err != null ) {
            return err;
        }

        this.differentialExpressionAnalyzerService = this.getBean( DifferentialExpressionAnalyzerService.class );

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( !( ee instanceof ExpressionExperiment ) ) {
                continue;
            }
            processExperiment( ( ExpressionExperiment ) ee );
        }
        summarizeProcessing();

        return null;
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

    /**
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        try {
            this.differentialExpressionAnalyzerService.updateScoreDistributionFiles( ee );

            successObjects.add( ee.toString() );

        } catch ( Exception e ) {
            e.printStackTrace();
            errorObjects.add( ee + ": " + e.getMessage() );
        }
    }

}
