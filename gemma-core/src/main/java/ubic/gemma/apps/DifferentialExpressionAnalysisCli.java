/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.security.SecurityService;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisCli extends ExpressionExperimentManipulatingCLI {

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
        System.exit( 0 );
    }

    protected DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    /*
     * Used when processing a single experiment.
     */
    private AnalysisType type = null;
    private List<Long> factorIds = new ArrayList<Long>();
    private List<String> factorNames = new ArrayList<String>();

    /**
     * Whether batch factors should be included (if they exist)
     */
    protected boolean ignoreBatch = false;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for differentially expressed genes.";
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

        Option topOpt = OptionBuilder.withLongOpt( "top" ).hasArg( true )
                .withDescription( "The top (most significant) results to display." ).create();
        super.addOption( topOpt );

        super.addAutoOption();
        this.autoSeekEventType = DifferentialExpressionAnalysisEvent.class;
        super.addForceOption( null );

        // Option forceAnalysisOpt = OptionBuilder.hasArg( false ).withDescription( "Force the run." ).create( 'r' );
        // super.addOption( forceAnalysisOpt );

        Option factors = OptionBuilder.hasArg()
                .withDescription( "ID numbers or names of the factor(s) to use, comma-delimited" ).create( "factors" );

        super.addOption( factors );

        Option analysisType = OptionBuilder
                .hasArg()
                .withDescription(
                        "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design. "
                                + "Choices are : TWA (two-way anova), TWIA (two-way ANOVA with interactions), OWA (one-way ANOVA), TTEST" )
                .create( "type" );

        super.addOption( analysisType );

        Option ignoreBatchOption = OptionBuilder
                .withDescription(
                        "If a 'batch' factor is available, ignore it. Otherwise, batch information can/will be included in the analysis." )
                .create( "ignorebatch" );

        super.addOption( ignoreBatchOption );

        super.addOption( "nodb", false, "Output only to STDOUT instead of database" );

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

        SecurityService securityService = this.getBean( SecurityService.class );

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( !( ee instanceof ExpressionExperiment ) ) {
                continue;
            }

            log.info( ee );

            /*
             * This is really only important when running as admin and in a batch mode.
             */
            log.debug( securityService.getOwner( ee ) );

            if ( !securityService.isOwnedByCurrentUser( ee ) && this.expressionExperiments.size() > 1 ) {
                log.warn( "Experiment is not owned by current user, skipping: " + ee );
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
        differentialExpressionAnalyzerService = this.getBean( DifferentialExpressionAnalyzerService.class );

        if ( hasOption( "type" ) ) {

            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "You can only specify the analysis type when analyzing a single experiment" );
            }

            if ( !hasOption( "factors" ) ) {
                throw new IllegalArgumentException( "Please specify the factor(s) when specifying the analysis type." );
            }
            this.type = AnalysisType.valueOf( getOptionValue( "type" ) );
        }

        if ( hasOption( "ignorebatch" ) ) {
            this.ignoreBatch = true;
        }

        if ( hasOption( "factors" ) ) {

            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "You can only specify the factors when analyzing a single experiment" );
            }

            String rawfactors = getOptionValue( "factors" );
            String[] factorIDst = StringUtils.split( rawfactors, "," );
            if ( factorIDst != null && factorIDst.length > 0 ) {
                for ( String string : factorIDst ) {
                    try {
                        Long factorId = Long.parseLong( string );
                        this.factorIds.add( factorId );
                    } catch ( NumberFormatException e ) {
                        this.factorNames.add( string );
                    }
                }
            }
        }
    }

    /**
     * Determine which factors to use if given from the command line. Only applicable if analysis is on a single data
     * set.
     * 
     * @param ee
     * @return
     */
    protected Collection<ExperimentalFactor> guessFactors( ExpressionExperiment ee ) {
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();

        ExperimentalFactorService efs = this.getBean( ExperimentalFactorService.class );
        if ( this.factorNames.size() > 0 ) {
            if ( this.factorIds.size() > 0 ) {
                throw new IllegalArgumentException( "Please provide factor names or ids, not a mixture of each" );
            }
            Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
            for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {

                // has already implemented way of figuring out human-friendly name of factor value.
                ExperimentalFactorValueObject fvo = new ExperimentalFactorValueObject( experimentalFactor );

                if ( ignoreBatch && BatchInfoPopulationServiceImpl.isBatchFactor( experimentalFactor ) ) {
                    log.info( "Ignoring batch factor:" + experimentalFactor );
                    continue;
                }

                if ( factorNames.contains( experimentalFactor.getName() ) ) {
                    factors.add( experimentalFactor );
                } else if ( fvo.getCategory() != null && factorNames.contains( fvo.getCategory() ) ) {
                    factors.add( experimentalFactor );
                }
            }

            if ( factors.size() != factorNames.size() ) {
                throw new IllegalArgumentException( "Didn't find factors for all the provided factor names" );
            }

        } else if ( this.factorIds.size() > 0 ) {
            for ( Long factorId : factorIds ) {
                if ( this.factorNames.size() > 0 ) {
                    throw new IllegalArgumentException( "Please provide factor names or ids, not a mixture of each" );
                }
                ExperimentalFactor factor = efs.load( factorId );
                if ( factor == null ) {
                    throw new IllegalArgumentException( "No factor for id=" + factorId );
                }
                if ( !factor.getExperimentalDesign().equals( ee.getExperimentalDesign() ) ) {
                    throw new IllegalArgumentException( "Factor with id=" + factorId + " does not belong to " + ee );
                }

                if ( ignoreBatch && BatchInfoPopulationServiceImpl.isBatchFactor( factor ) ) {
                    log.warn( "Selected factor looks like a batch, and 'ignoreBatch' is true, skipping:" + factor );
                    continue;
                }

                factors.add( factor );
            }
        }

        return factors;
    }

    /**
     * @param ee
     */
    protected void processExperiment( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> results;
        try {

            ee = this.eeService.thawLite( ee );

            Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
            if ( experimentalFactors.size() == 0 ) {
                if ( this.expressionExperiments.size() == 1 ) {
                    /*
                     * Only need to be noisy if this is the only ee. Batch processing should be less so.
                     */
                    throw new RuntimeException( "Experiment does not have an experimental design populated: "
                            + ee.getShortName() );
                }
                log.warn( "Experiment does not have an experimental design populated: " + ee.getShortName() );
                return;
            }

            Collection<ExperimentalFactor> factors = guessFactors( ee );

            if ( factors.size() > 0 ) {
                /*
                 * Manual selection of factors
                 */
                if ( this.type != null ) {
                    results = this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee,
                            factors, type );
                } else {
                    results = this.differentialExpressionAnalyzerService
                            .runDifferentialExpressionAnalyses( ee, factors );
                }
            } else {
                /*
                 * Automagically
                 */

                Collection<ExperimentalFactor> factorsToUse = new HashSet<ExperimentalFactor>();

                if ( this.ignoreBatch ) {
                    for ( ExperimentalFactor ef : experimentalFactors ) {
                        if ( !ExperimentalDesignUtils.isBatch( ef ) ) {
                            factorsToUse.add( ef );
                        }
                    }
                } else {

                    factorsToUse.addAll( experimentalFactors );
                }

                if ( factorsToUse.isEmpty() ) {
                    throw new RuntimeException( "No factors available for " + ee.getShortName() );
                }

                if ( factorsToUse.size() > 3 ) {
                    throw new RuntimeException( "Experiment has too many factors to run automatically: "
                            + ee.getShortName() );
                }

                results = this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee,
                        factorsToUse );
            }

            if ( results == null ) {
                throw new Exception( "Failed to process differential expression for experiment " + ee.getShortName() );
            }

            // expressionDataFileService.writeOrLocateDiffExpressionDataFile( results.iterator().next(), true );

            successObjects.add( ee.toString() );

        } catch ( Exception e ) {
            log.error( "Error while processing " + e + ": " + e.getMessage() );
            log.error( e, e );
            errorObjects.add( ee + ": " + e.getMessage() );
        }

    }
}
