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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;

import java.io.IOException;
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

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private ExperimentalFactorService efs;

    private final List<Long> factorIds = new ArrayList<>();
    private final List<String> factorNames = new ArrayList<>();
    /**
     * Whether batch factors should be included (if they exist)
     */
    private boolean ignoreBatch = true;
    private boolean delete = false;
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
    private boolean makeArchiveFiles = true;

    @Override
    public String getCommandName() {
        return "diffExAnalyze";
    }

    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for differentially expressed genes.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        /* Supports: running on all data sets that have not been run since a given date. */
        addLimitingDateOption( options );

        addAutoOption( options, DifferentialExpressionAnalysisEvent.class );
        addForceOption( options );

        Option factors = Option.builder( "factors" ).hasArg().desc(
                        "ID numbers, categories or names of the factor(s) to use, comma-delimited, with spaces replaced by underscores" )
                .build();

        options.addOption( factors );

        Option subsetFactor = Option.builder( "subset" ).hasArg().desc(
                        "ID number, category or name of the factor to use for subsetting the analysis; must also use with -factors" )
                .build();
        options.addOption( subsetFactor );

        Option analysisType = Option.builder( "type" ).hasArg().desc(
                        "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design. "
                                + "Choices are : TWO_WAY_ANOVA_WITH_INTERACTION, "
                                + "TWO_WAY_ANOVA_NO_INTERACTION , OWA (one-way ANOVA), TTEST, OSTTEST (one-sample t-test),"
                                + " GENERICLM (generic LM, no interactions); default: auto-detect" )
                .build();

        options.addOption( analysisType );

        Option ignoreBatchOption = Option.builder( "usebatch" ).desc(
                        "If a 'batch' factor is available, use it. Otherwise, batch information can/will be ignored in the analysis." )
                .build();

        options.addOption( ignoreBatchOption );

        options.addOption( "nodb", "Output files only to your gemma.appdata.home (unless you also set -nofiles) instead of persisting to the database" );

        options.addOption( "redo", "If using automatic analysis "
                + "try to base analysis on previous analysis's choice of statistical model. Will re-run all analyses for the experiment" );

        options.addOption( "delete", "Instead of running the analysis on the given experiments, remove the old analyses. Use with care!" );

        options.addOption( "nobayes", "Do not apply empirical-Bayes moderated statistics. Default is to use eBayes" );

        options.addOption( "nofiles", "Don't create archive files after analysis. Default is to make them" );

    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "type" ) ) {
            if ( !commandLine.hasOption( "factors" ) ) {
                throw new IllegalArgumentException( "Please specify the factor(s) when specifying the analysis type." );
            }
            this.type = AnalysisType.valueOf( commandLine.getOptionValue( "type" ) );
        }

        if ( commandLine.hasOption( "subset" ) ) {
            if ( !commandLine.hasOption( "factors" ) ) {
                throw new IllegalArgumentException( "You have to specify the factors if you also specify the subset" );
            }

            // note we add the given factor to the list of factors overall to make sure it is considered
            String subsetFactor = commandLine.getOptionValue( "subset" );
            try {
                this.subsetFactorId = Long.parseLong( subsetFactor );
                this.factorIds.add( subsetFactorId );
            } catch ( NumberFormatException e ) {
                this.subsetFactorName = subsetFactor;
                this.factorNames.add( subsetFactor );
            }
        }

        if ( commandLine.hasOption( "usebatch" ) ) {
            this.ignoreBatch = false;
        }

        if ( commandLine.hasOption( "delete" ) ) {
            this.delete = true;
        }

        if ( commandLine.hasOption( "nobayes" ) ) {
            this.ebayes = false;
        }

        if ( commandLine.hasOption( "nodb" ) ) {
            this.persist = false;
        }

        if ( commandLine.hasOption( "nofiles" ) ) {
            this.makeArchiveFiles = false;
        }

        this.tryToCopyOld = commandLine.hasOption( "redo" );

        if ( commandLine.hasOption( "factors" ) ) {

            if ( this.tryToCopyOld ) {
                throw new IllegalArgumentException( "You can't specify 'redo' and 'factors' together" );
            }

            String rawFactors = commandLine.getOptionValue( "factors" );
            String[] factorIDst = StringUtils.split( rawFactors, "," );
            if ( factorIDst != null ) {
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

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        if ( type != null ) {
            throw new IllegalArgumentException( "You can only specify the analysis type when analyzing a single experiment" );
        }
        if ( subsetFactorId != null ) {
            throw new IllegalArgumentException( "You can only specify the subset factor when analyzing a single experiment" );
        }
        if ( !factorIds.isEmpty() ) {
            throw new IllegalArgumentException( "You can only specify the factors when analyzing a single experiment" );
        }
        super.processBioAssaySets( expressionExperiments );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> results;
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setMakeArchiveFile( this.makeArchiveFiles );
        config.setModerateStatistics( this.ebayes );
        config.setPersist( this.persist );
        boolean rnaSeq = super.eeService.isRNASeq( ee );
        config.setUseWeights( rnaSeq );

        ee = this.eeService.thawLite( ee );

        if ( delete ) {
            log.info( "Deleting any analyses for experiment=" + ee );
            differentialExpressionAnalyzerService.deleteAnalyses( ee );
            addSuccessObject( ee, "Deleted analysis" );
            return;
        }

        if ( ee.getExperimentalDesign() == null ) {
            throw new IllegalStateException( ee + " does not have an experimental design." );
        }
        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        if ( experimentalFactors.isEmpty() ) {
            /*
             * Only need to be noisy if this is the only ee. Batch processing should be less so.
             */
            throw new RuntimeException(
                    "Experiment does not have an experimental design populated: " + ee.getShortName() );
        }

        Collection<ExperimentalFactor> factors = this.guessFactors( ee );

        if ( !factors.isEmpty() ) {
            /*
             * Manual selection of factors (possibly including a subset factor)
             */
            ExperimentalFactor subsetFactor = this.getSubsetFactor( ee );

            log.info( "Using " + factors.size() + " factors provided as arguments" );

            if ( subsetFactor != null ) {
                //                    if ( factors.contains( subsetFactor ) ) {
                //                        throw new IllegalArgumentException(
                //                                "Subset factor cannot also be included as factor to analyze" );
                //                    }
                factors.remove( subsetFactor );
                if ( factors.isEmpty() ) {
                    throw new IllegalArgumentException( "You must specify at least one other factor when using 'subset'" );
                }
                log.info( "Subsetting by " + subsetFactor );

            }

            config.setAnalysisType( this.type );
            config.setFactorsToInclude( factors );
            config.setSubsetFactor( subsetFactor );


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

                if ( factorsToUse.size() == 2 ) {
                    // include interactions by default
                    config.addInteractionToInclude( factorsToUse );
                }

                results = this.differentialExpressionAnalyzerService
                        .runDifferentialExpressionAnalyses( ee, config );
            }

        }

        if ( results == null ) {
            throw new RuntimeException( "Failed to process differential expression for experiment " + ee.getShortName() );
        }

        if ( this.persist ) {
            try {
                refreshExpressionExperimentFromGemmaWeb( ee, false, true );
            } catch ( Exception e ) {
                log.error( "Failed to refresh " + ee + " from Gemma Web.", e );
            }
        } else {
            log.info( "Writing results to disk" );
            for ( DifferentialExpressionAnalysis r : results ) {
                try ( ExpressionDataFileService.LockedPath lockedPath = expressionDataFileService.writeDiffExAnalysisArchiveFile( r, config ) ) {
                    log.info( "Wrote to " + lockedPath.getPath() );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
    }

    private ExperimentalFactor getSubsetFactor( ExpressionExperiment ee ) {
        Assert.notNull( ee.getExperimentalDesign() );
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
     */
    private Collection<ExperimentalFactor> guessFactors( ExpressionExperiment ee ) {
        Assert.notNull( ee.getExperimentalDesign() );
        Collection<ExperimentalFactor> factors = new HashSet<>();

        if ( !this.factorNames.isEmpty() ) {
            if ( !this.factorIds.isEmpty() ) {
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

        } else if ( !this.factorIds.isEmpty() ) {
            for ( Long factorId : factorIds ) {
                if ( !this.factorNames.isEmpty() ) {
                    throw new IllegalArgumentException( "Please provide factor names or ids, not a mixture of each" );
                }
                ExperimentalFactor factor = efs.loadOrFail( factorId );
                factor = efs.thaw( factor );
                if ( factor == null ) {
                    throw new IllegalArgumentException( "No factor for id=" + factorId );
                }
                if ( !factor.getExperimentalDesign().equals( ee.getExperimentalDesign() ) ) {
                    throw new IllegalArgumentException( "Factor with id=" + factorId + " does not belong to " + ee );
                }

                if ( ignoreBatch && BatchInfoPopulationServiceImpl.isBatchFactor( factor ) ) {
                    log.warn( "Selected factor looks like a batch, and 'ignoreBatch' is true, skipping:"
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
                .findByExperiment( ee );

        if ( oldAnalyses.isEmpty() ) {
            throw new IllegalArgumentException( "There are no old analyses to redo" );
        }

        log.info( "Will attempt to redo " + oldAnalyses.size() + " analyses for " + ee );
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( DifferentialExpressionAnalysis copyMe : oldAnalyses ) {
            results.addAll( this.differentialExpressionAnalyzerService.redoAnalysis( ee, copyMe, this.persist ) );
        }
        return results;

    }

}
