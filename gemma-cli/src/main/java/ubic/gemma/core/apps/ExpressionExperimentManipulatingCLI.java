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
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private GeneService geneService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    protected AuditTrailService auditTrailService;
    @Autowired
    protected AuditEventService auditEventService;

    // intentionally a TreeSet over IDs, to prevent proxy initialization via hashCode()
    protected final Set<BioAssaySet> expressionExperiments = new TreeSet<>( Comparator.comparing( BioAssaySet::getId ) );

    /**
     * Taxon used for filtering EEs.
     */
    private Taxon taxon = null;
    protected boolean force = false;
    private boolean useReferencesIfPossible = false;

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

        options.addOption( "all", false, "Process all expression experiments" );

        Option eeFileListOption = Option.builder( "f" ).hasArg().argName( "file" ).desc(
                        "File with list of short names or IDs of expression experiments (one per line; use instead of '-e')" )
                .longOpt( "eeListfile" ).build();
        options.addOption( eeFileListOption );

        Option eeSetOption = Option.builder( "eeset" ).hasArg().argName( "eeSetName" )
                .desc( "Name of expression experiment set to use" ).build();

        options.addOption( eeSetOption );

        Option taxonOption = Option.builder( "t" ).hasArg().argName( "taxon name" )
                .desc( "Taxon of the expression experiments and genes" ).longOpt( "taxon" )
                .build();
        options.addOption( taxonOption );

        Option excludeEeOption = Option.builder( "x" ).hasArg().argName( "file" )
                .desc( "File containing list of expression experiments to exclude" )
                .longOpt( "excludeEEFile" ).build();
        options.addOption( excludeEeOption );

        Option eeSearchOption = Option.builder( "q" ).hasArg().argName( "expressionQuery" )
                .desc( "Use a query string for defining which expression experiments to use" )
                .longOpt( "expressionQuery" ).build();
        options.addOption( eeSearchOption );

        addBatchOption( options );
    }

    @SuppressWarnings("unused") // Possible external use
    protected Gene findGeneByOfficialSymbol( String symbol, Taxon t ) {
        Collection<Gene> genes = geneService.findByOfficialSymbolInexact( symbol );
        for ( Gene gene : genes ) {
            if ( t.equals( gene.getTaxon() ) )
                return gene;
        }
        return null;
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( 't' ) ) {
            this.taxon = this.getTaxonByName( commandLine );
        }

        if ( commandLine.hasOption( "force" ) ) {
            this.force = true;
        }

        if ( commandLine.hasOption( "all" ) ) {
            if ( useReferencesIfPossible ) {
                log.info( "Loading all expression experiments, by reference..." );
                this.expressionExperiments.addAll( eeService.loadAllReferences() );
            } else {
                log.warn( "Loading all expression experiments, this might take a while..." );
                this.expressionExperiments.addAll( eeService.loadAll() );
            }
        } else if ( commandLine.hasOption( "eeset" ) ) {
            this.experimentsFromEeSet( commandLine.getOptionValue( "eeset" ) );
        } else if ( commandLine.hasOption( 'e' ) ) {
            this.experimentsFromCliList( commandLine );
        } else if ( commandLine.hasOption( 'f' ) ) {
            String experimentListFile = commandLine.getOptionValue( 'f' );
            AbstractCLI.log.info( "Reading experiment list from " + experimentListFile );
            try {
                this.expressionExperiments.addAll( this.readExpressionExperimentListFile( experimentListFile ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( commandLine.hasOption( 'q' ) ) {
            AbstractCLI.log.info( "Processing all experiments that match query " + commandLine.getOptionValue( 'q' ) );
            try {
                this.expressionExperiments.addAll( this.findExpressionExperimentsByQuery( commandLine.getOptionValue( 'q' ) ) );
            } catch ( SearchException e ) {
                log.error( "Failed to retrieve EEs for the passed query via -q.", e );
            }
        } else {
            throw new IllegalArgumentException( "At least one of -all, -e, -eeset, -f, or -q must be provided." );
        }

        if ( commandLine.hasOption( 'x' ) && !expressionExperiments.isEmpty() ) {
            this.excludeFromFile( commandLine );
        }

        if ( !force && !expressionExperiments.isEmpty() ) {

            if ( isAutoSeek() ) {
                if ( this.getAutoSeekEventType() == null ) {
                    throw new IllegalStateException( "Programming error: there is no 'autoSeekEventType' set" );
                }
                AbstractCLI.log.info( "Filtering for experiments lacking a " + this.getAutoSeekEventType().getSimpleName()
                        + " event" );
                auditEventService.retainLackingEvent( this.expressionExperiments, this.getAutoSeekEventType() );
            }

            Set<ExpressionExperiment> troubledExpressionExperiments = this.getTroubledExpressionExperiments();

            // only retain non-troubled experiments
            expressionExperiments.removeAll( troubledExpressionExperiments );

            if ( troubledExpressionExperiments.size() == 1 ) {
                AbstractCLI.log.info( troubledExpressionExperiments.stream().findFirst().get().getName() + " has an active trouble flag" );
            } else if ( troubledExpressionExperiments.size() > 1 ) {
                AbstractCLI.log.info( "Removed " + troubledExpressionExperiments.size() + " experiments with 'trouble' flags, leaving "
                        + expressionExperiments.size() );
            }
        }

        if ( expressionExperiments.isEmpty() ) {
            log.warn( "No expression experiments matched the given options." );
        } else if ( expressionExperiments.size() == 1 ) {
            AbstractCLI.log.info( "Final dataset: " + expressionExperiments.iterator().next() );
        } else {
            AbstractCLI.log.info( String.format( "Final list: %d expression experiments", this.expressionExperiments.size() ) );
        }
    }

    void addForceOption( Options options ) {
        String desc = "Ignore other reasons for skipping experiments (e.g., trouble) and overwrite existing data (see documentation for this tool to see exact behavior if not clear)";
        Option forceOption = Option.builder( "force" ).longOpt( "force" ).desc( desc ).build();
        options.addOption( forceOption );
    }

    private void excludeFromFile( CommandLine commandLine ) {
        String excludeEeFileName = commandLine.getOptionValue( 'x' );
        Collection<ExpressionExperiment> excludeExperiments;
        try {
            excludeExperiments = this.readExpressionExperimentListFile( excludeEeFileName );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        assert !expressionExperiments.isEmpty();

        int before = expressionExperiments.size();

        expressionExperiments.removeAll( excludeExperiments );
        int removed = before - expressionExperiments.size();

        if ( removed > 0 )
            AbstractCLI.log.info( "Excluded " + removed + " expression experiments" );
    }

    private void experimentsFromCliList( CommandLine commandLine ) {
        String[] identifiers = commandLine.getOptionValue( 'e' ).split( "," );
        for ( String identifier : identifiers ) {
            ExpressionExperiment expressionExperiment = this.locateExpressionExperiment( identifier );
            if ( expressionExperiment == null ) {
                log.warn( "No experiment " + identifier + " found either by ID or short name." );
                continue;
            }
            expressionExperiments.add( eeService.thawLite( expressionExperiment ) );
        }
        if ( expressionExperiments.isEmpty() ) {
            throw new RuntimeException( "There were no valid experiments specified." );
        }
    }

    private void experimentsFromEeSet( String optionValue ) {

        if ( StringUtils.isBlank( optionValue ) ) {
            throw new IllegalArgumentException( "Please provide an eeset name" );
        }

        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService.findByName( optionValue );
        if ( sets.size() > 1 ) {
            throw new IllegalArgumentException( "More than on EE set has name '" + optionValue + "'" );
        } else if ( sets.isEmpty() ) {
            throw new IllegalArgumentException( "No EE set has name '" + optionValue + "'" );
        }
        ExpressionExperimentSet set = sets.iterator().next();
        this.expressionExperiments.addAll( set.getExperiments() );

    }

    /**
     * Use the search engine to locate expression experiments.
     */
    private Set<ExpressionExperiment> findExpressionExperimentsByQuery( String query ) throws SearchException {
        Set<ExpressionExperiment> ees = new HashSet<>();

        // explicitly support one case
        if ( query.matches( "GPL[0-9]+" ) ) {
            ArrayDesign ad = arrayDesignService.findByShortName( query );
            if ( ad != null ) {
                Collection<ExpressionExperiment> ees2 = arrayDesignService.getExpressionExperiments( ad );
                ees.addAll( ees2 );
                log.info( ees.size() + " experiments matched to platform " + ad );
            }
            return ees;
        }

        Collection<SearchResult<ExpressionExperiment>> eeSearchResults = searchService
                .search( SearchSettings.expressionExperimentSearch( query ) )
                .getByResultObjectType( ExpressionExperiment.class );

        // Filter out all the ee that are not of correct taxon
        for ( SearchResult<ExpressionExperiment> sr : eeSearchResults ) {
            ExpressionExperiment ee = sr.getResultObject();
            if ( ee == null )
                continue; // ee no longer valid, could be an outdated compass hit
            Taxon t = eeService.getTaxon( ee );
            if ( t != null && t.getCommonName().equalsIgnoreCase( taxon.getCommonName() ) ) {
                ees.add( ee );
            }
        }

        AbstractCLI.log.info( ees.size() + " Expression experiments matched '" + query + "'" );

        return ees;

    }

    /**
     * Attempt to locate an experiment using the given identifier.
     */
    @Nullable
    private ExpressionExperiment locateExpressionExperiment( String identifier ) {
        if ( identifier == null ) {
            addErrorObject( null, "Expression experiment ID or short name must be provided" );
            return null;
        }

        ExpressionExperiment ee;
        try {
            Long id = Long.parseLong( identifier );
            if ( useReferencesIfPossible ) {
                ee = eeService.loadReference( id );
            } else {
                ee = eeService.load( id );
            }
        } catch ( NumberFormatException e ) {
            // can be safely ignored, we'll attempt to use it as a short name
            ee = null;
        }

        if ( ee == null ) {
            ee = eeService.findByShortName( identifier );
        }

        return ee;
    }

    /**
     * Load expression experiments based on a list of short names or IDs in a file. Only the first column of the file is
     * used, comments (#) are allowed.
     */
    private Collection<ExpressionExperiment> readExpressionExperimentListFile( String fileName ) throws IOException {
        List<String> idlist = FileUtils.readListFileToStrings( fileName );
        List<ExpressionExperiment> ees = new ArrayList<>( idlist.size() );
        log.info( "Found " + idlist.size() + " experiment identifiers in file " + fileName );
        int count = 0;
        for ( String eeName : idlist ) {
            ExpressionExperiment ee = locateExpressionExperiment( eeName );
            if ( ee == null ) {
                log.warn( "No experiment " + eeName + " found either by ID or short name." );
                continue;
            }
            count++;
            ees.add( ee );
            if ( idlist.size() > 500 && count > 0 && count % 500 == 0 ) {
                AbstractCLI.log.info( "Loaded " + count + " experiments ..." );
            }
        }
        log.info( "Loaded " + ees.size() + " experiments for processing" );
        return ees;
    }

    /**
     * Obtain EEs that are troubled among {@link ExpressionExperimentManipulatingCLI#expressionExperiments}.
     *
     * @return a collection of troubled experiemnt, or an empty set of non are
     */
    private Set<ExpressionExperiment> getTroubledExpressionExperiments() {
        if ( expressionExperiments.isEmpty() ) {
            AbstractCLI.log.warn( "No experiments to remove troubled from" );
            return Collections.emptySet();
        }
        // it's not possible to check the curation details directly as that might trigger proxy initialization
        List<Long> troubledIds = eeService.loadIds( Filters.by( eeService.getFilter( "curationDetails.troubled", Boolean.class, Filter.Operator.eq, true ) ), null );
        return expressionExperiments.stream()
                .map( ee -> {
                    // for subsets, check source experiment troubled flag
                    if ( ee instanceof ExpressionExperimentSubSet ) {
                        return ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment();
                    } else {
                        return ( ExpressionExperiment ) ee;
                    }
                } )
                .filter( ee -> troubledIds.contains( ee.getId() ) )
                .collect( Collectors.toCollection( () -> new TreeSet<>( Comparator.comparing( ExpressionExperiment::getId ) ) ) );
    }

    /**
     * @param auditable  auditable
     * @param eventClass can be null
     * @return boolean
     */
    protected boolean noNeedToRun( Auditable auditable, Class<? extends AuditEventType> eventClass ) {
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
                        AbstractCLI.log.info( auditable + ": " + " run more recently than " + skipIfLastRunLaterThan );
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

    protected Taxon getTaxonByName( CommandLine commandLine ) {
        String taxonName = commandLine.getOptionValue( 't' );
        ubic.gemma.model.genome.Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            AbstractCLI.log.error( "ERROR: Cannot find taxon " + taxonName );
        }
        return taxon;
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
     * Set this to true to allow reference to be retrieved instead of actual entities.
     * <p>
     * This only works for entities retrieved by ID.
     * <p>
     * When this is enabled, do not access anything but {@link ExpressionExperiment#getId()}, or else proxy-initialization
     * will be triggered, and you will have to deal with a {@link org.hibernate.LazyInitializationException}.
     * <p>
     * The default is false.
     */
    protected void setUseReferencesIfPossible( boolean useReferencesIfPossible ) {
        this.useReferencesIfPossible = useReferencesIfPossible;
    }
}
