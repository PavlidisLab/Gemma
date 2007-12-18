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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.diff.DifferentialExpressionAnalysis;
import ubic.gemma.analysis.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.analysis.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisCli extends ExpressionExperimentManipulatingCLI {
    private static Log log = LogFactory.getLog( DifferentialExpressionAnalysisCli.class );

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    private int top = 100;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        /*
         * These options from the super class support: running on one data set, running on list of data sets from a
         * file, running on all data sets.
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

        if ( hasOption( 't' ) ) {
            this.top = Integer.parseInt( ( getOptionValue( 't' ) ) );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Differential Expression Analysis", args );
        if ( err != null ) {
            return err;
        }

        this.differentialExpressionAnalysisService = ( DifferentialExpressionAnalysisService ) this
                .getBean( "differentialExpressionAnalysisService" );

        if ( this.getExperimentShortName() == null ) {
            /* nothing on the command line */
            if ( this.experimentListFile == null ) {
                /* no file, so run on all experiments */
                Collection<ExpressionExperiment> all = eeService.loadAll();
                log.info( "Total ExpressionExperiment: " + all.size() );
                for ( ExpressionExperiment ee : all ) {
                    eeService.thawLite( ee );
                    if ( !needToRun( ee, DifferentialExpressionAnalysisEvent.class ) ) {
                        continue;
                    }

                    try {
                        this.differentialExpressionAnalysisService.getTopResults( top, ee );
                        successObjects.add( ee.toString() );
                        // TODO add auditing
                        // audit( ee, "Part of run on all EEs",
                        // DifferentialExpressionAnalysisEvent.Factory.newInstance() );
                    } catch ( Exception e ) {
                        errorObjects.add( ee + ": " + e.getMessage() );
                        // TODO add logFailure
                        // logFailure( ee, e );
                        log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
                    }
                }
            } else {
                /* read short names from specified experiment list file */
                try {
                    InputStream is = new FileInputStream( this.experimentListFile );
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                    String shortName = null;
                    while ( ( shortName = br.readLine() ) != null ) {
                        if ( StringUtils.isBlank( shortName ) ) continue;
                        ExpressionExperiment expressionExperiment = eeService.findByShortName( shortName );

                        if ( expressionExperiment == null ) {
                            errorObjects.add( shortName + " is not found in the database! " );
                            continue;
                        }

                        eeService.thawLite( expressionExperiment );

                        if ( !needToRun( expressionExperiment, DifferentialExpressionAnalysisEvent.class ) ) {
                            continue;
                        }

                        try {
                            this.differentialExpressionAnalysisService.getTopResults( shortName, top );
                            successObjects.add( expressionExperiment.toString() );
                            // TODO add audit
                            // audit( expressionExperiment, "From list in file: " + experimentListFile,
                            // LinkAnalysisEvent.Factory.newInstance() );
                        } catch ( Exception e ) {
                            errorObjects.add( expressionExperiment + ": " + e.getMessage() );
                            // TODO add logFailure
                            // logFailure( expressionExperiment, e );

                            e.printStackTrace();
                            log.error( "**** Exception while processing " + expressionExperiment + ": "
                                    + e.getMessage() + " ********" );
                        }
                    }
                } catch ( Exception e ) {
                    return e;
                }
            }
        } else {
            /* read short names from the command line */
            String[] shortNames = this.getExperimentShortName().split( "," );

            for ( String shortName : shortNames ) {
                ExpressionExperiment expressionExperiment = locateExpressionExperiment( shortName );

                if ( expressionExperiment == null ) continue;

                eeService.thaw( expressionExperiment );

                Collection<ExpressionAnalysis> expressionAnalyses = this.differentialExpressionAnalysisService
                        .getExpressionAnalyses( expressionExperiment );

                summarizeProcessing( expressionAnalyses );

            }

        }

        return null;
    }

    /**
     * @param expressionAnalyses
     */
    private void summarizeProcessing( Collection<ExpressionAnalysis> expressionAnalyses ) {
        for ( ExpressionAnalysis analysis : expressionAnalyses ) {
            summarizeProcessing( analysis );
        }
    }

    /**
     * @param expressionAnalysis
     */
    private void summarizeProcessing( ExpressionAnalysis expressionAnalysis ) {

        // super.summarizeProcessing();

        log.info( "Results for expression analysis of type: " + expressionAnalysis.getName() );
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        log.info( resultSets.size() + " to process." );
        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

            Collection<ExpressionAnalysisResult> results = resultSet.getResults();

            for ( ExpressionAnalysisResult result : results ) {
                ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) result;
                log.debug( "probe: " + probeResult.getProbe().getName() + ", p-value: " + probeResult.getPvalue()
                        + ", score: " + probeResult.getScore() );
            }
            log.info( "Result set processed with " + results.size() + " results." );
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        DifferentialExpressionAnalysisCli analysisCli = new DifferentialExpressionAnalysisCli();
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

}
