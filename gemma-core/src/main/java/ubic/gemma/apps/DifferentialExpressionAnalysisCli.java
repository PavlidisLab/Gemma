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

import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisCli extends ExpressionExperimentManipulatingCLI {

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    private ExpressionExperimentReportService expressionExperimentReportService = null;

    private int top = 100;

    private boolean useDb = true;

    private boolean forceAnalysis = false;

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Differential Expression Analysis", args );
        if ( err != null ) {
            return err;
        }

        this.differentialExpressionAnalyzerService = ( DifferentialExpressionAnalyzerService ) this
                .getBean( "differentialExpressionAnalyzerService" );

        this.expressionExperimentReportService = ( ExpressionExperimentReportService ) this
                .getBean( "expressionExperimentReportService" );

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( !( ee instanceof ExpressionExperiment ) ) {
                continue;
            }
            processExperiment( ( ExpressionExperiment ) ee );
        }
        summarizeProcessing();

        return null;
    }

    /**
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        try {
            Collection<DifferentialExpressionAnalysis> expressionAnalyses = this.differentialExpressionAnalyzerService
                    .getDifferentialExpressionAnalyses( ee, forceAnalysis );

            if ( expressionAnalyses == null ) {
                throw new Exception( "Did not process differential expression for experiment " + ee.getShortName() );
            }

            logProcessing( expressionAnalyses );

            successObjects.add( ee.toString() );

            audit( ee, "", DifferentialExpressionAnalysisEvent.Factory.newInstance() );
        } catch ( Exception e ) {
            e.printStackTrace();
            errorObjects.add( ee + ": " + e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        /*
         * These options from the super class support: running on one or more data sets from the command line, running
         * on list of data sets from a file, running on all data sets.
         */
        super.buildOptions();

        /* Supports: running on all data sets that have not been run since a given date. */
        super.addDateOption();

        Option topOpt = OptionBuilder.withLongOpt( "top" ).hasArg( true ).withDescription(
                "The top (most significant) results to display." ).create();
        super.addOption( topOpt );

        Option forceAnalysisOpt = OptionBuilder.hasArg( false ).withDescription( "Force the run." ).create( 'r' );
        super.addOption( forceAnalysisOpt );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( "top" ) ) {
            this.top = Integer.parseInt( ( getOptionValue( "top" ) ) );
        }

        this.forceAnalysis = hasOption( 'r' );

    }

    /**
     * @param expressionAnalyses
     */
    private void logProcessing( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        for ( DifferentialExpressionAnalysis analysis : expressionAnalyses ) {
            logProcessing( analysis );
        }
    }

    /**
     * @param expressionAnalysis
     */
    private void logProcessing( DifferentialExpressionAnalysis expressionAnalysis ) {

        log.debug( "Summarizing results for expression analysis of type: " + expressionAnalysis.getName() );
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        log.debug( resultSets.size() + " result set(s) to process." );
        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            log.debug( "*** Result set ***" );
            Collection<DifferentialExpressionAnalysisResult> results = resultSet.getResults();

            for ( DifferentialExpressionAnalysisResult result : results ) {
                ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) result;
                log.debug( "probe: " + probeResult.getProbe().getName() + ", p-value: " + probeResult.getPvalue()
                        + ", score: " + probeResult.getScore() );
            }
            log.debug( "Result set processed with " + results.size() + " results." );
        }
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note, AuditEventType eventType ) {
        if ( useDb ) {
            expressionExperimentReportService.generateSummaryObject( ee.getId() );
            auditTrailService.addUpdateEvent( ee, eventType, note );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for differentially expressed genes.";
    }

}
