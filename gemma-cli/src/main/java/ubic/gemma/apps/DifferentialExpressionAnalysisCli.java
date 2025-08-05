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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.analysis.expression.diff.AnalysisType;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.eventType.DifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExperimentFactorUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
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
     * Indicate the type of analysis to perform.
     * <p>
     * The default is to detect it based on the dataset and the requested factors.
     */
    private AnalysisType type = null;

    /**
     * Mode for selecting factors.
     */
    private FactorSelectionMode mode;

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
    private boolean redo = false;

    /**
     * Use moderated statistics.
     */
    private boolean moderateStatistics = DifferentialExpressionAnalysisConfig.DEFAULT_MODERATE_STATISTICS;

    private boolean persist = true;
    private boolean makeArchiveFiles = true;
    private boolean ignoreFailingSubsets = false;

    enum FactorSelectionMode {
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

        options.addOption( Option.builder( "factors" ).longOpt( "factors" )
                .hasArgs()
                .valueSeparator( ',' )
                .desc( "ID numbers, categories or names of the factor(s) to use, comma-delimited, with spaces replaced by underscores" )
                .build() );

        options.addOption( "subset", "subset", true,
                "ID number, category or name of the factor to use for subsetting the analysis; must also use with -factors. "
                        + "If the experiment already has subsets for the factor, those will be reused." );

        OptionsUtils.addEnumOption( options, "type", "type",
                "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design.",
                AnalysisType.class,
                getAnalysisTypeDescriptions() );

        options.addOption( "usebatch", "use-batch-factor", false, "If a 'batch' factor is available, use it. Otherwise, batch information can/will be ignored in the analysis." );

        options.addOption( "nodb", "no-db", false, "Output files only to your gemma.appdata.home (unless you also set -nofiles) instead of persisting to the database" );

        options.addOption( "ignoreFailingSubsets", "ignore-failing-subsets", false, "Ignore failing subsets and continue processing other subsets." );

        options.addOption( "redo", "redo", false,
                "If using automatic analysis try to base analysis on previous analysis's choice of statistical model. "
                        + "Will re-run all analyses for the experiment" );

        options.addOption( "delete", "delete", false, "Instead of running the analysis on the given experiments, remove the old analyses. Use with care!" );

        options.addOption( "nobayes", "no-bayes", false, "Do not apply empirical-Bayes moderated statistics. Default is to use eBayes." );

        options.addOption( "nofiles", "no-files", false, "Don't create archive files after analysis. Default is to make them. This is incompatible with -nodb." );

        options.addOption( Option.builder( "outputDir" )
                .longOpt( "output-dir" )
                .hasArg().type( Path.class )
                .desc( "Directory to write output files to. If not specified, it will default to the current directory. This requires -nodb to be set." )
                .build() );
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
        this.type = OptionsUtils.getEnumOptionValue( commandLine, "type" );
        if ( commandLine.hasOption( "factors" ) ) {
            this.mode = FactorSelectionMode.MANUAL;
            this.factorIdentifiers.addAll( Arrays.asList( commandLine.getOptionValues( "factors" ) ) );
        } else {
            this.mode = FactorSelectionMode.AUTOMATIC;
        }
        // subset analysis can only be done in manual mode
        // note we add the given factor to the list of factors overall to make sure it is considered
        this.subsetFactorIdentifier = getOptionValue( commandLine, "subset", requires( toBeSet( "factors" ) ) );
        // we can only force the use of a batch factor during automatic selection
        this.ignoreBatch = !hasOption( commandLine, "usebatch", requires( toBeUnset( "factors" ) ) );
        this.moderateStatistics = !commandLine.hasOption( "nobayes" );
        this.persist = !commandLine.hasOption( "nodb" );
        this.makeArchiveFiles = !hasOption( commandLine, "nofiles", requires( toBeUnset( "nodb" ) ) );
        this.outputDir = getParsedOptionValue( commandLine, "outputDir", requires( toBeSet( "nodb" ) ) );
        this.ignoreFailingSubsets = commandLine.hasOption( "ignoreFailingSubsets" );
        this.redo = hasOption( commandLine, "redo", requires( allOf( toBeUnset( "subset" ), toBeUnset( "factors" ), toBeUnset( "ignoreBatch" ) ) ) );
        this.delete = commandLine.hasOption( "delete" );
    }

    @Override
    protected void processExpressionExperiments( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( type != null ) {
            throw new IllegalArgumentException( "You can only specify the analysis type when analyzing a single experiment" );
        }
        if ( subsetFactorIdentifier != null ) {
            throw new IllegalArgumentException( "You can only specify the subset factor when analyzing a single experiment" );
        }
        if ( !factorIdentifiers.isEmpty() ) {
            throw new IllegalArgumentException( "You can only specify the factors when analyzing a single experiment" );
        }
        super.processExpressionExperiments( expressionExperiments );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        config.setMakeArchiveFile( this.makeArchiveFiles );
        config.setModerateStatistics( this.moderateStatistics );
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
            throw new RuntimeException( ee + " does not have an experimental design populated." );
        }

        config.setAnalysisType( this.type );

        if ( redo ) {
            // selection of factors will be based on the old analysis
            log.info( "Factors will be selected based on the old analysis" );
        } else if ( mode == FactorSelectionMode.AUTOMATIC ) {
            log.info( "Factors will be selected automatically." );
            // automatic selection of factors
            Collection<ExperimentalFactor> factorsToUse = new HashSet<>( experimentalFactors );

            if ( this.ignoreBatch ) {
                factorsToUse.removeIf( ExperimentFactorUtils::isBatchFactor );
            }

            if ( factorsToUse.isEmpty() ) {
                throw new RuntimeException( "No factors available for " + ee.getShortName() );
            }

            if ( factorsToUse.size() > 3 ) {
                throw new RuntimeException(
                        "Experiment has too many factors to run automatically: " + ee.getShortName()
                                + "; try using the -redo flag to base it on an old analysis, or select factors manually with -factors." );
            } else {
                config.addFactorsToInclude( factorsToUse );
                if ( ( config.getAnalysisType() == null || config.getAnalysisType() == AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION ) && factorsToUse.size() == 2 ) {
                    // include interactions by default
                    log.info( "Including interaction of two factors. To prevent this, set the analysis type to " + AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION + "." );
                    config.addInteractionToInclude( factorsToUse );
                }
            }
        } else {
            // Manual selection of factors (possibly including a subset factor)
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

            Collection<ExperimentalFactor> factors = this.getFactors( factorsById, factorsByName );
            ExperimentalFactor subsetFactor = this.getSubsetFactor( factorsById, factorsByName );
            log.info( "Using " + factors.size() + " factors provided as arguments" );
            if ( subsetFactor != null ) {
                if ( factors.remove( subsetFactor ) ) {
                    log.warn( "A subset factor should not be included as a factor to analyze." );
                }
                if ( factors.isEmpty() ) {
                    throw new IllegalArgumentException( "You must specify at least one other factor when using 'subset'" );
                }
                log.info( "Subsetting by " + subsetFactor );
            }
            config.addFactorsToInclude( factors );
            config.setSubsetFactor( subsetFactor );
            /*
             * Interactions included by default. It's actually only complicated if there is a subset factor.
             */
            if ( ( config.getAnalysisType() == null || config.getAnalysisType() == AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION ) && factors.size() == 2 ) {
                log.info( "Including interaction of two factors. To prevent this, set the analysis type to " + AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION + "." );
                config.addInteractionToInclude( factors );
            }
        }

        Collection<DifferentialExpressionAnalysis> results;
        if ( redo ) {
            results = redoDifferentialExpressionAnalyses( ee, config );
        } else {
            results = runDifferentialExpressionAnalyses( ee, config );
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
        Collection<ExperimentalFactor> factors = new HashSet<>();

        for ( String factorId : factorIdentifiers ) {
            ExperimentalFactor factor = locateExperimentalFactor( factorId, factorsById, factorsByName );
            if ( ignoreBatch && ExperimentFactorUtils.isBatchFactor( factor ) ) {
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
     * Obtain the subset factor if given from the command line.
     */
    @Nullable
    private ExperimentalFactor getSubsetFactor( Map<Long, ExperimentalFactor> factorById, Map<String, Set<ExperimentalFactor>> factorByName ) {
        if ( this.subsetFactorIdentifier != null ) {
            ExperimentalFactor factor = locateExperimentalFactor( this.subsetFactorIdentifier, factorById, factorByName );
            if ( ignoreBatch && ExperimentFactorUtils.isBatchFactor( factor ) ) {
                throw new IllegalArgumentException( "Subset factor appears to be a batch, pass -usebatch to use." );
            }
            return factor;
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
}
