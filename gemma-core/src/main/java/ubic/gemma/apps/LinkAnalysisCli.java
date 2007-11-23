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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.LinkAnalysisConfig;
import ubic.gemma.analysis.linkAnalysis.LinkAnalysisService;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.preprocess.filter.InsufficientSamplesException;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedLinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TooSmallDatasetLinkAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Commandline tool to conduct link analysis
 * 
 * @author xiangwan
 * @author paul (refactoring)
 * @version $Id$
 */
public class LinkAnalysisCli extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        LinkAnalysisCli analysis = new LinkAnalysisCli();
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

    private ExpressionExperimentService eeService = null;

    private ExpressionExperimentReportService expressionExperimentReportService;

    private LinkAnalysisService linkAnalysisService;

    private FilterConfig filterConfig = new FilterConfig();

    private LinkAnalysisConfig linkAnalysisConfig = new LinkAnalysisConfig();

    private AuditTrailService auditTrailService;

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note, AuditEventType eventType ) {
        if ( linkAnalysisConfig.isUseDb() ) {
            expressionExperimentReportService.generateSummaryObject( ee.getId() );
            auditTrailService.addUpdateEvent( ee, eventType, note );
        }
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        super.addDateOption();

        Option geneFileOption = OptionBuilder.hasArg().withArgName( "dataSet" ).withDescription(
                "Short name of the expression experiment to analyze (default is to analyze all found in the database)" )
                .withLongOpt( "dataSet" ).create( 'e' );
        addOption( geneFileOption );

        Option cdfCut = OptionBuilder.hasArg().withArgName( "Tolerance Thresold" ).withDescription(
                "The tolerance threshold for coefficient value" ).withLongOpt( "cdfcut" ).create( 'c' );
        addOption( cdfCut );

        Option tooSmallToKeep = OptionBuilder.hasArg().withArgName( "Cache Threshold" ).withDescription(
                "The threshold for coefficient cache" ).withLongOpt( "cachecut" ).create( 'k' );
        addOption( tooSmallToKeep );

        Option fwe = OptionBuilder.hasArg().withArgName( "Family Wise Error Rate" ).withDescription(
                "The setting for family wise error control" ).withLongOpt( "fwe" ).create( 'w' );
        addOption( fwe );

        buildFilterConfigOptions();

        Option absoluteValue = OptionBuilder.withDescription( "If using absolute value in expression file" )
                .withLongOpt( "abs" ).create( 'a' );
        addOption( absoluteValue );

        Option useDB = OptionBuilder.withDescription( "Don't save the results in the database (i.e., testing)" )
                .withLongOpt( "nodb" ).create( 'd' );
        addOption( useDB );

        Option textOutOpt = OptionBuilder.withDescription( "Output links as text to STOUT" ).create( "text" );
        addOption( textOutOpt );

    }

    @SuppressWarnings("static-access")
    private void buildFilterConfigOptions() {
        Option minPresentFraction = OptionBuilder.hasArg().withArgName( "Missing Value Threshold" ).withDescription(
                "Fraction of data points that must be present in a profile to be retained , default="
                        + FilterConfig.DEFAULT_MINPRESENT_FRACTION ).withLongOpt( "missingcut" ).create( 'm' );
        addOption( minPresentFraction );

        Option lowExpressionCut = OptionBuilder.hasArg().withArgName( "Expression Threshold" ).withDescription(
                "Fraction of expression vectors to reject based on low values, default="
                        + FilterConfig.DEFAULT_LOWEXPRESSIONCUT ).withLongOpt( "lowcut" ).create( 'l' );
        addOption( lowExpressionCut );

        Option lowVarianceCut = OptionBuilder.hasArg().withArgName( "Variance Threshold" ).withDescription(
                "Fraction of expression vectors to reject based on low variance (or coefficient of variation), default="
                        + FilterConfig.DEFAULT_LOWVARIANCECUT ).withLongOpt( "lowvarcut" ).create( "lv" );
        addOption( lowVarianceCut );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }

        this.linkAnalysisService = ( LinkAnalysisService ) this.getBean( "linkAnalysisService" );

        if ( this.getExperimentShortName() == null ) {
            if ( this.experimentListFile == null ) {
                // run on all experiments
                Collection<ExpressionExperiment> all = eeService.loadAll();
                log.info( "Total ExpressionExperiment: " + all.size() );
                for ( ExpressionExperiment ee : all ) {
                    eeService.thawLite( ee );
                    if ( !needToRun( ee, LinkAnalysisEvent.class ) ) {
                        continue;
                    }

                    try {
                        linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );
                        successObjects.add( ee.toString() );
                        audit( ee, "Part of run on all EEs", LinkAnalysisEvent.Factory.newInstance() );
                    } catch ( Exception e ) {
                        errorObjects.add( ee + ": " + e.getMessage() );
                        logFailure( ee, e );
                        log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
                    }
                }
            } else {
                // read short names from specified experiment list file
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

                        if ( !needToRun( expressionExperiment, LinkAnalysisEvent.class ) ) {
                            continue;
                        }

                        try {
                            linkAnalysisService.process( expressionExperiment, filterConfig, linkAnalysisConfig );
                            successObjects.add( expressionExperiment.toString() );

                            audit( expressionExperiment, "From list in file: " + experimentListFile,
                                    LinkAnalysisEvent.Factory.newInstance() );
                        } catch ( Exception e ) {
                            errorObjects.add( expressionExperiment + ": " + e.getMessage() );

                            logFailure( expressionExperiment, e );

                            e.printStackTrace();
                            log.error( "**** Exception while processing " + expressionExperiment + ": "
                                    + e.getMessage() + " ********" );
                        }
                    }
                } catch ( Exception e ) {
                    return e;
                }
            }
            summarizeProcessing();
        } else {
            String[] shortNames = this.getExperimentShortName().split( "," );

            for ( String shortName : shortNames ) {
                ExpressionExperiment expressionExperiment = locateExpressionExperiment( shortName );

                if ( expressionExperiment == null ) {
                    continue;
                }
                eeService.thawLite( expressionExperiment );
                if ( !needToRun( expressionExperiment, LinkAnalysisEvent.class ) ) {
                    return null;
                }

                try {
                    linkAnalysisService.process( expressionExperiment, filterConfig, linkAnalysisConfig );
                    audit( expressionExperiment, "From item(s) given from command line", LinkAnalysisEvent.Factory
                            .newInstance() );
                } catch ( Exception e ) {
                    e.printStackTrace();
                    logFailure( expressionExperiment, e );
                    log.error( "**** Exception while processing " + expressionExperiment + ": " + e.getMessage()
                            + " ********" );
                }
            }

        }
        return null;
    }

    /**
     * @param expressionExperiment
     * @param e
     */
    private void logFailure( ExpressionExperiment expressionExperiment, Exception e ) {
        if ( e instanceof InsufficientSamplesException ) {
            audit( expressionExperiment, e.getMessage(), TooSmallDatasetLinkAnalysisEvent.Factory.newInstance() );
        } else {
            audit( expressionExperiment, ExceptionUtils.getFullStackTrace( e ), FailedLinkAnalysisEvent.Factory
                    .newInstance() );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'c' ) ) {
            this.linkAnalysisConfig.setCdfCut( Double.parseDouble( getOptionValue( 'c' ) ) );
        }
        if ( hasOption( 'k' ) ) {
            this.linkAnalysisConfig.setCorrelationCacheThreshold( Double.parseDouble( getOptionValue( 'k' ) ) );
        }
        if ( hasOption( 'w' ) ) {
            this.linkAnalysisConfig.setFwe( Double.parseDouble( getOptionValue( 'w' ) ) );
        }

        getFilterConfigOptions();

        if ( hasOption( 'a' ) ) {
            this.linkAnalysisConfig.setAbsoluteValue( true );
        }
        if ( hasOption( 'd' ) ) {
            this.linkAnalysisConfig.setUseDb( false );
        }
        if ( hasOption( "text" ) ) {
            this.linkAnalysisConfig.setTextOut( true );
        }
        this.expressionExperimentReportService = ( ExpressionExperimentReportService ) this
                .getBean( "expressionExperimentReportService" );
        this.auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
    }

    private void getFilterConfigOptions() {
        if ( hasOption( 'm' ) ) {
            filterConfig.setMinPresentFraction( Double.parseDouble( getOptionValue( 'm' ) ) );
        }
        if ( hasOption( 'l' ) ) {
            filterConfig.setLowExpressionCut( Double.parseDouble( getOptionValue( 'l' ) ) );
        }
        if ( hasOption( "lv" ) ) {
            filterConfig.setLowVarianceCut( Double.parseDouble( getOptionValue( "lv" ) ) );
        }
    }
}
