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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import ubic.gemma.cli.completion.CompletionSource;
import ubic.gemma.cli.completion.CompletionUtils;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.analysis.expression.diff.AnalysisType;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisFilter;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.cli.util.OptionsUtils.*;

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

    /**
     * Mode of operation of the CLI.
     */
    private Mode mode;

    private enum Mode {
        RUN,
        DELETE,
        DELETE_ANALYSES,
        COMPLETE_ANALYSES,
        COMPLETE_SUBSETS,
        COMPLETE_SUBSET_FACTORS,
        COMPLETE_FACTORS
    }

    /**
     * Specific analyses to redo.
     */
    @Nullable
    private Collection<Long> analysisIds = null;

    @Nullable
    private Collection<Long> subsetIds = null;

    /**
     * Indicate the type of analysis to perform.
     * <p>
     * The default is to detect it based on the dataset and the requested factors.
     */
    private AnalysisType type = null;

    /**
     * Mode for selecting factors.
     */
    private FactorSelectionMode factorSelectionMode;

    /**
     * Identifiers of factors to include in the linear model.
     */
    @Nullable
    private List<String> factorIdentifiers;

    /**
     * Identifier of the factor to use to create subset analyses.
     */
    @Nullable
    private String subsetFactorIdentifier;

    /**
     * Whether batch factors should be included (if they exist)
     */
    private boolean ignoreBatch = true;

    /**
     * Use moderated statistics.
     */
    private boolean moderateStatistics = DifferentialExpressionAnalysisConfig.DEFAULT_MODERATE_STATISTICS;

    /**
     * Persist results to the database.
     */
    private boolean persist = true;

    private boolean makeArchiveFiles = true;

    private boolean ignoreFailingSubsets = false;

    @Nullable
    private Integer filterMinNumberOfCells;
    private DifferentialExpressionAnalysisFilter.RepetitiveValuesFilterMode filterMode;
    @Nullable
    private Integer filterMinSamples;
    @Nullable
    private Double filterMinUniqueValues;
    @Nullable
    private Double filterMinVariance;

    private ExpressionDataFileResult result;

    enum FactorSelectionMode {
        REDO,
        AUTOMATIC,
        MANUAL
    }

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

        addSingleExperimentEnumOption( options, "type", "type",
                "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design.",
                AnalysisType.class,
                getAnalysisTypeDescriptions() );

        addSingleExperimentOption( options, Option.builder( "factors" ).longOpt( "factors" )
                .hasArgs()
                .argName( "ID, name, category" )
                .valueSeparator( ',' )
                .desc( "ID numbers, categories or names of the factor(s) to use, comma-delimited, with spaces ' ' and colons ':' replaced by underscores '_'. "
                        + "Interaction can be specified by using ':' as a delimiter (e.g. 'factor1:factor2'). "
                        + "Only categorical factors can be used in interactions. "
                        + "Multiple factors can be provided using comma-delimited IDs or by passing the option multiple times. "
                        + "If omitted, factors will be selected automatically. "
                        + "This is incompatible with -redo,--redo, -redoAnalysis,--redo-analysis or -redoSubset,--redo-subset." )
                .get() );

        addSingleExperimentOption( options, Option.builder( "subset" )
                .longOpt( "subset" )
                .hasArg()
                .argName( "ID, name, category" )
                .desc( "ID number, category or name of the factor to use for subsetting the analysis. "
                        + "The factor must be categorical. "
                        + "If used without specifying  " + formatOption( options, "factors" ) + ", factors will be selected automatically among the remaining one in the design. "
                        + "If the experiment already has subsets for the factor, those will be reused. "
                        + "This is incompatible with -redo,--redo, -redoAnalysis,--redo-analysis or -redoSubset,--redo-subset." ).get() );

        options.addOption( "usebatch", "use-batch-factor", false, "If a batch factor is available, use it. Otherwise, batch information can/will be ignored in the analysis. This is incompatible with " + formatOption( options, "factors" ) + ", -redo,--redo, -redoAnalysis,--redo-analysis and -redoSubset,--redo-subset." );
        options.addOption( "nobayes", "no-bayes", false, "Do not apply empirical-Bayes moderated statistics. Default is to use eBayes." );
        options.addOption( "ignoreFailingSubsets", "ignore-failing-subsets", false, "Ignore failing subsets and continue processing other subsets. Requires the " + formatOption( options, "subset" ) + " option to be set or -redo,--redo option with existing subset analyses." );

        addExpressionDataFileOptions( options, "diff. ex. archive files", false, false, true, true, false );

        // destination (db, standard location or custom directory)
        options.addOption( "nodb", "no-db", false, "Do not persist diff. ex. results to the database and instead save them to the current directory (or the location defined by " + formatOption( options, OUTPUT_DIR_OPTION ) + ")." );
        options.addOption( "nofiles", "no-files", false, "Don't create archive files after analysis. Default is to make them. This is incompatible with " + formatOption( options, "nodb" ) + "." );

        // redo mode
        options.addOption( "redo", "redo", false,
                "Re-run all analyses for the experiment. "
                        + "Try to base analysis on previous analysis's choice of statistical model." );
        addSingleExperimentOption( options, Option.builder( "redoAnalysis" )
                .longOpt( "redo-analysis" )
                .hasArgs()
                .argName( "ID" )
                .valueSeparator( ',' )
                .desc( "Re-run a specific analysis for the experiment. "
                        + "Try to base analysis on previous analysis's choice of statistical model. "
                        + "Multiple analysis can be provided using comma-delimited IDs or by passing the option multiple times." )
                .get() );
        addSingleExperimentOption( options, Option.builder( "redoSubset" ).longOpt( "redo-subset" )
                .hasArg().argName( "ID" ).valueSeparator( ',' )
                .desc( "Re-run all analyses for a subset of the experiment. "
                        + "Try to base analysis on previous analysis's choice of statistical model. "
                        + "Multiple subsets can be provided using comma-delimited IDs or by passing the option multiple times." )
                .get() );

        // filter options
        options.addOption( Option.builder( "filterMinNumberOfCells" )
                .longOpt( "filter-minimum-number-of-cells" )
                .hasArg( true )
                .type( Integer.class )
                .desc( "Minimum number of cells required for a sample to be included in the analysis. Defaults to " + DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_NUMBER_OF_CELLS + "." )
                .get() );
        addEnumOption( options, "filterRepetitiveValuesMode", "filter-repetitive-values-mode", "Mode to use for filtering repetitive values. Default is to auto-detect based on the quantitation type.", DifferentialExpressionAnalysisFilter.RepetitiveValuesFilterMode.class );
        options.addOption( Option.builder( "filterRepetitiveValuesMinSamples" )
                .longOpt( "filter-repetitive-values-minimum-number-of-samples" )
                .hasArg( true )
                .type( Integer.class )
                .desc( "Minimum number of samples to apply the repetitive values filter. Defaults to " + DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_NUMBER_OF_SAMPLES_TO_APPLY_REPETITIVE_VALUES_FILTER + "." )
                .get() );
        options.addOption( Option.builder( "filterRepetitiveValuesMinUniqueValues" )
                .longOpt( "filter-repetitive-values-minimum-unique-values" )
                .hasArg( true )
                .type( Double.class )
                .desc( "Minimum fraction of unique values to retain a design element when filtering repetitive values. Defaults to " + DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_FRACTION_OF_UNIQUE_VALUES + "." )
                .get() );
        options.addOption( Option.builder( "filterMinVariance" )
                .longOpt( "filter-minimum-variance" )
                .hasArg( true )
                .type( Double.class )
                .desc( "Minimum variance required for a design element to be included in the analysis. Defaults to " + DifferentialExpressionAnalysisFilter.DEFAULT_MINIMUM_VARIANCE + "." )
                .get() );

        // delete mode
        options.addOption( "delete", "delete", false, "Remove all the existing analyses for the specified experiment(s). Use with care!" );
        addSingleExperimentOption( options, Option.builder( "deleteAnalysis" )
                .longOpt( "delete-analysis" )
                .hasArgs()
                .argName( "ID" )
                .valueSeparator( ',' )
                .desc( "Remove the specified analysis for the specified experiment. "
                        + "Multiple analyses can be provided using comma-delimited IDs or by passing the option multiple times. "
                        + "Use with care!" )
                .get() );
        addSingleExperimentOption( options, Option.builder( "deleteSubset" )
                .longOpt( "delete-subset" )
                .hasArgs()
                .argName( "ID" )
                .valueSeparator( ',' )
                .desc( "Remove all analyses for the specified subset of the experiment. "
                        + "Multiple subsets can be provided using comma-delimited IDs or by passing the option multiple times. "
                        + "Use with care!" )
                .get() );

        // complete modes
        addSingleExperimentOption( options, Option.builder( "completeAnalyses" )
                .longOpt( "complete-analyses" )
                .desc( "Provide completions for existing analyses." ).get() );
        addSingleExperimentOption( options, Option.builder( "completeSubsets" )
                .longOpt( "complete-subsets" )
                .desc( "Provide completions for existing subsets with analysis." ).get() );
        addSingleExperimentOption( options, Option.builder( "completeSubsetFactors" )
                .longOpt( "complete-subset-factors" )
                .desc( "Provide completions for subset factors." ).get() );
        addSingleExperimentOption( options, Option.builder( "completeFactors" ).longOpt( "complete-factors" )
                .desc( "Provide completions for factors and interactions. If a subset factor is set via " + formatOption( options, "subset" ) + ", it will not be suggested." ).get() );
    }

    private EnumMap<AnalysisType, MessageSourceResolvable> getAnalysisTypeDescriptions() {
        EnumMap<AnalysisType, MessageSourceResolvable> result = new EnumMap<>( AnalysisType.class );
        for ( AnalysisType e : AnalysisType.values() ) {
            result.put( e, new DefaultMessageSourceResolvable( new String[] { "AnalysisType." + e.name() + ".shortDesc" }, null, "" ) );
        }
        return result;
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "delete" ) ) {
            mode = Mode.DELETE;
        } else if ( commandLine.hasOption( "deleteAnalysis" ) ) {
            mode = Mode.DELETE_ANALYSES;
            this.analysisIds = Arrays.stream( commandLine.getOptionValues( "deleteAnalysis" ) )
                    .map( Long::parseLong )
                    .collect( Collectors.toSet() );
        } else if ( commandLine.hasOption( "deleteSubset" ) ) {
            mode = Mode.DELETE_ANALYSES;
            this.subsetIds = Arrays.stream( commandLine.getOptionValues( "deleteSubset" ) )
                    .map( Long::parseLong )
                    .collect( Collectors.toSet() );
        } else if ( commandLine.hasOption( "completeAnalyses" ) ) {
            mode = Mode.COMPLETE_ANALYSES;
        } else if ( commandLine.hasOption( "completeSubsets" ) ) {
            mode = Mode.COMPLETE_SUBSETS;
        } else if ( commandLine.hasOption( "completeSubsetFactors" ) ) {
            mode = Mode.COMPLETE_SUBSET_FACTORS;
        } else if ( commandLine.hasOption( "completeFactors" ) ) {
            mode = Mode.COMPLETE_FACTORS;
        } else {
            mode = Mode.RUN;
        }
        this.type = OptionsUtils.getEnumOptionValue( commandLine, "type" );
        if ( commandLine.hasOption( "redo" ) ) {
            this.factorSelectionMode = FactorSelectionMode.REDO;
        } else if ( commandLine.hasOption( "redoAnalysis" ) ) {
            this.factorSelectionMode = FactorSelectionMode.REDO;
            this.analysisIds = Arrays.stream( commandLine.getOptionValues( "redoAnalysis" ) ).map( Long::parseLong ).collect( Collectors.toSet() );
        } else if ( commandLine.hasOption( "redoSubset" ) ) {
            this.factorSelectionMode = FactorSelectionMode.REDO;
            this.subsetIds = Arrays.stream( commandLine.getOptionValues( "redoSubset" ) ).map( Long::parseLong ).collect( Collectors.toSet() );
        } else if ( commandLine.hasOption( "factors" ) ) {
            this.factorSelectionMode = FactorSelectionMode.MANUAL;
            this.factorIdentifiers = Arrays.asList( commandLine.getOptionValues( "factors" ) );
        } else {
            this.factorSelectionMode = FactorSelectionMode.AUTOMATIC;
        }
        // subset analysis can only be done in manual mode
        // note we add the given factor to the list of factors overall to make sure it is considered
        this.subsetFactorIdentifier = getOptionValue( commandLine, "subset", requires( allOf( toBeUnset( "redo" ), toBeUnset( "redoAnalysis" ), toBeUnset( "redoSubset" ) ) ) );
        // we can only force the use of a batch factor during automatic selection
        this.ignoreBatch = !hasOption( commandLine, "usebatch", requires( allOf( toBeUnset( "factors" ), toBeUnset( "redo" ), toBeUnset( "redoAnalysis" ), toBeUnset( "redoSubset" ) ) ) );
        this.moderateStatistics = !commandLine.hasOption( "nobayes" );
        this.persist = !commandLine.hasOption( "nodb" );
        this.makeArchiveFiles = !hasOption( commandLine, "nofiles", requires( toBeUnset( "nodb" ) ) );
        this.result = getExpressionDataFileResult( commandLine, false, false, true );
        this.filterMinNumberOfCells = commandLine.getParsedOptionValue( "filterMinNumberOfCells" );
        this.filterMode = getEnumOptionValue( commandLine, "filterRepetitiveValuesMode" );
        this.filterMinSamples = commandLine.getParsedOptionValue( "filterRepetitiveValuesMinSamples" );
        this.filterMinUniqueValues = commandLine.getParsedOptionValue( "filterRepetitiveValuesMinUniqueValues" );
        this.filterMinVariance = commandLine.getParsedOptionValue( "filterMinVariance" );
        // TODO: check if the analysis being redone is a subset analysis
        this.ignoreFailingSubsets = hasOption( commandLine, "ignoreFailingSubsets",
                requires( anyOf( toBeSet( "subset" ), toBeSet( "redo" ) ) ) );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ee = this.eeService.thawLite( ee );

        if ( mode == Mode.DELETE ) {
            log.info( "Deleting any analyses for " + ee + "..." );
            int da = differentialExpressionAnalyzerService.deleteAnalyses( ee );
            addSuccessObject( ee, "Deleted " + da + " analyses." );
            refreshDeaFromGemmaWeb( ee );
            return;
        } else if ( mode == Mode.DELETE_ANALYSES ) {
            log.info( "Deleting selected analyses for " + ee + "..." );
            Collection<DifferentialExpressionAnalysis> toDelete = getAnalyses( ee );
            if ( !toDelete.isEmpty() ) {
                int da = differentialExpressionAnalyzerService.deleteAnalyses( ee, toDelete );
                addSuccessObject( ee, "Deleted " + da + " analyses." );
                refreshDeaFromGemmaWeb( ee );
            } else {
                addWarningObject( ee, "No analysis found to be deleted." );
            }
            return;
        }

        if ( ee.getExperimentalDesign() == null || ee.getExperimentalDesign().getExperimentalFactors().isEmpty() ) {
            throw new IllegalStateException( ee + " does not have an experimental design populated." );
        }

        Map<Long, ExperimentalFactor> factorsById = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .collect( Collectors.toMap( ExperimentalFactor::getId, ef -> ef ) );
        Map<String, Set<ExperimentalFactor>> factorsByName = new HashMap<>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            factorsByName
                    .computeIfAbsent( sanitizeFactorName( ef.getName() ), k -> new HashSet<>() )
                    .add( ef );
            if ( ef.getCategory() != null && ef.getCategory().getCategory() != null ) {
                factorsByName
                        .computeIfAbsent( sanitizeFactorName( ef.getCategory().getCategory() ), k -> new HashSet<>() )
                        .add( ef );
            }
        }

        if ( mode == Mode.COMPLETE_ANALYSES ) {
            CompletionUtils.writeCompletions( new DiffExAnalysisCompletionSource( ee, false ), getCliContext().getOutputStream() );
            return;
        }

        if ( mode == Mode.COMPLETE_SUBSETS ) {
            CompletionUtils.writeCompletions( new DiffExAnalysisCompletionSource( ee, true ), getCliContext().getOutputStream() );
            return;
        }

        if ( mode == Mode.COMPLETE_SUBSET_FACTORS ) {
            CompletionUtils.writeCompletions( new SubSetFactorCompletionSource( ee.getExperimentalDesign() ),
                    getCliContext().getOutputStream() );
            return;
        }

        if ( mode == Mode.COMPLETE_FACTORS ) {
            CompletionUtils.writeCompletions( new FactorsAndInteractionsCompletionSource( ee.getExperimentalDesign(),
                    getSubsetFactor( factorsById, factorsByName ) ), getCliContext().getOutputStream() );
            return;
        }

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setAnalysisType( this.type );
        config.setModerateStatistics( this.moderateStatistics );
        config.setPersist( this.persist );
        config.setMakeArchiveFile( this.persist && this.makeArchiveFiles );
        config.setIgnoreFailingSubsets( this.ignoreFailingSubsets );
        config.setUseWeights( super.eeService.isRNASeq( ee ) );

        // filtering
        config.setRepetitiveValuesFilterMode( this.filterMode );
        config.setMinimumNumberOfSamplesToApplyRepetitiveValuesFilter( this.filterMinSamples );
        config.setMinimumFractionOfUniqueValues( this.filterMinUniqueValues );
        config.setMinimumNumberOfCells( this.filterMinNumberOfCells );
        config.setMinimumVariance( this.filterMinVariance );

        if ( factorSelectionMode == FactorSelectionMode.REDO ) {
            // selection of factors will be based on the existing analysis
            log.info( "Factors will be selected based on the existing analysis." );
        } else if ( factorSelectionMode == FactorSelectionMode.AUTOMATIC ) {
            log.info( "Factors will be selected automatically." );
            // automatic selection of factors
            Collection<ExperimentalFactor> factorsToUse = new HashSet<>( ee.getExperimentalDesign().getExperimentalFactors() );

            if ( subsetFactorIdentifier != null ) {
                ExperimentalFactor subsetFactor = getSubsetFactor( factorsById, factorsByName );
                factorsToUse.remove( subsetFactor );
                config.setSubsetFactor( subsetFactor );
                log.info( "Subsetting by " + subsetFactor + "." );
            }

            if ( this.ignoreBatch ) {
                factorsToUse.removeIf( ExperimentFactorUtils::isBatchFactor );
            }

            if ( factorsToUse.isEmpty() ) {
                throw new RuntimeException( "No suitable factors to analyze found." );
            } else if ( factorsToUse.size() == 1 ) {
                config.addFactorsToInclude( factorsToUse );
            } else if ( factorsToUse.size() == 2 ) {
                config.addFactorsToInclude( factorsToUse );
                if ( ( config.getAnalysisType() == null || config.getAnalysisType() == AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION ) && factorsToUse.stream().allMatch( factor -> factor.getType().equals( FactorType.CATEGORICAL ) ) ) {
                    // include interactions by default
                    log.info( "Including interaction of two categorical factors automatically. To prevent this, set the analysis type to " + AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION + "." );
                    config.addInteractionToInclude( factorsToUse );
                }
            } else {
                throw new RuntimeException( "Experiment has too many factors (" + factorsToUse.size() + ") to run automatically. Try using the -redo,--redo (or -redoAnalysis,--redo-analyses) flag to base it on an existing analysis, or select factors manually with -factors,--factors." );
            }
        } else {
            log.info( "Factors and interactions will be selected manually." );
            // Manual selection of factors (possibly including a subset factor)
            Collection<ExperimentalFactor> factors = this.getFactors( factorsById, factorsByName );
            Collection<Collection<ExperimentalFactor>> factorInteractions = this.getFactorInteractions( factorsById, factorsByName );

            // make sure that all the individual factors in interactions are also included as factors to analyze
            for ( Collection<ExperimentalFactor> interaction : factorInteractions ) {
                for ( ExperimentalFactor factor : interaction ) {
                    if ( factors.add( factor ) ) {
                        log.warn( "Added " + factor + " as a factor to analyze since it is used in an interaction. Include it explicitly with -factor,--factors to suppress this warning." );
                    }
                }
            }

            if ( config.getAnalysisType() == AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION && !factorInteractions.isEmpty() ) {
                throw new IllegalArgumentException( "Interactions cannot be specified requesting an analysis of type " + type + "." );
            }

            // make sure that the subset factor is not used as a factor to analyze (or as an interaction)
            ExperimentalFactor subsetFactor = this.getSubsetFactor( factorsById, factorsByName );
            if ( subsetFactor != null ) {
                if ( factors.contains( subsetFactor ) ) {
                    throw new IllegalArgumentException( "A subset factor cannot be included as a factor to analyze." );
                }
                for ( Collection<ExperimentalFactor> interaction : factorInteractions ) {
                    if ( interaction.contains( subsetFactor ) ) {
                        throw new IllegalArgumentException( "A subset factor cannot be included in an interaction." );
                    }
                }
                log.info( "Subsetting by " + subsetFactor + "." );
            }

            // if interactions are allowed and the user did not specify any, automatically include them
            if ( ( config.getAnalysisType() == null || config.getAnalysisType() == AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION )
                    && factors.size() == 2
                    && factors.stream().allMatch( ef -> ef.getType().equals( FactorType.CATEGORICAL ) )
                    && factorInteractions.isEmpty() ) {
                log.info( "Including interaction of two categorical factors automatically. To prevent this, set the analysis type to " + AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION + " or specify interactions in -factors,--factors using ':' as a delimiter." );
                factorInteractions = Collections.singleton( new HashSet<>( factors ) );
            }

            config.setSubsetFactor( subsetFactor );
            config.addFactorsToInclude( factors );
            config.addInteractionsToInclude( factorInteractions );
        }

        Collection<DifferentialExpressionAnalysis> results;
        if ( factorSelectionMode == FactorSelectionMode.REDO ) {
            results = redoDifferentialExpressionAnalyses( ee, config );
            addSuccessObject( ee, "Performed " + results.size() + " differential expression analyses based on a previous analyses." );
        } else {
            results = runDifferentialExpressionAnalyses( ee, config );
            addSuccessObject( ee, "Performed " + results.size() + " differential expression analyses." );
        }

        if ( config.isPersist() ) {
            refreshDeaFromGemmaWeb( ee );
        } else {
            // defaults to the current directory
            Path outputDir = requireNonNull( result.getOutputDir() );
            log.info( "Writing diff. ex. results to " + outputDir.toAbsolutePath() + "..." );
            try {
                expressionDataFileService.writeDiffExAnalysisArchiveFiles( results, outputDir, isForce() );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    /**
     * Determine which factors to use if given from the command line. Only applicable if analysis is on a single data
     * set.
     */
    private Collection<ExperimentalFactor> getFactors( Map<Long, ExperimentalFactor> factorsById, Map<String, Set<ExperimentalFactor>> factorsByName ) {
        Assert.notNull( factorIdentifiers, "No factors were set from the command line." );
        Collection<ExperimentalFactor> factors = new HashSet<>();
        for ( String factorId : factorIdentifiers ) {
            if ( factorId.contains( ":" ) ) {
                // this is an interaction, skip it
                continue;
            }
            ExperimentalFactor factor = locateExperimentalFactor( factorId, factorsById, factorsByName );
            if ( !factors.add( factor ) ) {
                log.warn( "Factor " + factorId + " was already added by either a name or ID, ignoring." );
            }
        }
        return factors;
    }

    /**
     * Determine which factor interactions to use.
     */
    private Collection<Collection<ExperimentalFactor>> getFactorInteractions( Map<Long, ExperimentalFactor> factorsById, Map<String, Set<ExperimentalFactor>> factorsByName ) {
        Assert.notNull( factorIdentifiers, "No factors were set from the command line." );
        Collection<Collection<ExperimentalFactor>> interactions = new HashSet<>();
        for ( String interactionId : factorIdentifiers ) {
            if ( !interactionId.contains( ":" ) ) {
                // this is a regular single factor, skip it
                continue;
            }
            String[] factorIds = interactionId.split( ":" );
            if ( factorIds.length != 2 ) {
                throw new IllegalArgumentException( "An interaction must have exactly two factors." );
            }
            Collection<ExperimentalFactor> interaction = new HashSet<>( 2 );
            for ( String factorId : factorIds ) {
                ExperimentalFactor factor = locateExperimentalFactor( factorId, factorsById, factorsByName );
                if ( factor.getType() != FactorType.CATEGORICAL ) {
                    throw new IllegalArgumentException( String.format( "Interactions can only be specified for categorical factors. Factor %s in interaction %s is %s.",
                            factorId, interactionId, factor.getType().name().toLowerCase() ) );
                }
                if ( !interaction.add( factor ) ) {
                    throw new IllegalArgumentException( "Factor " + factorId + " was already added by either a name or ID to interaction " + interactionId + "." );
                }
            }
            if ( !interactions.add( interaction ) ) {
                log.warn( "Interaction " + interactionId + " was already added by either a name or ID, ignoring." );
            }
        }
        return interactions;
    }

    /**
     * Obtain the subset factor if given from the command line.
     */
    @Nullable
    private ExperimentalFactor getSubsetFactor( Map<Long, ExperimentalFactor> factorById, Map<String, Set<ExperimentalFactor>> factorByName ) {
        if ( this.subsetFactorIdentifier != null ) {
            ExperimentalFactor subsetFactor = locateExperimentalFactor( this.subsetFactorIdentifier, factorById, factorByName );
            if ( subsetFactor.getType() != FactorType.CATEGORICAL ) {
                throw new IllegalArgumentException( subsetFactor + " is not categorical. A subset factor must be categorical." );
            }
            return subsetFactor;
        }
        return null;
    }

    /**
     * Locate a factor given a user-supplied identifier.
     */
    private ExperimentalFactor locateExperimentalFactor( String identifier, Map<Long, ExperimentalFactor> factorsById, Map<String, Set<ExperimentalFactor>> factorsByName ) {
        try {
            ExperimentalFactor factor = factorsById.get( Long.parseLong( identifier ) );
            if ( factor == null ) {
                throw new IllegalArgumentException( String.format( "No factor for ID %s. Possible values are:\n\t%s",
                        identifier,
                        factorsById.entrySet().stream()
                                .sorted( Map.Entry.comparingByValue( ExperimentalFactor.COMPARATOR ) )
                                .map( e -> e.getKey() + ":\t" + e.getValue() )
                                .collect( Collectors.joining( "\n\t" ) ) ) );
            }
            return factor;
        } catch ( NumberFormatException e ) {
            // ignore, will match by name
        }
        Set<ExperimentalFactor> matchingFactors = factorsByName.get( identifier );
        if ( matchingFactors == null ) {
            throw new IllegalArgumentException( String.format( "No factor for name %s. Possible values are:\n\t%s",
                    identifier,
                    factorsByName.entrySet().stream()
                            // only suggest unambiguous factors
                            .filter( e -> e.getValue().size() == 1 )
                            .sorted( Comparator.comparing( e -> e.getValue().iterator().next(), ExperimentalFactor.COMPARATOR ) )
                            .map( e -> e.getKey() + ":\t" + e.getValue().iterator().next() )
                            .collect( Collectors.joining( "\n\t" ) ) ) );
        }
        if ( matchingFactors.size() > 1 ) {
            throw new IllegalArgumentException( String.format( "More than one factors match %s, use a numerical ID instead. Possible values are:\n\t%s",
                    identifier,
                    matchingFactors.stream()
                            .sorted( ExperimentalFactor.COMPARATOR )
                            .map( f -> f.getId() + ":\t" + f ).collect( Collectors.joining( "\n\t" ) ) ) );
        }
        return matchingFactors.iterator().next();
    }

    /**
     * Run analyses from scratch based on a given configuration.
     */
    private Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses( ExpressionExperiment ee, DifferentialExpressionAnalysisConfig config ) {
        return differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee, config );
    }

    /**
     * Run the analysis using configuration based on an existing analysis.
     */
    private Collection<DifferentialExpressionAnalysis> redoDifferentialExpressionAnalyses( ExpressionExperiment ee, DifferentialExpressionAnalysisConfig config ) {
        Collection<DifferentialExpressionAnalysis> existingAnalyses = getAnalyses( ee );
        if ( existingAnalyses.isEmpty() ) {
            throw new IllegalArgumentException( "There are no existing analyses to process." );
        }
        log.info( "Will attempt to redo " + existingAnalyses.size() + " analyses for " + ee );
        return differentialExpressionAnalyzerService.redoAnalyses( ee, existingAnalyses, config,
                // FIXME: this is not exactly correct, but multiple analyses generally imply subsets
                config.isIgnoreFailingSubsets() );
    }

    /**
     * Obtain the analyses that are selected by the user (if any).
     */
    private Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment ee ) {
        Collection<DifferentialExpressionAnalysis> existingAnalyses = differentialExpressionAnalysisService
                .findByExperiment( ee, true );
        if ( analysisIds != null ) {
            Set<Long> existingAnalysesIds = existingAnalyses.stream()
                    .map( DifferentialExpressionAnalysis::getId )
                    .collect( Collectors.toSet() );
            if ( !existingAnalysesIds.containsAll( analysisIds ) ) {
                Set<Long> unknownAnalysisIds = new HashSet<>( analysisIds );
                unknownAnalysisIds.removeAll( existingAnalysesIds );
                throw new IllegalArgumentException( String.format( "Some of the requested analysis do not exist for %s: %s. Possible values are: %s.",
                        ee.getShortName(),
                        unknownAnalysisIds.stream().sorted().map( String::valueOf ).collect( Collectors.joining( ", " ) ),
                        existingAnalysesIds.stream().sorted().map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );
            }
            return existingAnalyses.stream()
                    .filter( da -> analysisIds.contains( da.getId() ) )
                    .collect( Collectors.toList() );
        } else if ( subsetIds != null ) {
            Set<Long> existingSubsetIds = existingAnalyses.stream()
                    .map( da -> da.getExperimentAnalyzed().getId() )
                    .collect( Collectors.toSet() );
            if ( !existingSubsetIds.containsAll( subsetIds ) ) {
                Set<Long> unknownSubsetIds = new HashSet<>( subsetIds );
                unknownSubsetIds.removeAll( existingSubsetIds );
                throw new IllegalArgumentException( String.format( "Some of the requested subsets do not exist for %s: %s. Possible values are: %s.",
                        ee.getShortName(),
                        unknownSubsetIds.stream().sorted().map( String::valueOf ).collect( Collectors.joining( ", " ) ),
                        existingSubsetIds.stream().sorted().map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );
            }
            return existingAnalyses.stream()
                    .filter( da -> subsetIds.contains( da.getExperimentAnalyzed().getId() ) )
                    .collect( Collectors.toList() );
        } else {
            return existingAnalyses;
        }
    }

    private void refreshDeaFromGemmaWeb( ExpressionExperiment ee ) {
        try {
            refreshExpressionExperimentFromGemmaWeb( ee, false, true );
        } catch ( Exception e ) {
            addWarningObject( ee, "Failed to refresh DEAs from Gemma Web.", e );
        }
    }

    private class DiffExAnalysisCompletionSource implements CompletionSource {

        private final ExpressionExperiment expressionExperiment;
        private final boolean showSubSetId;

        private DiffExAnalysisCompletionSource( ExpressionExperiment expressionExperiment, boolean showSubSetId ) {
            this.expressionExperiment = expressionExperiment;
            this.showSubSetId = showSubSetId;
        }

        @Override
        public List<Completion> getCompletions() {
            return differentialExpressionAnalysisService.findByExperiment( expressionExperiment, true )
                    .stream()
                    .map( a -> new Completion( String.valueOf( showSubSetId ? a.getExperimentAnalyzed().getId() : a.getId() ), showSubSetId ? a.getExperimentAnalyzed().toString() : a.toString() ) )
                    .collect( Collectors.toList() );
        }
    }

    /**
     * A completion source that provide subset factors.
     */
    private class SubSetFactorCompletionSource implements CompletionSource {

        private final ExperimentalDesign experimentalDesign;

        private SubSetFactorCompletionSource( ExperimentalDesign experimentalDesign ) {
            this.experimentalDesign = experimentalDesign;
        }

        @Override
        public List<Completion> getCompletions() {
            Map<ExperimentalFactor, LinkedHashSet<String>> factorIds = getPossibleIds( experimentalDesign );
            List<Completion> c = new ArrayList<>();
            for ( ExperimentalFactor factor : experimentalDesign.getExperimentalFactors() ) {
                if ( factor.getType() == FactorType.CATEGORICAL ) {
                    for ( String id : factorIds.get( factor ) ) {
                        c.add( new Completion( id, factor.toString() ) );
                    }
                }
            }
            return c;
        }
    }

    /**
     * A completion source that provide factors and interactions.
     */
    private class FactorsAndInteractionsCompletionSource implements CompletionSource {

        private final ExperimentalDesign experimentalDesign;
        @Nullable
        private final ExperimentalFactor subsetFactor;

        private FactorsAndInteractionsCompletionSource( ExperimentalDesign experimentalDesign, @Nullable ExperimentalFactor subsetFactor ) {
            this.experimentalDesign = experimentalDesign;
            this.subsetFactor = subsetFactor;
        }

        @Override
        public List<Completion> getCompletions() {
            List<Completion> c = new ArrayList<>();
            Map<ExperimentalFactor, LinkedHashSet<String>> possibleIds = getPossibleIds( experimentalDesign );
            for ( ExperimentalFactor factor : experimentalDesign.getExperimentalFactors() ) {
                if ( subsetFactor != null && subsetFactor.equals( factor ) ) {
                    // skip the subset factor
                    continue;
                }
                Set<String> factorIds = possibleIds.get( factor );
                for ( String id : factorIds ) {
                    c.add( new Completion( id, factor.toString() ) );
                }
                // print all possible interactions
                for ( ExperimentalFactor factor2 : experimentalDesign.getExperimentalFactors() ) {
                    if ( subsetFactor != null && subsetFactor.equals( factor2 ) ) {
                        // skip the subset factor
                        continue;
                    }
                    // only categorical interactions are supported
                    if ( factor != factor2
                            && factor.getType() == FactorType.CATEGORICAL
                            && factor2.getType() == FactorType.CATEGORICAL ) {
                        String interactionDesc = factor + " x " + factor2;
                        Set<String> factor2Ids = possibleIds.get( factor2 );
                        for ( String i1 : factorIds ) {
                            for ( String i2 : factor2Ids ) {
                                if ( !i1.equals( i2 ) ) {
                                    c.add( new Completion( i1 + ":" + i2, interactionDesc ) );
                                }
                            }
                        }
                    }
                }
            }
            return c;
        }
    }

    /**
     * Obtain all possible identifiers for each factor, ignoring ambiguous ones.
     */
    private Map<ExperimentalFactor, LinkedHashSet<String>> getPossibleIds( ExperimentalDesign ed ) {
        Map<ExperimentalFactor, LinkedHashSet<String>> result = new HashMap<>();
        for ( ExperimentalFactor factor : ed.getExperimentalFactors() ) {
            LinkedHashSet<String> ids = new LinkedHashSet<>( 3 );
            ids.add( String.valueOf( factor.getId() ) );
            ids.add( sanitizeFactorName( factor.getName() ) );
            if ( factor.getCategory() != null && factor.getCategory().getCategory() != null ) {
                ids.add( sanitizeFactorName( factor.getCategory().getCategory() ) );
            }
            result.put( factor, ids );
        }
        // remove ambiguous IDs
        Set<String> seenIds = new HashSet<>();
        Set<String> ambiguousIds = new HashSet<>();
        for ( Set<String> possibleIds : result.values() ) {
            for ( String id : possibleIds ) {
                if ( !seenIds.add( id ) ) {
                    ambiguousIds.add( id );
                }
            }
        }
        result.values().forEach( v -> v.removeAll( ambiguousIds ) );
        return result;
    }

    /**
     * Replace spaces and colons in factor names with underscores.
     */
    private String sanitizeFactorName( String factorName ) {
        return StringUtils.strip( factorName
                .replace( ' ', '_' )
                .replace( ':', '_' ) );
    }
}
