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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import ubic.gemma.core.analysis.expression.diff.AnalysisType;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 *
 * @author keshav
 */
public class DifferentialExpressionAnalysisCli extends ExpressionExperimentManipulatingCLI implements MessageSourceAware {

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    // cannot be autowired because we use it for generating options
    private MessageSource messageSource;

    public void setMessageSource( MessageSource messageSource ) {
        this.messageSource = messageSource;
    }

    /**
     * Indicate the type of analysis to perform.
     * <p>
     * The default is to detect it based on the dataset and the requested factors.
     */
    private AnalysisType type = null;

    /**
     * Identifiers of factors to include in the linear model.
     */
    private final List<String> factorIdentifiers = new ArrayList<>();

    /**
     * Identifier of the factor to use to create subset analyses.
     */
    private String subsetFactorIdentifier;

    /**
     * Whether batch factors should be included (if they exist)
     */
    private boolean ignoreBatch = true;
    private boolean delete = false;
    private boolean tryToCopyOld = false;

    /**
     * Use moderated statistics.
     */
    private boolean ebayes = DifferentialExpressionAnalysisConfig.DEFAULT_EBAYES;

    private boolean persist = true;
    private boolean makeArchiveFiles = true;
    private boolean ignoreFailingSubsets = false;

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

        options.addOption( "factors", "factors", true, "ID numbers, categories or names of the factor(s) to use, comma-delimited, with spaces replaced by underscores" );

        options.addOption( "subset", "subset", true,
                "ID number, category or name of the factor to use for subsetting the analysis; must also use with -factors. "
                        + "If the experiment already has subsets for the factor, those will be reused." );

        options.addOption( "type", "type", true,
                "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design. "
                        + "Choices are : "
                        + Arrays.stream( AnalysisType.values() )
                        .map( e -> messageSource.getMessage( "AnalysisType." + e.name() + ".shortDesc", new Object[] { e }, e.name(), Locale.getDefault() ) )
                        .collect( Collectors.joining( ", " ) )
                        + "; default: auto-detect" );

        options.addOption( "usebatch", "use-batch-factor", false, "If a 'batch' factor is available, use it. Otherwise, batch information can/will be ignored in the analysis." );

        options.addOption( "nodb", "no-db", false, "Output files only to your gemma.appdata.home (unless you also set -nofiles) instead of persisting to the database" );

        options.addOption( "ignoreFailingSubsets", "ignore-failing-subsets", false, "Ignore failing subsets and continue processing other subsets." );

        options.addOption( "redo", "redo", false,
                "If using automatic analysis try to base analysis on previous analysis's choice of statistical model. "
                        + "Will re-run all analyses for the experiment" );

        options.addOption( "delete", "delete", false, "Instead of running the analysis on the given experiments, remove the old analyses. Use with care!" );

        options.addOption( "nobayes", "no-bayes", false, "Do not apply empirical-Bayes moderated statistics. Default is to use eBayes." );

        options.addOption( "nofiles", "no-files", false, "Don't create archive files after analysis. Default is to make them." );

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
            this.subsetFactorIdentifier = commandLine.getOptionValue( "subset" );
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

        if ( commandLine.hasOption( "ignoreFailingSubsets" ) ) {
            this.ignoreFailingSubsets = true;
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
                this.factorIdentifiers.addAll( Arrays.asList( factorIDst ) );
            }
        }
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        if ( type != null ) {
            throw new IllegalArgumentException( "You can only specify the analysis type when analyzing a single experiment" );
        }
        if ( subsetFactorIdentifier != null ) {
            throw new IllegalArgumentException( "You can only specify the subset factor when analyzing a single experiment" );
        }
        if ( !factorIdentifiers.isEmpty() ) {
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
        config.setIgnoreFailingSubsets( this.ignoreFailingSubsets );
        config.setUseWeights( super.eeService.isRNASeq( ee ) );

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

        Map<Long, ExperimentalFactor> factorsById = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .collect( Collectors.toMap( ExperimentalFactor::getId, ef -> ef ) );

        Map<String, Set<ExperimentalFactor>> factorsByName = new HashMap<>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            factorsByName
                    .computeIfAbsent( ef.getName().replace( " ", "_" ), k -> new HashSet<>() )
                    .add( ef );
            if ( ef.getCategory() != null && ef.getCategory().getCategory() != null ) {
                factorsByName
                        .computeIfAbsent( ef.getCategory().getCategory().replace( " ", "_" ), k -> new HashSet<>() )
                        .add( ef );
            }
        }

        Collection<ExperimentalFactor> factors = this.guessFactors( factorsById, factorsByName );

        if ( !factors.isEmpty() ) {
            /*
             * Manual selection of factors (possibly including a subset factor)
             */
            ExperimentalFactor subsetFactor = this.getSubsetFactor( factorsById, factorsByName );

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
            config.addFactorsToInclude( factors );
            config.setSubsetFactor( subsetFactor );


            /*
             * Interactions included by default. It's actually only complicated if there is a subset factor.
             */
            if ( type == null && factors.size() == 2 ) {
                config.addInteractionToInclude( factors );
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
                    if ( !ExperimentalDesignUtils.isBatchFactor( ef ) ) {
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

                config.addFactorsToInclude( factorsToUse );

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
                try ( LockedPath lockedPath = expressionDataFileService.writeDiffExAnalysisArchiveFile( r, config ) ) {
                    log.info( "Wrote to " + lockedPath.getPath() );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }
        }
    }

    /**
     * Obtain the subset factor if given from the command line.
     */
    @Nullable
    private ExperimentalFactor getSubsetFactor( Map<Long, ExperimentalFactor> factorById, Map<String, Set<ExperimentalFactor>> factorByName ) {
        if ( this.subsetFactorIdentifier != null ) {
            ExperimentalFactor factor = locateExperimentalFactor( this.subsetFactorIdentifier, factorById, factorByName );
            if ( ignoreBatch && ExperimentalDesignUtils.isBatchFactor( factor ) ) {
                throw new IllegalArgumentException( "Subset factor appears to be a batch, pass -usebatch to use." );
            }
            return factor;
        }
        return null;
    }

    /**
     * Determine which factors to use if given from the command line. Only applicable if analysis is on a single data
     * set.
     */
    private Collection<ExperimentalFactor> guessFactors( Map<Long, ExperimentalFactor> factorsById, Map<String, Set<ExperimentalFactor>> factorsByName ) {
        Collection<ExperimentalFactor> factors = new HashSet<>();

        for ( String factorId : factorIdentifiers ) {
            ExperimentalFactor factor = locateExperimentalFactor( factorId, factorsById, factorsByName );
            if ( ignoreBatch && ExperimentalDesignUtils.isBatchFactor( factor ) ) {
                log.warn( "Selected factor looks like a batch, skipping. Pass -usebatch to include."
                        + factor );
                continue;
            }
            if ( !factors.add( factor ) ) {
                log.warn( factor + " was already added by either a name or ID." );
            }
        }

        return factors;
    }

    /**
     * Locate a factor given a user-supplied identifier.
     */
    private ExperimentalFactor locateExperimentalFactor( String identifier, Map<Long, ExperimentalFactor> factorsById, Map<String, Set<ExperimentalFactor>> factorsByName ) {
        try {
            ExperimentalFactor factor = factorsById.get( Long.parseLong( identifier ) );
            if ( factor == null ) {
                throw new IllegalArgumentException( String.format( "No factor for ID %s. Possible values are:\n\t%s",
                        identifier, factorsById.entrySet().stream().map( e -> e.getKey() + ":\t" + e.getValue() ).collect( Collectors.joining( "\n\t" ) ) ) );
            }
            return factor;
        } catch ( NumberFormatException e ) {
            // ignore, will match by name
        }
        Set<ExperimentalFactor> matchingFactors = factorsByName.get( identifier );
        if ( matchingFactors == null ) {
            throw new IllegalArgumentException( String.format( "No factor for name %s. Possible values are:\n\t%s",
                    // only suggest unambiguous factors
                    identifier, factorsByName.entrySet().stream()
                            .filter( e -> e.getValue().size() == 1 )
                            .map( e -> e.getKey() + ":\t" + e.getValue().iterator().next() )
                            .collect( Collectors.joining( "\n\t" ) ) ) );
        }
        if ( matchingFactors.size() > 1 ) {
            throw new IllegalArgumentException( "More than one factor match " + identifier + ", use a numerical ID instead. Possible values are:\n\t"
                    + matchingFactors.stream().map( String::valueOf ).collect( Collectors.joining( "\n\t" ) ) );
        }
        return matchingFactors.iterator().next();
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
