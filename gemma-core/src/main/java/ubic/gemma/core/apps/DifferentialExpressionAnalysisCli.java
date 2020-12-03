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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;

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

    private final List<Long> factorIds = new ArrayList<>();
    private final List<String> factorNames = new ArrayList<>();
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;
    /**
     * Whether batch factors should be included (if they exist)
     */
    private boolean ignoreBatch = true;
    private boolean delete = false;
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private ExpressionDataFileService expressionDataFileService;
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

    @Override
    public String getCommandName() {
        return "diffExAnalyze";
    }

    @Override
    protected void doWork() throws Exception {
        SecurityService securityService = this.getBean( SecurityService.class );

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( !( ee instanceof ExpressionExperiment ) ) {
                continue;
            }

            if ( expressionExperiments.size() > 1 ) {
                AbstractCLI.log.info( ">>>>>> Begin processing: " + ee );
            }

            /*
             * This is really only important when running as admin and in a batch mode.
             */
            AbstractCLI.log.debug( securityService.getOwner( ee ) );

            if ( !securityService.isOwnedByCurrentUser( ee ) && this.expressionExperiments.size() > 1 ) {
                AbstractCLI.log.warn( "Experiment is not owned by current user, skipping: " + ee );
                continue;
            }

            this.processExperiment( ( ExpressionExperiment ) ee );
        }
    }

    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for differentially expressed genes.";
    }

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

        //
        //        Option topOpt = Option.builder( "top" ).hasArg().argName( "number" ).desc( "The top (most significant) results to display." )
        //                .build();
        //        super.addOption( topOpt );

        super.addAutoOption();
        this.autoSeekEventType = DifferentialExpressionAnalysisEvent.class;
        super.addForceOption();

        Option factors = Option.builder( "factors" ).hasArg().desc(
                "ID numbers, categories or names of the factor(s) to use, comma-delimited, with spaces replaced by underscores" )
                .build();

        super.addOption( factors );

        Option subsetFactor = Option.builder( "subset" ).hasArg().desc(
                "ID number, category or name of the factor to use for subsetting the analysis; must also use with -factors" )
                .build();
        super.addOption( subsetFactor );

        Option analysisType = Option.builder( "type" ).hasArg().desc(
                "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design. "
                        + "Choices are : TWO_WAY_ANOVA_WITH_INTERACTION, "
                        + "TWO_WAY_ANOVA_NO_INTERACTION , OWA (one-way ANOVA), TTEST, OSTTEST (one-sample t-test),"
                        + " GENERICLM (generic LM, no interactions); default: auto-detect" )
                .build();

        super.addOption( analysisType );

        Option ignoreBatchOption = Option.builder( "usebatch" ).desc(
                "If a 'batch' factor is available, use it. Otherwise, batch information can/will be ignored in the analysis." )
                .build();

        super.addOption( ignoreBatchOption );

        super.addOption( "nodb", "Output files only to your gemma.appdata.home instead of database" );

        super.addOption( "redo", "If using automatic analysis "
                + "try to base analysis on previous analyses. Will re-run all analyses for the experiment" );

        super.addOption( "delete",
                "Instead of running the analysis on the given experiments, remove the old analyses. Use with care!" );

        super.addOption( "ebayes", "Use empirical-Bayes moderated statistics. Default: "
                + DifferentialExpressionAnalysisConfig.DEFAULT_EBAYES );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        differentialExpressionAnalyzerService = this.getBean( DifferentialExpressionAnalyzerService.class );
        differentialExpressionAnalysisService = this.getBean( DifferentialExpressionAnalysisService.class );
        expressionDataFileService = this.getBean( ExpressionDataFileService.class );
        if ( this.hasOption( "type" ) ) {

            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "You can only specify the analysis type when analyzing a single experiment" );
            }

            if ( !this.hasOption( "factors" ) ) {
                throw new IllegalArgumentException( "Please specify the factor(s) when specifying the analysis type." );
            }
            this.type = AnalysisType.valueOf( this.getOptionValue( "type" ) );
        }

        if ( this.hasOption( "subset" ) ) {
            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "You can only specify the subset factor when analyzing a single experiment" );
            }

            if ( !this.hasOption( "factors" ) ) {
                throw new IllegalArgumentException( "You have to specify the factors if you also specify the subset" );
            }

            // note we add the given factor to the list of factors overall to make sure it is considered
            String subsetFactor = this.getOptionValue( "subset" );
            try {
                this.subsetFactorId = Long.parseLong( subsetFactor );
                this.factorIds.add( subsetFactorId );
            } catch ( NumberFormatException e ) {
                this.subsetFactorName = subsetFactor;
                this.factorNames.add( subsetFactor );
            }
        }

        if ( this.hasOption( "usebatch" ) ) {
            this.ignoreBatch = false;
        }

        if ( this.hasOption( "delete" ) ) {
            this.delete = true;
        }

        if ( this.hasOption( "ebayes" ) ) {
            this.ebayes = true;
        }

        if ( this.hasOption( "nodb" ) ) {
            this.persist = false;
        }

        this.tryToCopyOld = this.hasOption( "redo" );

        if ( this.hasOption( "factors" ) ) {

            if ( this.tryToCopyOld ) {
                throw new IllegalArgumentException( "You can't specify 'redo' and 'factors' together" );
            }

            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "You can only specify the factors when analyzing a single experiment" );
            }

            String rawFactors = this.getOptionValue( "factors" );
            String[] factorIDst = StringUtils.split( rawFactors, "," );
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

    private void processExperiment( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> results;
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        try {

            ee = this.eeService.thawLite( ee );

            if ( delete ) {
                AbstractCLI.log.info( "Deleting any analyses for experiment=" + ee );
                differentialExpressionAnalyzerService.deleteAnalyses( ee );
                addSuccessObject( ee, "Deleted analysis" );
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
                AbstractCLI.log
                        .warn( "Experiment does not have an experimental design populated: " + ee.getShortName() );
                return;
            }

            Collection<ExperimentalFactor> factors = this.guessFactors( ee );

            if ( factors.size() > 0 ) {
                /*
                 * Manual selection of factors (possibly including a subset factor)
                 */
                ExperimentalFactor subsetFactor = this.getSubsetFactor( ee );

                AbstractCLI.log.info( "Using " + factors.size() + " factors provided as arguments" );

                if ( subsetFactor != null ) {
                    //                    if ( factors.contains( subsetFactor ) ) {
                    //                        throw new IllegalArgumentException(
                    //                                "Subset factor cannot also be included as factor to analyze" );
                    //                    }
                    factors.remove( subsetFactor );
                    if ( factors.size() == 0 ) {
                        throw new IllegalArgumentException( "You must specify at least one other factor when using 'subset'" );
                    }
                    AbstractCLI.log.info( "Subsetting by " + subsetFactor );

                }

                config.setAnalysisType( this.type );
                config.setFactorsToInclude( factors );
                config.setSubsetFactor( subsetFactor );
                config.setModerateStatistics( this.ebayes );
                config.setPersist( this.persist );
                boolean rnaSeq = super.eeService.isRNASeq( ee );
                config.setUseWeights( rnaSeq );
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
                    this.tryToRedoBasedOnOldAnalysis( ee );
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
                    results = this.tryToRedoBasedOnOldAnalysis( ee );

                } else {

                    config.setFactorsToInclude( factorsToUse );
                    config.setPersist( this.persist );
                    config.setModerateStatistics( this.ebayes );

                    if ( factorsToUse.size() == 2 ) {
                        // include interactions by default
                        config.addInteractionToInclude( factorsToUse );
                    }

                    boolean rnaSeq = super.eeService.isRNASeq( ee );
                    config.setUseWeights( rnaSeq );

                    results = this.differentialExpressionAnalyzerService
                            .runDifferentialExpressionAnalyses( ee, config );
                }

            }

            if ( results == null ) {
                throw new Exception( "Failed to process differential expression for experiment " + ee.getShortName() );
            }

            if ( !this.persist ) {
                AbstractCLI.log.info( "Writing results to disk" );
                for ( DifferentialExpressionAnalysis r : results ) {
                    expressionDataFileService.writeDiffExArchiveFile( ee, r, config );
                }
            }

            addSuccessObject( ee, "Successfully processed " + ee.getShortName() );

        } catch ( Exception e ) {
            ExceptionUtils.printRootCauseStackTrace( e );
            addErrorObject( ee, e.getMessage(), e );
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
                    AbstractCLI.log.info( "Ignoring batch factor:" + experimentalFactor );
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
                    AbstractCLI.log.info( "Ignoring batch factor:" + experimentalFactor );
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
                factor = efs.thaw( factor );
                if ( factor == null ) {
                    throw new IllegalArgumentException( "No factor for id=" + factorId );
                }
                if ( !factor.getExperimentalDesign().equals( ee.getExperimentalDesign() ) ) {
                    throw new IllegalArgumentException( "Factor with id=" + factorId + " does not belong to " + ee );
                }

                if ( ignoreBatch && BatchInfoPopulationServiceImpl.isBatchFactor( factor ) ) {
                    AbstractCLI.log.warn( "Selected factor looks like a batch, and 'ignoreBatch' is true, skipping:"
                            + factor );
                    continue;
                }

                factors.add( factor );
            }
        }

        return factors;
    }

    /**
     * Run the analysis using configuration based on an old analysis.
     */
    private Collection<DifferentialExpressionAnalysis> tryToRedoBasedOnOldAnalysis( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> oldAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        if ( oldAnalyses.isEmpty() ) {
            throw new IllegalArgumentException( "There are no old analyses to redo" );
        }

        AbstractCLI.log.info( "Will attempt to redo " + oldAnalyses.size() + " analyses for " + ee );
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( DifferentialExpressionAnalysis copyMe : oldAnalyses ) {
            results.addAll( this.differentialExpressionAnalyzerService.redoAnalysis( ee, copyMe, this.persist ) );
        }
        return results;

    }

}
