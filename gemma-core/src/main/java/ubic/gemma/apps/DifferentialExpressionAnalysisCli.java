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

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
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
    protected boolean ignoreBatch = true;

    /**
     * If there are too many factors, try to do the analysis the way it was done before ('redo')
     */
    private boolean tryToCopyOld = false;

    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    private boolean delete = false;

    private Long subsetFactorId;

    private String subsetFactorName;

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

        Option subsetFactor = OptionBuilder
                .hasArg()
                .withDescription(
                        "ID number or name of the factor to use for subsetting the analysis; must also use with -factors" )
                .create( "subset" );
        super.addOption( subsetFactor );

        Option analysisType = OptionBuilder
                .hasArg()
                .withDescription(
                        "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design. "
                                + "Choices are : TWA (two-way anova with interaction), "
                                + "TWANI (two-way ANOVA without interactions), OWA (one-way ANOVA), TTEST, OSTTEST (one-sample t-test),"
                                + " GENERICLM (generic LM, no interactions)" ).create( "type" );

        super.addOption( analysisType );

        Option ignoreBatchOption = OptionBuilder
                .withDescription(
                        "If a 'batch' factor is available, use it. Otherwise, batch information can/will be ignored in the analysis." )
                .create( "usebatch" );

        super.addOption( ignoreBatchOption );

        super.addOption( "nodb", false, "Output only to STDOUT instead of database" );

        super.addOption( "redo", false, "If using automatic analysis, and there are more than 3 factors available, "
                + "try to base the analysis on a previous analysis. Only works if there is just one old analysis." );

        super.addOption( "delete", false,
                "Instead of running the analyssi on the given experiments, delete the old analyses. Use with care!" );

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

            if ( expressionExperiments.size() > 1 ) {
                log.info( ">>>>>> Begin processing: " + ee );
            }

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
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for differentially expressed genes.";
    }

    private ExperimentalFactor getSubsetFactor( ExpressionExperiment ee ) {
        ExperimentalFactorService efs = this.getBean( ExperimentalFactorService.class );
        ExperimentalFactor subsetFactor = null;
        if ( StringUtils.isNotBlank( this.subsetFactorName ) ) {
            Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
            for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {

                // has already implemented way of figuring out human-friendly name of factor value.
                ExperimentalFactorValueObject fvo = new ExperimentalFactorValueObject( experimentalFactor );

                if ( ignoreBatch && BatchInfoPopulationServiceImpl.isBatchFactor( experimentalFactor ) ) {
                    log.info( "Ignoring batch factor:" + experimentalFactor );
                    continue;
                }

                if ( subsetFactorName.equals( experimentalFactor.getName() ) ) {
                    subsetFactor = experimentalFactor;
                } else if ( fvo.getCategory() != null && subsetFactorName.equals( fvo.getCategory() ) ) {
                    subsetFactor = experimentalFactor;
                }
            }

            if ( subsetFactor == null )
                throw new IllegalArgumentException( "Didn't find factor for provided subset factor name" );

        } else if ( this.subsetFactorId != null ) {
            subsetFactor = efs.load( subsetFactorId );
            if ( subsetFactor == null ) {
                throw new IllegalArgumentException( "No factor for id=" + subsetFactorId );
            }
            return subsetFactor;
        }
        return null;
    }

    /**
     * Determine which factors to use if given from the command line. Only applicable if analysis is on a single data
     * set.
     * 
     * @param ee
     * @return
     */
    private Collection<ExperimentalFactor> guessFactors( ExpressionExperiment ee ) {
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
        Collection<DifferentialExpressionAnalysis> results = null;
        try {

            ee = this.eeService.thawLite( ee );

            if ( delete ) {
                log.info( "Deleting any analyses for experiment=" + ee );
                differentialExpressionAnalyzerService.deleteOldAnalyses( ee );
                successObjects.add( "Deleted analysis for: " + ee.toString() );
                return;
            }

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
                ExperimentalFactor subsetFactor = getSubsetFactor( ee );

                log.info( "Using " + factors.size() + "factors provided as arguments" );

                if ( subsetFactor != null ) {
                    if ( factors.contains( subsetFactor ) ) {
                        throw new IllegalArgumentException(
                                "Subset factor cannot also be included as factor to analyze" );
                    }
                    log.info( "Subsetting by " + subsetFactor );

                }

                DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
                config.setAnalysisType( this.type );
                config.setFactorsToInclude( factors );
                config.setSubsetFactor( subsetFactor );

                /*
                 * FIXME I am pretty sure this is the right thing to do here, to get iterations included by default.
                 * It's actually only complicated if there is a subset factor.
                 */
                if ( type == null && factors.size() == 2 ) {
                    config.getInteractionsToInclude().add( factors );
                }

                this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee, config );

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

                    if ( !tryToCopyOld ) {
                        throw new RuntimeException(
                                "Experiment has too many factors to run automatically: "
                                        + ee.getShortName()
                                        + "; try using the -redo flag to base it on an old analysis, or select factors with the ." );
                    }
                    Collection<DifferentialExpressionAnalysis> oldAnalyses = differentialExpressionAnalysisService
                            .findByInvestigation( ee );

                    if ( oldAnalyses.size() > 1 ) {
                        throw new RuntimeException( "Experiment has too many factors to run automatically: "
                                + ee.getShortName()
                                + " and there is more than one old analysis on which to base the new one" );
                    }

                    DifferentialExpressionAnalysis copyMe = oldAnalyses.iterator().next();
                    differentialExpressionAnalysisService.thaw( copyMe );

                    log.info( "Will base analysis on old one: " + copyMe );
                    DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

                    if ( copyMe.getSubsetFactorValue() != null ) {
                        config.setSubsetFactor( copyMe.getSubsetFactorValue().getExperimentalFactor() );
                    }

                    Collection<ExpressionAnalysisResultSet> resultSets = copyMe.getResultSets();
                    Collection<ExperimentalFactor> factorsFromOldExp = new HashSet<ExperimentalFactor>();
                    for ( ExpressionAnalysisResultSet rs : resultSets ) {
                        Collection<ExperimentalFactor> oldfactors = rs.getExperimentalFactors();
                        factorsFromOldExp.addAll( oldfactors );

                        if ( oldfactors.size() == 2 ) {
                            config.getInteractionsToInclude().add( oldfactors );
                        }

                    }
                    if ( factorsFromOldExp.isEmpty() ) {
                        throw new IllegalStateException( "Old analysis didn't have any factors" );
                    }
                    config.getFactorsToInclude().addAll( factorsFromOldExp );
                    results = this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee, config );

                } else {
                    assert !factorsToUse.isEmpty();
                    results = this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee,
                            factorsToUse );
                }

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        differentialExpressionAnalyzerService = this.getBean( DifferentialExpressionAnalyzerService.class );
        differentialExpressionAnalysisService = this.getBean( DifferentialExpressionAnalysisService.class );
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

        if ( hasOption( "subset" ) ) {
            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "You can only specify the subset factor when analyzing a single experiment" );
            }

            if ( !hasOption( "factors" ) ) {
                throw new IllegalArgumentException( "You have to specify the factors if you also specify the subset" );
            }

            String subsetFactor = getOptionValue( "subset" );
            try {
                this.subsetFactorId = Long.parseLong( subsetFactor );

            } catch ( NumberFormatException e ) {
                this.subsetFactorName = subsetFactor;
            }
        }

        if ( hasOption( "usebatch" ) ) {
            this.ignoreBatch = false;
        }

        if ( hasOption( "delete" ) ) {
            this.delete = true;
        }

        this.tryToCopyOld = hasOption( "redo" );

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
}
