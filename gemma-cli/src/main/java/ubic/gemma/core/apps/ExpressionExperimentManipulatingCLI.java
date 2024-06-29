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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.IdentifiableUtils.toIdentifiableSet;

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
public abstract class ExpressionExperimentManipulatingCLI extends AbstractAuthenticatedCLI {

    @Autowired
    protected ExpressionExperimentService eeService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    protected AuditTrailService auditTrailService;
    @Autowired
    protected AuditEventService auditEventService;

    /**
     * Single-experiment mode.
     */
    private boolean singleExperimentMode = false;

    /**
     * Try to use references instead of actual entities.
     */
    private boolean useReferencesIfPossible = false;

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
     * Force processing of EEs regardless of their troubled status.
     */
    protected boolean force = false;

    protected ExpressionExperimentManipulatingCLI() {
        setRequireLogin( true );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        Option expOption = Option.builder( "e" ).hasArg().argName( "shortname" ).desc(
                        "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                                + "and if this option is omitted (and none other provided), the tool will be applied to all expression experiments." )
                .longOpt( "experiment" ).build();

        options.addOption( expOption );

        if ( singleExperimentMode )
            return;

        options.addOption( "all", false, "Process all expression experiments" );

        Option eeFileListOption = Option.builder( "f" ).hasArg().type( Path.class ).argName( "file" )
                .desc( "File with list of short names or IDs of expression experiments (one per line; use instead of '-e')" )
                .longOpt( "eeListfile" ).build();
        options.addOption( eeFileListOption );

        Option eeSetOption = Option.builder( "eeset" ).hasArg().argName( "eeSetName" )
                .desc( "Name of expression experiment set to use" ).build();
        options.addOption( eeSetOption );

        Option eeSearchOption = Option.builder( "q" ).hasArg().argName( "expressionQuery" )
                .desc( "Use a query string for defining which expression experiments to use" )
                .longOpt( "expressionQuery" ).build();
        options.addOption( eeSearchOption );

        Option taxonOption = Option.builder( "t" ).hasArg().argName( "taxon name" )
                .desc( "Taxon of the expression experiments and genes" ).longOpt( "taxon" )
                .build();
        options.addOption( taxonOption );

        Option excludeEeOption = Option.builder( "x" ).hasArg().type( Path.class ).argName( "file" )
                .desc( "File containing list of expression experiments to exclude" )
                .longOpt( "excludeEEFile" ).build();
        options.addOption( excludeEeOption );

        addBatchOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        Assert.isTrue( commandLine.hasOption( "all" ) || commandLine.hasOption( "eeset" )
                        || commandLine.hasOption( "e" ) || commandLine.hasOption( 'f' ) || commandLine.hasOption( 'q' ),
                "At least one of -all, -e, -eeset, -f, or -q must be provided." );
        this.force = commandLine.hasOption( "force" );
        this.all = commandLine.hasOption( "all" );
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
    }

    @Override
    protected void doWork() throws Exception {
        // intentionally a TreeSet over IDs, to prevent proxy initialization via hashCode()
        Set<BioAssaySet> expressionExperiments = new TreeSet<>( Comparator.comparing( BioAssaySet::getId ) );

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
            Taxon taxon = this.taxonName != null ? locateTaxon( this.taxonName ) : null;
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

        if ( !force && !expressionExperiments.isEmpty() ) {

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

        if ( expressionExperiments.isEmpty() ) {
            throw new RuntimeException( "No expression experiments matched the given options." );
        } else if ( expressionExperiments.size() == 1 ) {
            BioAssaySet ee = expressionExperiments.iterator().next();
            log.info( "Final dataset: " + experimentToString( ee ) );
        } else {
            log.info( String.format( "Final list: %d expression experiments", expressionExperiments.size() ) );
        }

        processBioAssaySets( expressionExperiments );
    }

    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        for ( BioAssaySet bas : expressionExperiments ) {
            try {
                processBioAssaySet( bas );
                addSuccessObject( bas );
            } catch ( Exception e ) {
                addErrorObject( bas, e );
            }
        }
    }

    /**
     * Process a BioAssaySet.
     * <p>
     * This method delegates to one of {@link #processExpressionExperiment(ExpressionExperiment)},
     * {@link #processExpressionExperimentSubSet(ExpressionExperimentSubSet)} or {@link #processOtherBioAssaySet(BioAssaySet)}.
     */
    protected void processBioAssaySet( BioAssaySet bas ) {
        Assert.notNull( bas, "Cannot process a null BioAssaySet." );
        if ( bas instanceof ExpressionExperiment ) {
            processExpressionExperiment( ( ExpressionExperiment ) bas );
        } else if ( bas instanceof ExpressionExperimentSubSet ) {
            processExpressionExperimentSubSet( ( ExpressionExperimentSubSet ) bas );
        } else {
            processOtherBioAssaySet( bas );
        }
    }

    /**
     * Process an {@link ExpressionExperiment}.
     */
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        throw new UnsupportedOperationException( "This command line does support experiments." );
    }

    /**
     * Process an {@link ExpressionExperimentSubSet}.
     */
    protected void processExpressionExperimentSubSet( ExpressionExperimentSubSet expressionExperimentSubSet ) {
        throw new UnsupportedOperationException( "This command line does support experiment subsets." );
    }

    /**
     * Process other kinds of {@link BioAssaySet} that are neither experiment nor subset.
     */
    protected void processOtherBioAssaySet( BioAssaySet bas ) {
        throw new UnsupportedOperationException( "This command line does support other kinds of BioAssaySet." );
    }

    protected void addForceOption( Options options ) {
        String desc = "Ignore other reasons for skipping experiments (e.g., trouble) and overwrite existing data (see documentation for this tool to see exact behavior if not clear)";
        Option forceOption = Option.builder( "force" ).longOpt( "force" ).desc( desc ).build();
        options.addOption( forceOption );
    }

    private void excludeFromFile( Set<BioAssaySet> expressionExperiments, Path excludeEeFileName ) throws IOException {
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
            ExpressionExperiment expressionExperiment = this.locateExpressionExperiment( identifier );
            if ( expressionExperiment == null ) {
                continue;
            }
            if ( !useReferencesIfPossible ) {
                expressionExperiment = eeService.thawLite( expressionExperiment );
            }
            ees.add( expressionExperiment );
        }
        return ees;
    }

    private Set<BioAssaySet> experimentsFromEeSet( String optionValue ) {
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
     * Attempt to locate an experiment using the given identifier.
     */
    @Nullable
    private ExpressionExperiment locateExpressionExperiment( String identifier ) {
        Assert.isTrue( StringUtils.isNotBlank( identifier ), "Expression experiment ID or short name must be provided" );
        identifier = StringUtils.strip( identifier );
        ExpressionExperiment ee;
        try {
            Long id = Long.parseLong( identifier );
            if ( useReferencesIfPossible ) {
                // this is never null, but may produce ObjectNotFoundException later on
                return eeService.loadReference( id );
            } else if ( ( ee = eeService.load( id ) ) != null ) {
                log.debug( "Found " + ee + " by ID" );
                return ee;
            } else {
                return null;
            }
        } catch ( NumberFormatException e ) {
            // can be safely ignored, we'll attempt to use it as a short name
        }
        if ( ( ee = eeService.findByShortName( identifier ) ) != null ) {
            log.debug( "Found " + ee + " by short name" );
            return ee;
        }
        if ( ( ee = eeService.findOneByAccession( identifier ) ) != null ) {
            log.debug( "Found " + ee + " by accession" );
            return ee;
        }
        if ( ( ee = eeService.findOneByName( identifier ) ) != null ) {
            log.debug( "Found " + ee + " by name" );
            return ee;
        }
        log.warn( "Could not locate any experiment with identifier or name " + identifier );
        return null;
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
            ExpressionExperiment ee = locateExpressionExperiment( id );
            if ( ee == null ) {
                continue;
            }
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
    private void removeTroubledExperiments( Collection<BioAssaySet> expressionExperiments ) {
        if ( expressionExperiments.isEmpty() ) {
            log.warn( "No experiments to remove troubled from" );
            return;
        }
        // it's not possible to check the curation details directly as that might trigger proxy initialization
        List<Long> troubledIds = eeService.loadIds( Filters.by( eeService.getFilter( "curationDetails.troubled", Boolean.class, Filter.Operator.eq, true ) ), null );

        // for subsets, check if the source experiment is troubled
        Set<BioAssaySet> troubledExpressionExperiments = expressionExperiments.stream()
                .filter( ee -> {
                    // for subsets, check source experiment troubled flag
                    if ( ee instanceof ExpressionExperimentSubSet ) {
                        return troubledIds.contains( ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment().getId() );
                    } else {
                        return troubledIds.contains( ee.getId() );
                    }
                } )
                .collect( toIdentifiableSet() );

        // only retain non-troubled experiments
        expressionExperiments.removeAll( troubledExpressionExperiments );

        if ( !troubledExpressionExperiments.isEmpty() ) {
            log.info( String.format( "Removed %s troubled experiments, leaving %d to be processed; use -force to include those.",
                    experimentsToString( troubledExpressionExperiments ), expressionExperiments.size() ) );
        }
    }

    /**
     * @param auditable  auditable
     * @param eventClass can be null
     * @return boolean
     */
    protected boolean noNeedToRun( Auditable auditable, @Nullable Class<? extends AuditEventType> eventClass ) {
        boolean needToRun = true;
        Date skipIfLastRunLaterThan = this.getLimitingDate();
        List<AuditEvent> events = this.auditEventService.getEvents( auditable );

        boolean okToRun = true; // assume okay unless indicated otherwise

        // figure out if we need to run it by date; or if there is no event of the given class; "Fail" type events don't
        // count.
        for ( int j = events.size() - 1; j >= 0; j-- ) {
            AuditEvent event = events.get( j );
            if ( event == null ) {
                continue; // legacy of ordered-list which could end up with gaps; should not be needed any more
            }
            AuditEventType eventType = event.getEventType();
            if ( eventType != null && eventClass != null && eventClass.isAssignableFrom( eventType.getClass() )
                    && !eventType.getClass().getSimpleName().startsWith( "Fail" ) ) {
                if ( skipIfLastRunLaterThan != null ) {
                    if ( event.getDate().after( skipIfLastRunLaterThan ) ) {
                        log.info( auditable + ": " + " run more recently than " + skipIfLastRunLaterThan );
                        addErrorObject( auditable, "Run more recently than " + skipIfLastRunLaterThan );
                        needToRun = false;
                    }
                } else {
                    needToRun = false; // it has been run already at some point
                }
            }
        }

        /*
         * Always skip if the object is curatable and troubled
         */
        if ( auditable instanceof Curatable ) {
            Curatable curatable = ( Curatable ) auditable;
            okToRun = !curatable.getCurationDetails().getTroubled(); //not ok if troubled

            // special case for expression experiments - check associated ADs.
            if ( okToRun && curatable instanceof ExpressionExperiment ) {
                for ( ArrayDesign ad : eeService.getArrayDesignsUsed( ( ExpressionExperiment ) auditable ) ) {
                    if ( ad.getCurationDetails().getTroubled() ) {
                        okToRun = false; // not ok if even one parent AD is troubled, no need to check the remaining ones.
                        break;
                    }
                }
            }

            if ( !okToRun ) {
                addErrorObject( auditable, "Has an active 'trouble' flag" );
            }
        }

        return !needToRun || !okToRun;
    }

    protected Taxon locateTaxon( String taxonName ) {
        Assert.isTrue( StringUtils.isNotBlank( taxonName ), "Taxon name must be be blank." );
        taxonName = StringUtils.strip( taxonName );
        Taxon taxon;
        try {
            long id = Long.parseLong( taxonName );
            if ( ( taxon = taxonService.load( id ) ) != null ) {
                log.info( "Found " + taxon + " by ID" );
                return taxon;
            }
            if ( ( taxon = taxonService.findByNcbiId( Math.toIntExact( id ) ) ) != null ) {
                log.info( "Found " + taxon + " by NCBI ID" );
                return taxon;
            }
            throw new NullPointerException( "No taxon with ID or NCBI ID " + id );
        } catch ( NumberFormatException e ) {
            // ignore
        }
        if ( ( taxon = taxonService.findByCommonName( taxonName ) ) != null ) {
            log.info( "Found " + taxon + " by common name." );
            return taxon;
        }
        if ( ( taxon = taxonService.findByScientificName( taxonName ) ) != null ) {
            log.info( "Found " + taxon + " by scientific name." );
            return taxon;
        }
        throw new NullPointerException( "Cannot find taxon with name " + taxonName );
    }

    protected ArrayDesign locateArrayDesign( String name ) {
        Assert.isTrue( StringUtils.isNotBlank( name ), "Platform name must not be blank." );
        name = StringUtils.strip( name );
        ArrayDesign arrayDesign;
        try {
            long id = Long.parseLong( name );
            if ( ( arrayDesign = arrayDesignService.load( id ) ) != null ) {
                log.info( "Found " + arrayDesign + " by ID." );
                return arrayDesign;
            }
            throw new NullPointerException( "No platform with ID " + id );
        } catch ( NumberFormatException e ) {
            // ignore
        }
        if ( ( arrayDesign = arrayDesignService.findByShortName( name ) ) != null ) {
            log.info( "Found " + arrayDesign + " by short name." );
            return arrayDesign;
        }
        if ( ( arrayDesign = arrayDesignService.findOneByName( name ) ) != null ) {
            log.info( "Found " + arrayDesign + " by name." );
            return arrayDesign;
        }
        if ( ( arrayDesign = arrayDesignService.findOneByAlternateName( name ) ) != null ) {
            log.info( "Found " + arrayDesign + " by alternate name." );
            return arrayDesign;
        }
        throw new NullPointerException( "No platform found with ID or name " + name );
    }

    /**
     * Refresh a dataset for Gemma Web.
     */
    protected void refreshExpressionExperimentFromGemmaWeb( ExpressionExperiment ee, boolean refreshVectors, boolean refreshReports ) throws Exception {
        StopWatch timer = StopWatch.createStarted();
        // using IDs here to prevent proxy initialization
        GemmaRestApiClient.Response response = getGemmaRestApiClient()
                .perform( "/datasets/" + ee.getId() + "/refresh",
                        "refreshVectors", refreshVectors,
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
        this.singleExperimentMode = true;
    }

    /**
     * Set this to true to allow reference to be retrieved instead of actual entities.
     * <p>
     * This only works for entities retrieved by ID.
     * <p>
     * When this is enabled, do not access anything but {@link ExpressionExperiment#getId()}, or else proxy-initialization
     * will be triggered, and you will have to deal with a {@link org.hibernate.LazyInitializationException}.
     * <p>
     * The default is false.
     */
    protected void setUseReferencesIfPossible() {
        this.useReferencesIfPossible = true;
    }

    /**
     * Render an experiment to string, with special handling in case of an uninitialized proxy.
     */
    private String experimentToString( BioAssaySet bas ) {
        if ( Hibernate.isInitialized( bas ) ) {
            return String.valueOf( bas );
        } else if ( bas instanceof ExpressionExperiment ) {
            return "ExpressionExperiment Id=" + bas.getId();
        } else if ( bas instanceof ExpressionExperimentSubSet ) {
            return "ExpressionExperimentSubSet Id=" + bas.getId();
        } else {
            return "BioAssaySet Id=" + bas.getId();
        }
    }

    private String experimentsToString( Collection<? extends BioAssaySet> bas ) {
        if ( bas.isEmpty() ) {
            return "no experiments";
        } else if ( bas.size() == 1 ) {
            return experimentToString( bas.iterator().next() );
        } else {
            return bas.size() + " experiments";
        }
    }
}
