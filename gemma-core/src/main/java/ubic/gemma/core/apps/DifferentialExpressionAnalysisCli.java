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
package ubic.gemma.core.apps;

import gemma.gsec.SecurityService;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 *
 * @author keshav
 */
public class DifferentialExpressionAnalysisCli extends ExpressionExperimentManipulatingCLI {

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;
    /**
     * Whether batch factors should be included (if they exist)
     */
    private boolean ignoreBatch = true;
    private boolean delete = false;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private ExpressionDataFileService expressionDataFileService;

    private final List<Long> factorIds = new ArrayList<>();
    private final List<String> factorNames = new ArrayList<>();
    private Long subsetFactorId;
    private String subsetFactorName;
    private boolean tryToCopyOld = false;
    /*
     * Used when processing a single experiment.
     */
    private AnalysisType type = null;

    /**
     * Use moderated statistics.
     */
    private boolean ebayes = DifferentialExpressionAnalysisConfig.DEFAULT_EBAYES;

    private boolean persist = true;

    public static void main( String[] args ) {
        DifferentialExpressionAnalysisCli analysisCli = new DifferentialExpressionAnalysisCli();
        tryDoWorkNoExit( analysisCli, args );
        System.exit( 0 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "diffExAnalyze";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for differentially expressed genes.";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.apps.AbstractGeneExpressionExperimentManipulatingCLI#buildOptions()
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

        Option factors = OptionBuilder.hasArg().withDescription(
                "ID numbers, categories or names of the factor(s) to use, comma-delimited, with spaces replaced by underscores" )
                .create( "factors" );

        super.addOption( factors );

        Option subsetFactor = OptionBuilder.hasArg().withDescription(
                "ID number, category or name of the factor to use for subsetting the analysis; must also use with -factors" )
                .create( "subset" );
        super.addOption( subsetFactor );

        Option analysisType = OptionBuilder.hasArg().withDescription(
                "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design. "
                        + "Choices are : TWO_WAY_ANOVA_WITH_INTERACTION, "
                        + "TWO_WAY_ANOVA_NO_INTERACTION , OWA (one-way ANOVA), TTEST, OSTTEST (one-sample t-test),"
                        + " GENERICLM (generic LM, no interactions); default: auto-detect" )
                .create( "type" );

        super.addOption( analysisType );

        Option ignoreBatchOption = OptionBuilder.withDescription(
                "If a 'batch' factor is available, use it. Otherwise, batch information can/will be ignored in the analysis." )
                .create( "usebatch" );

        super.addOption( ignoreBatchOption );

        super.addOption( "nodb", false, "Output files only to your gemma.appdata.home instead of database" );

        super.addOption( "redo", false, "If using automatic analysis "
                + "try to base analysis on previous analyses. Will re-run all analyses for the experiment" );

        super.addOption( "delete", false,
                "Instead of running the analysis on the given experiments, delete the old analyses. Use with care!" );

        super.addOption( "ebayes", false,
                "Use emperical-Bayes moderated statistics. Default: " + DifferentialExpressionAnalysisConfig.DEFAULT_EBAYES );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( args );
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

    private void processExperiment( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> results;
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        try {

            ee = this.eeService.thawLite( ee );

            if ( delete ) {
                log.info( "Deleting any analyses for experiment=" + ee );
                differentialExpressionAnalyzerService.deleteAnalyses( ee );
                successObjects.add( "Deleted analysis for: " + ee.toString() );
                return;
            }

            Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
            if ( experimentalFactors.size() == 0 ) {
                if ( this.expressionExperiments.size() == 1 ) {
                    /*
                     * Only need to be noisy if this is the only ee. Batch processing should be less so.
                     */
                    throw new RuntimeException(
                            "Experiment does not have an experimental design populated: " + ee.getShortName() );
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

                log.info( "Using " + factors.size() + " factors provided as arguments" );

                if ( subsetFactor != null ) {
                    if ( factors.contains( subsetFactor ) ) {
                        throw new IllegalArgumentException(
                                "Subset factor cannot also be included as factor to analyze" );
                    }
                    log.info( "Subsetting by " + subsetFactor );

                }

                config.setAnalysisType( this.type );
                config.setFactorsToInclude( factors );
                config.setSubsetFactor( subsetFactor );
                config.setModerateStatistics( this.ebayes );
                config.setPersist( this.persist );

                /*
                 * Interactions included by default. It's actually only complicated if there is a subset factor.
                 */
                if ( type == null && factors.size() == 2 ) {
                    config.getInteractionsToInclude().add( factors );
                }

                results = this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee, config );

            } else {
                /*
                 * Automatically
                 */

                if ( tryToCopyOld ) {
                    tryToRedoBasedOnOldAnalysis( ee );
                }

                Collection<ExperimentalFactor> factorsToUse = new HashSet<>();

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
                                "Experiment has too many factors to run automatically: " + ee.getShortName()
                                        + "; try using the -redo flag to base it on an old analysis, or select factors manually" );
                    }
                    results = tryToRedoBasedOnOldAnalysis( ee );

                } else {

                    assert !factorsToUse.isEmpty();

                    config.setFactorsToInclude( factorsToUse );
                    config.setPersist( this.persist );
                    config.setModerateStatistics( this.ebayes );

                    if ( factorsToUse.size() == 2 ) {
                        // include intearactions by default
                        config.addInteractionToInclude( factorsToUse );
                    }

                    results = this.differentialExpressionAnalyzerService
                            .runDifferentialExpressionAnalyses( ee, config );
                }

            }

            if ( results == null ) {
                throw new Exception( "Failed to process differential expression for experiment " + ee.getShortName() );
            }

            if ( !this.persist ) {
                log.info( "Writing results to disk" );
                for ( DifferentialExpressionAnalysis r : results ) {
                    expressionDataFileService.writeDiffExArchiveFile( ee, r, config );
                }
            }

            successObjects.add( ee.toString() );

        } catch ( Exception e ) {
            log.error( "Error while processing " + ee + ": " + e.getMessage() );
            ExceptionUtils.printRootCauseStackTrace( e );
            errorObjects.add( ee + ": " + e.getMessage() );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.apps.AbstractGeneExpressionExperimentManipulatingCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        differentialExpressionAnalyzerService = this.getBean( DifferentialExpressionAnalyzerService.class );
        differentialExpressionAnalysisService = this.getBean( DifferentialExpressionAnalysisService.class );
        expressionDataFileService = this.getBean( ExpressionDataFileService.class );
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

        if ( hasOption( "ebayes" ) ) {
            this.ebayes = true;
        }

        if ( hasOption( "nodb" ) ) {
            this.persist = false;
        }

        this.tryToCopyOld = hasOption( "redo" );

        if ( hasOption( "factors" ) ) {

            if ( this.tryToCopyOld ) {
                throw new IllegalArgumentException( "You can't specify 'redo' and 'factors' together" );
            }

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

                if ( subsetFactorName.equals( experimentalFactor.getName().replaceAll( " ", "_" ) ) ) {
                    subsetFactor = experimentalFactor;
                } else if ( fvo.getCategory() != null && subsetFactorName
                        .equals( fvo.getCategory().replaceAll( " ", "_" ) ) ) {
                    subsetFactor = experimentalFactor;
                }
            }

            if ( subsetFactor == null )
                throw new IllegalArgumentException( "Didn't find factor for provided subset factor name" );

            return subsetFactor;

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
     */
    private Collection<ExperimentalFactor> guessFactors( ExpressionExperiment ee ) {
        Collection<ExperimentalFactor> factors = new HashSet<>();

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

                if ( factorNames.contains( experimentalFactor.getName().replaceAll( " ", "_" ) ) ) {
                    factors.add( experimentalFactor );
                } else if ( fvo.getCategory() != null && factorNames
                        .contains( fvo.getCategory().replaceAll( " ", "_" ) ) ) {
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
     * Run the analysis using configuration based on an old analysis.
     * 
     */
    private Collection<DifferentialExpressionAnalysis> tryToRedoBasedOnOldAnalysis( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> oldAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        if ( oldAnalyses.isEmpty() ) {
            throw new IllegalArgumentException( "There are no old analyses to redo" );
        }

        log.info( "Will attempt to redo " + oldAnalyses.size() + " analyses for " + ee );
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( DifferentialExpressionAnalysis copyMe : oldAnalyses ) {
            results.addAll(
                    this.differentialExpressionAnalyzerService.redoAnalysis( ee, copyMe, this.persist ) );
        }
        return results;

    }

}
