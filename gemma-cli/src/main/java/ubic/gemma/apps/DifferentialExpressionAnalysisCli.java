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
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
        COMPLETE_SUBSET_FACTORS,
        COMPLETE_FACTORS
    }

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

    private boolean persist = true;
    private boolean makeArchiveFiles = true;
    private boolean ignoreFailingSubsets = false;

    enum FactorSelectionMode {
        REDO,
        AUTOMATIC,
        MANUAL
    }

    @Nullable
    private Path outputDir = null;

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

        options.addOption( "redo", "redo", false,
                "Try to base analysis on previous analysis's choice of statistical model. "
                        + "Will re-run all analyses for the experiment" );

        addSingleExperimentEnumOption( options, "type", "type",
                "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design.",
                AnalysisType.class,
                getAnalysisTypeDescriptions() );

        addSingleExperimentOption( options, Option.builder( "factors" ).longOpt( "factors" )
                .hasArgs()
                .valueSeparator( ',' )
                .desc( "ID numbers, categories or names of the factor(s) to use, comma-delimited, with spaces replaced by underscores. "
                        + "If omitted, factors will be selected automatically. "
                        + "This is incompatible with " + formatOption( options, "redo" ) + "." )
                .build() );

        addSingleExperimentOption( options, "subset", "subset", true,
                "ID number, category or name of the factor to use for subsetting the analysis. "
                        + "If used without specifying  " + formatOption( options, "factors" ) + ", factors will be selected automatically among the remaining one in the design. "
                        + "If the experiment already has subsets for the factor, those will be reused. "
                        + "This is incompatible with " + formatOption( options, "redo" ) + "." );

        options.addOption( "usebatch", "use-batch-factor", false, "If a 'batch' factor is available, use it. Otherwise, batch information can/will be ignored in the analysis. This is incompatible with " + formatOption( options, "factors" ) + " and " + formatOption( options, "redo" ) + "." );

        options.addOption( "nodb", "no-db", false, "Output files only to your gemma.appdata.home (unless you also set -nofiles) instead of persisting to the database" );

        options.addOption( "ignoreFailingSubsets", "ignore-failing-subsets", false, "Ignore failing subsets and continue processing other subsets." );

        options.addOption( "delete", "delete", false, "Instead of running the analysis on the given experiments, remove the old analyses. Use with care!" );

        options.addOption( "nobayes", "no-bayes", false, "Do not apply empirical-Bayes moderated statistics. Default is to use eBayes." );

        options.addOption( "nofiles", "no-files", false, "Don't create archive files after analysis. Default is to make them. This is incompatible with " + formatOption( options, "nodb" ) + "." );

        options.addOption( Option.builder( "outputDir" )
                .longOpt( "output-dir" )
                .hasArg().type( Path.class )
                .desc( "Directory to write output files to. If not specified, it will default to the current directory. This requires " + formatOption( options, "nodb" ) + " to be set." )
                .build() );

        addSingleExperimentOption( options, Option.builder( "completeSubsetFactors" )
                .longOpt( "complete-subset-factors" )
                .desc( "Provide completions ofr subset factors." ).build() );

        addSingleExperimentOption( options, Option.builder( "completeFactors" ).longOpt( "complete-factors" )
                .desc( "Provide completion for factors and interactions." ).build() );
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
        } else if ( commandLine.hasOption( "factors" ) ) {
            this.factorSelectionMode = FactorSelectionMode.MANUAL;
            this.factorIdentifiers = Arrays.asList( commandLine.getOptionValues( "factors" ) );
        } else {
            this.factorSelectionMode = FactorSelectionMode.AUTOMATIC;
        }
        // subset analysis can only be done in manual mode
        // note we add the given factor to the list of factors overall to make sure it is considered
        this.subsetFactorIdentifier = getOptionValue( commandLine, "subset", requires( toBeUnset( "redo" ) ) );
        // we can only force the use of a batch factor during automatic selection
        this.ignoreBatch = !hasOption( commandLine, "usebatch", requires( allOf( toBeUnset( "factors" ), toBeUnset( "redo" ) ) ) );
        this.moderateStatistics = !commandLine.hasOption( "nobayes" );
        this.persist = !commandLine.hasOption( "nodb" );
        this.makeArchiveFiles = !hasOption( commandLine, "nofiles", requires( toBeUnset( "nodb" ) ) );
        this.outputDir = getParsedOptionValue( commandLine, "outputDir", requires( toBeSet( "nodb" ) ) );
        this.ignoreFailingSubsets = commandLine.hasOption( "ignoreFailingSubsets" );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ee = this.eeService.thawLite( ee );

        if ( mode == Mode.DELETE ) {
            log.info( "Deleting any analyses for experiment=" + ee );
            int da = differentialExpressionAnalyzerService.deleteAnalyses( ee );
            addSuccessObject( ee, "Deleted " + da + " analyses." );
            return;
        }

        if ( ee.getExperimentalDesign() == null ) {
            throw new IllegalStateException( ee + " does not have an experimental design." );
        }

        if ( mode == Mode.COMPLETE_SUBSET_FACTORS ) {
            CompletionUtils.writeCompletions( new SubSetFactorCompletionSource( ee.getExperimentalDesign() ),
                    getCliContext().getOutputStream() );
            return;
        }

        if ( mode == Mode.COMPLETE_FACTORS ) {
            CompletionUtils.writeCompletions( new FactorsAndInteractionsCompletionSource( ee.getExperimentalDesign() ),
                    getCliContext().getOutputStream() );
            return;
        }

        Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign().getExperimentalFactors();
        if ( experimentalFactors.isEmpty() ) {
            /*
             * Only need to be noisy if this is the only ee. Batch processing should be less so.
             */
            throw new RuntimeException( ee + " does not have an experimental design populated." );
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

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setAnalysisType( this.type );
        config.setMakeArchiveFile( this.makeArchiveFiles );
        config.setModerateStatistics( this.moderateStatistics );
        config.setPersist( this.persist );
        config.setIgnoreFailingSubsets( this.ignoreFailingSubsets );
        config.setUseWeights( super.eeService.isRNASeq( ee ) );

        if ( factorSelectionMode == FactorSelectionMode.REDO ) {
            // selection of factors will be based on the old analysis
            log.info( "Factors will be selected based on the old analysis." );
        } else if ( factorSelectionMode == FactorSelectionMode.AUTOMATIC ) {
            log.info( "Factors will be selected automatically." );
            // automatic selection of factors
            Collection<ExperimentalFactor> factorsToUse = new HashSet<>( experimentalFactors );

            if ( subsetFactorIdentifier != null ) {
                ExperimentalFactor subsetFactor = locateExperimentalFactor( subsetFactorIdentifier, factorsById, factorsByName );
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
                throw new RuntimeException( "Experiment has too many factors (" + factorsToUse.size() + ") to run automatically. Try using the -redo flag to base it on an old analysis, or select factors manually with -factors." );
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
                        log.warn( "Added " + factor + " as a factor to analyze since it is used in an interaction. Include it explicitly to suppress this warning." );
                    }
                }
            }

            if ( config.getAnalysisType() == AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION && !factorInteractions.isEmpty() ) {
                throw new IllegalArgumentException( "Interactions cannot be specified with -factors when requesting an analysis of type " + type + "." );
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
                log.info( "Including interaction of two categorical factors automatically. To prevent this, set the analysis type to " + AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION + " or specify interactions in -factors using ':' as a delimiter." );
                factorInteractions = Collections.singleton( new HashSet<>( factors ) );
            }

            config.setSubsetFactor( subsetFactor );
            config.addFactorsToInclude( factors );
            config.addInteractionsToInclude( factorInteractions );
        }

        Collection<DifferentialExpressionAnalysis> results;
        if ( factorSelectionMode == FactorSelectionMode.REDO ) {
            results = redoDifferentialExpressionAnalyses( ee, config );
            addSuccessObject( ee, "Performed differential expression analysis based on a previous analysis." );
        } else {
            results = runDifferentialExpressionAnalyses( ee, config );
            addSuccessObject( ee, "Performed differential expression analysis." );
        }

        if ( config.isPersist() ) {
            try {
                refreshExpressionExperimentFromGemmaWeb( ee, false, true );
            } catch ( Exception e ) {
                addWarningObject( ee, "Failed to refresh " + ee + " from Gemma Web.", e );
            }
        } else {
            log.info( "Writing results to disk" );
            try {
                expressionDataFileService.writeDiffExAnalysisArchiveFiles( results, outputDir != null ? outputDir : Paths.get( "" ), isForce() );
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
            return locateExperimentalFactor( this.subsetFactorIdentifier, factorById, factorByName );
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
     * Run the analysis using configuration based on an old analysis.
     */
    private Collection<DifferentialExpressionAnalysis> redoDifferentialExpressionAnalyses( ExpressionExperiment ee, DifferentialExpressionAnalysisConfig config ) {
        Collection<DifferentialExpressionAnalysis> oldAnalyses = differentialExpressionAnalysisService
                .findByExperiment( ee, true );

        if ( oldAnalyses.isEmpty() ) {
            throw new IllegalArgumentException( "There are no old analyses to redo" );
        }

        log.info( "Will attempt to redo " + oldAnalyses.size() + " analyses for " + ee );
        return differentialExpressionAnalyzerService.redoAnalyses( ee, oldAnalyses, config,
                // FIXME: this is not exactly correct, but multiple analyses generally imply subsets
                config.isIgnoreFailingSubsets() );
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

        private FactorsAndInteractionsCompletionSource( ExperimentalDesign experimentalDesign ) {
            this.experimentalDesign = experimentalDesign;
        }

        @Override
        public List<Completion> getCompletions() {
            List<Completion> c = new ArrayList<>();
            Map<ExperimentalFactor, LinkedHashSet<String>> possibleIds = getPossibleIds( experimentalDesign );
            for ( ExperimentalFactor factor : experimentalDesign.getExperimentalFactors() ) {
                Set<String> factorIds = possibleIds.get( factor );
                for ( String id : factorIds ) {
                    c.add( new Completion( id, factor.toString() ) );
                }
                // print all possible interactions
                for ( ExperimentalFactor factor2 : experimentalDesign.getExperimentalFactors() ) {
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
