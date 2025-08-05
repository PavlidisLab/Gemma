/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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

import lombok.Value;
import org.apache.commons.cli.*;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import ubic.gemma.cli.util.AbstractAutoSeekingCLI;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.FileUtils;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static ubic.gemma.cli.util.EntityOptionsUtils.*;

/**
 * Base class for CLIs that needs one or more expression experiment as an input. It offers the following ways of reading
 * them in:
 * <ul>
 * <li>All EEs (if -all is supplied)</li>
 * <li>All EEs for a particular taxon.</li>
 * <li>A specific ExpressionExperimentSet, identified by name</li>
 * <li>A comma-delimited list of one or more EEs identified by short name given on the command line</li>
 * <li>From a file, with one short name per line.</li>
 * <li>EEs matching a query string (e.g., 'brain')</li>
 * <li>(Optional) 'Auto' mode, in which experiments to analyze are selected automatically based on their workflow state.
 * This can be enabled and modified by subclasses who override the "needToRun" method.</li>
 * <li>All EEs that were last processed after a given date, similar to 'auto' otherwise.</li>
 * </ul>
 * Some of these options can be (or should be) combined, and modified by a (optional) "force" option, and will have
 * customized behavior.
 * <p>
 * In addition, EEs can be excluded based on a list given in a separate file.
 *
 * @author Paul
 */
public abstract class ExpressionExperimentManipulatingCLI extends AbstractAutoSeekingCLI<ExpressionExperiment> {

    @Autowired
    protected ExpressionExperimentService eeService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    protected AuditTrailService auditTrailService;
    @Autowired
    protected AuditEventService auditEventService;
    @Autowired
    protected EntityLocator entityLocator;
    @Autowired
    protected EntityUrlBuilder entityUrlBuilder;
    @Autowired
    private GemmaRestApiClient gemmaRestApiClient;

    /**
     * Single-experiment mode.
     */
    private boolean singleExperimentMode = false;

    /**
     * Default to all datasets if no options are supplied.
     */
    private boolean defaultToAll = false;

    /**
     * Try to use references instead of actual entities.
     */
    private boolean useReferencesIfPossible = false;

    /**
     * Abort processing experiments if an error occurs.
     */
    private boolean abortOnError = false;

    /**
     * List of options only available when a single experiment is processed.
     */
    private final Set<String> singleExperimentOptions = new HashSet<>();

    /**
     * Process all experiments.
     */
    private boolean all;
    /**
     * List of EE IDs to process.
     */
    private String[] ees;
    /**
     * Name or ID of an EE set whose experiments are to be processed.
     */
    private String eeSet;
    /**
     * File containing experiments.
     */
    private Path file;
    /**
     * Query to match experiments.
     */
    private String query;
    /**
     * Only consider datasets with the given taxon.
     */
    private String taxonName;
    /**
     * File containing experiments to exclude.
     */
    private Path excludeFile;
    /**
     * Subset of {@link #singleExperimentOptions} being used.
     */
    private final Set<String> singleExperimentOptionsUsed = new HashSet<>();

    protected ExpressionExperimentManipulatingCLI() {
        super( ExpressionExperiment.class );
        setRequireLogin();
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    /**
     * Add an option that is only available in single-experiment mode.
     */
    protected void addSingleExperimentOption( Options options, Option option ) {
        option.setDescription( option.getDescription() + "\n" + "This option is not available when processing more than one experiment." );
        options.addOption( option );
        singleExperimentOptions.add( option.getOpt() );
    }

    /**
     * Add an option that is only available in single-experiment mode.
     */
    protected void addSingleExperimentOption( Options options, String opt, String longOpt, boolean hasArg, String description ) {
        options.addOption( opt, longOpt, hasArg, description + "\n" + "This option is not available when processing more than one experiment." );
        singleExperimentOptions.add( opt );
    }

    /**
     * Add an option that is only available in single-experiment mode to an option group.
     * <p>
     * The group itself must be added to the options via {@link Options#addOptionGroup}
     */
    private void addSingleExperimentOption( OptionGroup optionGroup, Option option ) {
        option.setDescription( option.getDescription() + "\n" + "This option is not available when processing more than one experiment." );
        optionGroup.addOption( option );
        singleExperimentOptions.add( option.getOpt() );
    }

    protected static final String OUTPUT_FILE_OPTION = "o",
            OUTPUT_DIR_OPTION = "d",
            STANDARD_LOCATION_OPTION = "standardLocation",
            STANDARD_OUTPUT_OPTION = "stdout";

    /**
     * Add options for writing expression data files, such as raw or processed data files.
     *
     * @see ubic.gemma.core.analysis.service.ExpressionDataFileService
     * @see ubic.gemma.core.analysis.service.ExpressionDataFileUtils
     * @param allowStandardLocation if true, the standard location option will be added
     */
    protected void addExpressionDataFileOptions( Options options, String what, boolean allowStandardLocation ) {
        OptionGroup og = new OptionGroup();
        addSingleExperimentOption( og, Option.builder( OUTPUT_FILE_OPTION )
                .longOpt( "output-file" ).hasArg().type( Path.class )
                .desc( "Write " + what + " to the given output file." ).build() );
        og.addOption( Option.builder( OUTPUT_DIR_OPTION )
                .longOpt( "output-dir" ).hasArg().type( Path.class )
                .desc( "Write " + what + " inside the given directory." ).build() );
        if ( allowStandardLocation ) {
            og.addOption( Option.builder( STANDARD_LOCATION_OPTION )
                    .longOpt( "standard-location" )
                    .desc( "Write " + what + " to the standard location under ${gemma.appdata.home}/dataFiles. This is the default if no other destination is selected." )
                    .build() );
        }
        addSingleExperimentOption( og, Option.builder( STANDARD_OUTPUT_OPTION )
                .longOpt( "stdout" )
                .desc( "Write " + what + " to standard output." + ( !allowStandardLocation ? " This is the default if no other destination is selected." : "" ) )
                .build() );
        options.addOptionGroup( og );
    }

    @Value
    protected class ExpressionDataFileResult {
        /**
         * Write the file to the standard location in the {@code ${gemma.appdata.home}/dataFiles} directory.
         */
        boolean standardLocation;
        /**
         * Write the file to standard output.
         */
        boolean standardOutput;
        /**
         * Write the file to the given output file.
         */
        @Nullable
        Path outputFile;
        /**
         * Write the file to the given output directory.
         */
        @Nullable
        Path outputDir;

        /**
         *
         * @param filenameToUseIfDirectory if the output directory is set, this filename will be used to create the
         *                                 output file. Use utilities in {@link ubic.gemma.core.analysis.service.ExpressionDataFileUtils}
         *                                 to generate the filename.
         */
        public Path getOutputFile( String filenameToUseIfDirectory ) {
            if ( getOutputFile() != null ) {
                return checkIfExists( outputFile );
            } else if ( getOutputDir() != null ) {
                return checkIfExists( getOutputDir().resolve( filenameToUseIfDirectory ) );
            } else {
                throw new IllegalStateException( "This result does not have an output file or directory set." );
            }
        }

        private Path checkIfExists( Path o ) {
            if ( !isForce() && Files.exists( o ) ) {
                throw new RuntimeException( o + " already exists, use -" + FORCE_OPTION + " to overwrite it." );
            }
            return o;
        }
    }

    /**
     * Obtain the result of the expression data file options added by {@link #addExpressionDataFileOptions(Options, String, boolean)}.
     * @param allowStandardLocation if true, the standard location option will be considered and used as a default if no
     *                              other destination is selected. Otherwise, standard output will be used as a default.
     */
    protected ExpressionDataFileResult getExpressionDataFileResult( CommandLine commandLine, boolean allowStandardLocation ) throws ParseException {
        if ( !OptionsUtils.hasAnyOption( commandLine, STANDARD_LOCATION_OPTION, STANDARD_OUTPUT_OPTION, OUTPUT_FILE_OPTION, OUTPUT_DIR_OPTION ) ) {
            if ( allowStandardLocation ) {
                log.debug( "No expression data file options provided, defaulting to -standardLocation/--standard-location." );
                return new ExpressionDataFileResult( true, false, null, null );
            } else {
                log.debug( "No expression data file options provided, defaulting to -standardOutput/--standard-output." );
                return new ExpressionDataFileResult( false, true, null, null );
            }
        }
        return new ExpressionDataFileResult(
                commandLine.hasOption( STANDARD_LOCATION_OPTION ),
                commandLine.hasOption( STANDARD_OUTPUT_OPTION ),
                commandLine.getParsedOptionValue( OUTPUT_FILE_OPTION ),
                commandLine.getParsedOptionValue( OUTPUT_DIR_OPTION ) );
    }

    @Override
    protected final void buildOptions( Options options ) {
        addDatasetOption( options, "e", "experiment",
                "Dataset identifier. Most tools recognize comma-delimited values given on the command line, "
                        + "and if this option is omitted (and none other provided), the tool will be applied to all expression experiments." );

        if ( singleExperimentMode ) {
            buildExperimentOptions( options );
            return;
        }

        if ( !defaultToAll ) {
            options.addOption( "all", false, "Process all expression experiments" );
        }

        Option eeFileListOption = Option.builder( "f" ).hasArg().type( Path.class ).argName( "file" )
                .desc( "File with list of short names or IDs of expression experiments (one per line; use instead of '-e')" )
                .longOpt( "eeListfile" ).build();
        options.addOption( eeFileListOption );

        addExperimentSetOption( options, "eeset", "experiment-set", "Name of expression experiment set to use" );

        Option eeSearchOption = Option.builder( "q" ).hasArg().argName( "expressionQuery" )
                .desc( "Use a query string for defining which expression experiments to use" )
                .longOpt( "expressionQuery" ).build();
        options.addOption( eeSearchOption );

        addTaxonOption( options, "t", "taxon", "Taxon of the expression experiments and genes" );

        Option excludeEeOption = Option.builder( "x" ).hasArg().type( Path.class ).argName( "file" )
                .desc( "File containing list of expression experiments to exclude" )
                .longOpt( "excludeEEFile" ).build();
        options.addOption( excludeEeOption );

        addBatchOption( options );

        buildExperimentOptions( options );
    }

    protected void buildExperimentOptions( Options options ) {

    }

    @Override
    protected final void processOptions( CommandLine commandLine ) throws ParseException {
        boolean hasAnyDatasetOptions = commandLine.hasOption( "all" ) || commandLine.hasOption( "eeset" )
                || commandLine.hasOption( "e" ) || commandLine.hasOption( 'f' ) || commandLine.hasOption( 'q' );
        Assert.isTrue( hasAnyDatasetOptions || defaultToAll, "At least one of -all, -e, -eeset, -f, or -q must be provided." );
        super.processOptions( commandLine );
        if ( defaultToAll && !hasAnyDatasetOptions ) {
            this.all = true;
        } else {
            this.all = commandLine.hasOption( "all" );
        }
        if ( commandLine.hasOption( 'e' ) ) {
            String optionValue = commandLine.getOptionValue( 'e' );
            Assert.isTrue( StringUtils.isNotBlank( optionValue ), "List of EE identifiers must not be blank." );
            this.ees = StringUtils.split( optionValue, "," );
        }
        this.eeSet = commandLine.getOptionValue( "eeset" );
        this.file = commandLine.getParsedOptionValue( 'f' );
        this.query = commandLine.getOptionValue( 'q' );
        this.taxonName = commandLine.getOptionValue( 't' );
        this.excludeFile = commandLine.getParsedOptionValue( 'x' );
        for ( Option option : commandLine.getOptions() ) {
            if ( singleExperimentOptions.contains( option.getOpt() ) ) {
                singleExperimentOptionsUsed.add( option.getOpt() );
            }
        }
        processExperimentOptions( commandLine );
    }

    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {

    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        // intentionally a TreeSet over IDs, to prevent proxy initialization via hashCode()
        Collection<ExpressionExperiment> expressionExperiments = new TreeSet<>( Comparator.comparing( ExpressionExperiment::getId ) );

        if ( all ) {
            if ( useReferencesIfPossible ) {
                log.info( "Loading all expression experiments by reference..." );
                expressionExperiments.addAll( eeService.loadAllReferences() );
            } else {
                log.warn( "Loading all expression experiments, this might take a while..." );
                expressionExperiments.addAll( eeService.loadAll() );
            }
        }

        if ( eeSet != null ) {
            expressionExperiments.addAll( this.experimentsFromEeSet( eeSet ) );
        }

        if ( ees != null ) {
            expressionExperiments.addAll( this.experimentsFromCliList( ees ) );
        }

        if ( file != null ) {
            log.info( "Reading experiment list from " + file );
            try {
                expressionExperiments.addAll( this.readExpressionExperimentListFile( file ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }

        if ( query != null ) {
            Taxon taxon = this.taxonName != null ? entityLocator.locateTaxon( this.taxonName ) : null;
            log.info( "Processing all experiments that match query " + query + ( taxon != null ? " in taxon " + taxon : "" ) );
            try {
                expressionExperiments.addAll( this.findExpressionExperimentsByQuery( query, taxon ) );
            } catch ( SearchException e ) {
                log.error( "Failed to retrieve EEs for the passed query via -q.", e );
            }
        }

        if ( excludeFile != null && !expressionExperiments.isEmpty() ) {
            try {
                this.excludeFromFile( expressionExperiments, excludeFile );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }

        if ( !isForce() && !expressionExperiments.isEmpty() ) {

            if ( isAutoSeek() ) {
                if ( this.getAutoSeekEventType() == null ) {
                    throw new IllegalStateException( "Programming error: there is no 'autoSeekEventType' set" );
                }
                log.info( "Filtering for experiments lacking a " + this.getAutoSeekEventType().getSimpleName()
                        + " event" );
                auditEventService.retainLackingEvent( expressionExperiments, this.getAutoSeekEventType() );
            }

            this.removeTroubledExperiments( expressionExperiments );
        }

        expressionExperiments = preprocessExpressionExperiments( expressionExperiments );

        if ( expressionExperiments.isEmpty() ) {
            throw new RuntimeException( "No expression experiments matched the given options." );
        } else if ( expressionExperiments.size() == 1 ) {
            ExpressionExperiment ee = expressionExperiments.iterator().next();
            log.info( "Final dataset: " + formatExperiment( ee ) );
            ExpressionExperiment bas = expressionExperiments.iterator().next();
            Assert.notNull( bas, "Cannot process a null ExpressionExperiment." );
            processExpressionExperiment( bas );
        } else {
            if ( !singleExperimentOptionsUsed.isEmpty() ) {
                throw new IllegalStateException( String.format( "There are single-experiment options used: %s, but more than one experiments was found.",
                        singleExperimentOptionsUsed.stream().map( o -> "-" + o ).collect( Collectors.joining( ", " ) ) ) );
            }
            log.info( String.format( "Final list: %d expression experiments", expressionExperiments.size() ) );
            processExpressionExperiments( expressionExperiments );
        }
    }

    /**
     * Preprocess the set of {@link ExpressionExperiment} before invoking {@link #processExpressionExperiments(Collection)} or
     * {@link #processExpressionExperiment(ExpressionExperiment)}.
     * <p>
     * This can be an opportunity to filter or modify the set of experiments.
     */
    protected Collection<ExpressionExperiment> preprocessExpressionExperiments( Collection<ExpressionExperiment> expressionExperiments ) {
        return expressionExperiments;
    }

    /**
     * Process multiple {@link ExpressionExperiment}.
     * <p>
     * This only called if more than one experiment was found.
     */
    protected void processExpressionExperiments( Collection<ExpressionExperiment> expressionExperiments ) {
        setEstimatedMaxTasks( expressionExperiments.size() );
        for ( ExpressionExperiment ee : expressionExperiments ) {
            try {
                Assert.notNull( ee, "Cannot process a null ExpressionExperiment." );
                processExpressionExperiment( ee );
            } catch ( Exception e ) {
                addErrorObject( toBatchObject( ee ), e );
                if ( abortOnError ) {
                    throw new RuntimeException( "Aborted processing due to error.", e );
                }
            }
        }
    }

    /**
     * Process an {@link ExpressionExperiment}.
     */
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        throw new UnsupportedOperationException( "This command line does support experiments." );
    }

    protected final Serializable toBatchObject( @Nullable ExpressionExperiment object ) {
        if ( object == null ) {
            return null;
        }
        if ( Hibernate.isInitialized( object ) ) {
            return object.getShortName();
        } else {
            return "ExpressionExperiment Id=" + object.getId();
        }
    }

    private void excludeFromFile( Collection<ExpressionExperiment> expressionExperiments, Path excludeEeFileName ) throws IOException {
        assert !expressionExperiments.isEmpty();
        Collection<ExpressionExperiment> excludeExperiments;
        excludeExperiments = this.readExpressionExperimentListFile( excludeEeFileName );
        int before = expressionExperiments.size();
        if ( expressionExperiments.removeAll( excludeExperiments ) ) {
            int removed = before - expressionExperiments.size();
            log.info( "Excluded " + removed + " experiments from " + excludeEeFileName + "." );
        }
    }

    private List<ExpressionExperiment> experimentsFromCliList( String[] identifiers ) {
        List<ExpressionExperiment> ees = new ArrayList<>( identifiers.length );
        for ( String identifier : identifiers ) {
            ees.add( entityLocator.locateExpressionExperiment( identifier, useReferencesIfPossible ) );
        }
        return ees;
    }

    private Set<ExpressionExperiment> experimentsFromEeSet( String optionValue ) {
        Assert.isTrue( StringUtils.isNotBlank( optionValue ), "Please provide an eeset name" );
        ExpressionExperimentSet eeSet;
        try {
            eeSet = expressionExperimentSetService.loadOrFail( Long.parseLong( optionValue ) );
        } catch ( NumberFormatException e ) {
            Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( optionValue );
            if ( sets.size() > 1 ) {
                throw new IllegalArgumentException( "More than on EE set has name '" + optionValue + "'" );
            } else if ( sets.isEmpty() ) {
                throw new IllegalArgumentException( "No EE set has name '" + optionValue + "'" );
            }
            eeSet = sets.iterator().next();
        }
        return eeSet.getExperiments();
    }

    /**
     * Use the search engine to locate expression experiments.
     */
    private Collection<ExpressionExperiment> findExpressionExperimentsByQuery( String query, @Nullable Taxon taxon ) throws SearchException {
        Assert.isTrue( StringUtils.isNotBlank( query ), "Query must not be blank." );
        // explicitly support one case
        if ( query.matches( "GPL[0-9]+" ) ) {
            List<ExpressionExperiment> ees = new ArrayList<>();
            ArrayDesign ad = arrayDesignService.findByShortName( query );
            if ( ad != null ) {
                Collection<ExpressionExperiment> ees2 = arrayDesignService.getExpressionExperiments( ad );
                ees.addAll( ees2 );
                log.info( ees.size() + " experiments matched to platform " + ad );
            }
            return ees;
        }

        Collection<SearchResult<ExpressionExperiment>> eeSearchResults = searchService
                .search( SearchSettings.expressionExperimentSearch( query )
                        .withFillResults( !useReferencesIfPossible ) )
                .getByResultObjectType( ExpressionExperiment.class );

        Collection<ExpressionExperiment> ees;
        if ( useReferencesIfPossible ) {
            // search results are not filled, so we need to get a bunch of proxies
            Set<Long> ids = eeSearchResults.stream().map( SearchResult::getResultId ).collect( Collectors.toSet() );
            ees = eeService.loadReferences( ids );
        } else {
            ees = eeSearchResults.stream()
                    .filter( Objects::nonNull )// ee no longer valid, could be an outdated compass hit
                    .map( SearchResult::getResultObject )
                    .collect( Collectors.toList() );
        }

        // Filter out all the ee that are not of correct taxon
        if ( taxon != null ) {
            Map<ExpressionExperiment, Taxon> taxa = eeService.getTaxa( ees );
            ees.removeIf( ee -> !taxon.equals( taxa.get( ee ) ) );
        }

        log.info( ees.size() + " Expression experiments matched '" + query + "'" );

        return ees;
    }


    /**
     * Load expression experiments based on a list of short names or IDs in a file. Only the first column of the file is
     * used, comments (#) are allowed.
     */
    private Collection<ExpressionExperiment> readExpressionExperimentListFile( Path fileName ) throws IOException {
        List<String> idlist = FileUtils.readListFileToStrings( fileName );
        List<ExpressionExperiment> ees = new ArrayList<>( idlist.size() );
        log.info( String.format( "Found %d experiment identifiers in %s", idlist.size(), fileName ) );
        int count = 0;
        for ( String id : idlist ) {
            ExpressionExperiment ee = entityLocator.locateExpressionExperiment( id, useReferencesIfPossible );
            count++;
            ees.add( ee );
            if ( idlist.size() > 500 && count > 0 && count % 500 == 0 ) {
                log.info( "Loaded " + count + " experiments ..." );
            }
        }
        log.info( String.format( "Loaded %d experiments for processing from %s", ees.size(), fileName ) );
        return ees;
    }

    /**
     * Obtain EEs that are troubled.
     */
    private void removeTroubledExperiments( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( expressionExperiments.isEmpty() ) {
            log.warn( "No experiments to remove troubled from" );
            return;
        }

        // it's not possible to check the curation details directly as that might trigger proxy initialization
        Set<Long> troubledIds = new HashSet<>( eeService.loadTroubledIds() );

        // only retain non-troubled experiments
        AtomicInteger removedTroubledExperiments = new AtomicInteger();
        expressionExperiments.removeIf( ee -> {
            // for subsets, check source experiment troubled flag
            if ( troubledIds.contains( ee.getId() ) ) {
                removedTroubledExperiments.incrementAndGet();
                return true;
            } else {
                return false;
            }
        } );
        if ( removedTroubledExperiments.get() > 0 ) {
            log.info( String.format( "Removed %d troubled experiments, leaving %d to be processed; use -%s to include those.",
                    removedTroubledExperiments.get(), expressionExperiments.size(), FORCE_OPTION ) );
        }
    }

    /**
     * Refresh a dataset for Gemma Web.
     * @param refreshProcessedVectors if true, refresh processed vectors from the caches
     */
    protected void refreshExpressionExperimentFromGemmaWeb( ExpressionExperiment ee, boolean refreshProcessedVectors, boolean refreshReports ) throws Exception {
        StopWatch timer = StopWatch.createStarted();
        // using IDs here to prevent proxy initialization
        GemmaRestApiClient.Response response = gemmaRestApiClient
                .perform( "/datasets/" + ee.getId() + "/refresh",
                        "refreshVectors", refreshProcessedVectors,
                        "refreshReports", refreshReports );
        if ( response instanceof GemmaRestApiClient.DataResponse ) {
            log.info( "Successfully refreshed dataset with ID " + ee.getId() + " from Gemma Web in " + timer.getTime() + " ms." );
        } else if ( response instanceof GemmaRestApiClient.ErrorResponse ) {
            GemmaRestApiClient.ErrorResponse errorResponse = ( GemmaRestApiClient.ErrorResponse ) response;
            throw new RuntimeException( String.format( "Unexpected reply from refreshing datasets with ID %d: got status code %d with message: %s",
                    ee.getId(),
                    errorResponse.getError().getCode(),
                    errorResponse.getError().getMessage() ) );
        } else {
            throw new RuntimeException( "Unknown response from the REST API: " + response );
        }
    }

    /**
     * Enable the single-experiment mode.
     */
    protected void setSingleExperimentMode() {
        Assert.state( !this.singleExperimentMode, "Single experiment mode is already enabled." );
        this.singleExperimentMode = true;
    }

    /**
     * Default to all datasets if no options are provided.
     * <p>
     * This is a very dangerous setting that should be combined with {@link #useReferencesIfPossible}.
     */
    public void setDefaultToAll() {
        Assert.state( !this.defaultToAll, "Default to all is already enabled." );
        this.defaultToAll = true;
    }

    /**
     * Set this to allow reference to be retrieved instead of actual entities.
     * <p>
     * This only works for entities retrieved by ID.
     * <p>
     * When this is enabled, do not access anything but {@link ExpressionExperiment#getId()}, or else proxy-initialization
     * will be triggered, and you will have to deal with a {@link org.hibernate.LazyInitializationException}.
     * <p>
     * The default is false.
     */
    protected void setUseReferencesIfPossible() {
        Assert.state( !this.useReferencesIfPossible, "Use references if possible is already enabled." );
        this.useReferencesIfPossible = true;
    }

    /**
     * Indicate if this CLI should abort on error or move on to the next experiment.
     */
    public boolean isAbortOnError() {
        return abortOnError;
    }

    /**
     * Set this to stop processing experiments if an error occurs.
     */
    protected void setAbortOnError() {
        Assert.state( !this.abortOnError, "Abort on error is already enabled." );
        this.abortOnError = true;
    }

    /**
     * Render an experiment to string, with special handling in case of an uninitialized proxy.
     * <p>
     * Use this for printing datasets if {@link #useReferencesIfPossible} is set to prevent {@link org.hibernate.LazyInitializationException}.
     */
    protected String formatExperiment( ExpressionExperiment bas ) {
        if ( Hibernate.isInitialized( bas ) ) {
            return bas + " " + entityUrlBuilder.fromHostUrl().entity( bas ).web().toUriString();
        } else {
            return "ExpressionExperiment Id=" + bas.getId() + " " + entityUrlBuilder.fromHostUrl().entity( ( ExpressionExperiment ) bas ).web().toUriString();
        }
    }

    @Override
    protected boolean noNeedToRun( ExpressionExperiment auditable, @Nullable Class<? extends AuditEventType> eventClass ) {
        if ( super.noNeedToRun( auditable, eventClass ) ) {
            return true;
        }

        // special case for expression experiments - check associated ADs.
        for ( ArrayDesign ad : eeService.getArrayDesignsUsed( auditable ) ) {
            if ( ad.getCurationDetails().getTroubled() ) {
                addErrorObject( auditable, "Associated platform " + ad.getShortName() + " has an active troubled flag, use - " + FORCE_OPTION + "to process anyway." );
                return true; // not ok if even one parent AD is troubled, no need to check the remaining ones.
            }
        }

        return false;
    }

    /**
     * Read a changelog entry from the console.
     * @param defaultText a default text to be shown in the editor, or null to keep the file empty
     */
    protected String readChangelogEntryFromConsole( ExpressionExperiment expressionExperiment, @Nullable String defaultText ) throws IOException, InterruptedException {
        Assert.notNull( getCliContext().getConsole(), "An interactive console is required to read the changelog entry." );
        String editorBin = getCliContext().getEnvironment().get( "EDITOR" );
        if ( editorBin == null ) {
            editorBin = "nano";
        }
        Path tempFile = Files.createTempFile( expressionExperiment.getShortName() + "-changelog-entry.md", null );
        if ( defaultText != null ) {
            PathUtils.writeString( tempFile, defaultText, StandardCharsets.UTF_8 );
        }
        int status = new ProcessBuilder( editorBin, tempFile.toString() )
                .inheritIO()
                .start()
                .waitFor();
        if ( status != 0 ) {
            throw new RuntimeException( "Editor returned non-zero exit code." );
        }
        String buf = PathUtils.readString( tempFile, StandardCharsets.UTF_8 );
        if ( StringUtils.isBlank( buf ) ) {
            throw new RuntimeException( "Changelog entry is empty for " + expressionExperiment + "." );
        }
        return buf;
    }
}
