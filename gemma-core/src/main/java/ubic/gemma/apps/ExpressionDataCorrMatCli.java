/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.service.AnalysisHelperService;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ConfigUtils;

/**
 * Create correlation visualizations for expression experiments
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionDataCorrMatCli extends ExpressionExperimentManipulatingCLI {

    private FilterConfig filterConfig = new FilterConfig();
    private AnalysisHelperService analysisHelperService = null;
    private File directory;

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note, AuditEventType eventType ) {
        auditTrailService.addUpdateEvent( ee, eventType, "Generated correlation matrix images" );
    }

    private void process( ExpressionExperiment ee ) throws IOException {
        ExpressionDataDoubleMatrix matrix = analysisHelperService.getFilteredMatrix( ee, filterConfig );
        DoubleMatrixNamed cormat = ExpressionDataSampleCorrelation.getMatrix( matrix );
        String fileBaseName = ee.getShortName() + "_corrmat";
        ExpressionDataSampleCorrelation.createMatrixImages( cormat, directory, fileBaseName );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        this.processCommandLine( "corrMat", args );
        this.directory = new File( ConfigUtils.getAnalysisStoragePath() + File.separatorChar
                + ExpressionDataSampleCorrelation.CORRMAT_DIR_NAME );
        if ( !directory.exists() ) {
            boolean success = directory.mkdir();
            if ( !success ) {
                return new IOException( "Could not create directory to store results: " + directory );
            }
        }

        if ( this.getExperimentShortName() == null ) {
            if ( this.experimentListFile == null ) {
                // run on all experiments
                Collection<ExpressionExperiment> all = eeService.loadAll();
                log.info( "Total ExpressionExperiment: " + all.size() );
                for ( ExpressionExperiment ee : all ) {
                    eeService.thawLite( ee );
                    if ( !needToRun( ee, null ) ) {
                        continue;
                    }

                    try {
                        process( ee );
                        successObjects.add( ee.toString() );
                        audit( ee, "Part of run on all EEs", null );
                    } catch ( Exception e ) {
                        errorObjects.add( ee + ": " + e.getMessage() );
                        log.error( ee, e );
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

                        if ( !needToRun( expressionExperiment, null ) ) {
                            continue;
                        }

                        try {
                            process( expressionExperiment );
                            successObjects.add( expressionExperiment.toString() );

                            audit( expressionExperiment, "From list in file: " + experimentListFile, null );
                        } catch ( Exception e ) {
                            errorObjects.add( expressionExperiment + ": " + e.getMessage() );

                            // logFailure( expressionExperiment, e );

                            log.error( e, e );
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
                if ( !needToRun( expressionExperiment, null ) ) {
                    return null;
                }

                try {
                    process( expressionExperiment );
                    audit( expressionExperiment, "From item(s) given from command line", null );
                } catch ( Exception e ) {
                    log.error( e, e );
                    log.error( "**** Exception while processing " + expressionExperiment + ": " + e.getMessage()
                            + " ********" );
                }
            }

        }
        return null;

    }

    public static void main( String[] args ) {
        try {
            ExpressionDataCorrMatCli e = new ExpressionDataCorrMatCli();
            Exception ex = e.doWork( args );
            log.info( ex, ex );
        } catch ( Exception e ) {
            log.info( e, e );
        }
    }

    protected void buildOptions() {
        super.buildOptions();
        this.buildFilterConfigOptions();
    }

    protected void processOptions() {
        super.processOptions();
        getFilterConfigOptions();
        analysisHelperService = ( AnalysisHelperService ) this.getBean( "analysisHelperService" );
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

    @Override
    public String getShortDesc() {
        return "Create visualizations of the sample correlation matrices for expression experiments";
    }

}
