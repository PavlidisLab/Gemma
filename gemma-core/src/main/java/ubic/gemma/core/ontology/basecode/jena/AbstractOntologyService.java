/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfxml.xmlinput.ARPErrorNumbers;
import org.apache.jena.rdfxml.xmlinput.ParseException;
import org.apache.jena.reasoner.ReasonerFactory;
import org.apache.jena.reasoner.rulesys.OWLFBRuleReasonerFactory;
import org.apache.jena.reasoner.rulesys.OWLMicroReasonerFactory;
import org.apache.jena.reasoner.rulesys.OWLMiniReasonerFactory;
import org.apache.jena.reasoner.transitiveReasoner.TransitiveReasonerFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.DC_11;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.ontology.basecode.model.OntologyIndividual;
import ubic.gemma.core.ontology.basecode.model.OntologyModel;
import ubic.gemma.core.ontology.basecode.model.OntologyResource;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.ontology.basecode.providers.OntologyService;
import ubic.gemma.core.ontology.basecode.search.OntologySearchException;
import ubic.gemma.core.ontology.basecode.search.OntologySearchResult;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ubic.gemma.core.ontology.basecode.jena.JenaUtils.as;

public abstract class AbstractOntologyService implements OntologyService {

    /**
     * Properties through which propagation is allowed for {@link #getParents(Collection, boolean, boolean)}}
     */
    private static final Set<Property> DEFAULT_ADDITIONAL_PROPERTIES;
    protected static Logger log = LoggerFactory.getLogger( AbstractOntologyService.class );

    static {
        DEFAULT_ADDITIONAL_PROPERTIES = new HashSet<>();
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.partOf );
        // all those are sub-properties of partOf, but some ontologies might not have them
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.activeIngredientIn );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.boundingLayerOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.branchingPartOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.determinedBy );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.ends );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.isSubsequenceOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.isEndSequenceOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.isStartSequenceOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.lumenOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.luminalSpaceOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.mainStemOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.memberOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.occurrentPartOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.skeletonOf );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.starts );
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.subclusterOf );
        // used by some older ontologies
        //noinspection deprecation
        DEFAULT_ADDITIONAL_PROPERTIES.add( RO.properPartOf );
    }

    private final String ontologyName;
    private final String ontologyUrl;
    private final boolean ontologyEnabled;
    private final String cacheName;

    /**
     * Internal state.
     */
    @Nullable
    private State state = null;
    /* settings (applicable for next initialization) */
    private LanguageLevel languageLevel = LanguageLevel.FULL;
    private InferenceMode inferenceMode = InferenceMode.TRANSITIVE;
    private boolean processImports = true;
    private boolean searchEnabled = true;
    private Set<String> excludedWordsFromStemming = Collections.emptySet();
    private Set<String> additionalPropertyUris = DEFAULT_ADDITIONAL_PROPERTIES.stream().map( Property::getURI ).collect( Collectors.toSet() );
    @Nullable
    private Set<String> allowedUriPrefixes = null;

    protected AbstractOntologyService( String ontologyName, String ontologyUrl, boolean ontologyEnabled, @Nullable String cacheName ) {
        this.ontologyName = ontologyName;
        this.ontologyUrl = ontologyUrl;
        this.ontologyEnabled = ontologyEnabled;
        this.cacheName = cacheName;
    }

    protected String getOntologyName() {
        return ontologyName;
    }

    protected String getOntologyUrl() {
        return ontologyUrl;
    }

    protected boolean isOntologyEnabled() {
        return ontologyEnabled;
    }

    @Nullable
    protected String getCacheName() {
        return cacheName;
    }

    private volatile Thread initializationThread = null;

    @Override
    public synchronized void startInitializationThread( boolean forceLoad, boolean forceIndexing ) {
        if ( initializationThread != null && initializationThread.isAlive() ) {
            log.warn( " Initialization thread for {} is currently running, not restarting.", this );
            return;
        }
        // create and start the initialization thread
        initializationThread = new Thread( () -> {
            try {
                this.initialize( forceLoad, forceIndexing );
            } catch ( Exception e ) {
                log.error( "Initialization of {} failed.", this, e );
            }
        }, getOntologyName() + "_load_thread_" + RandomStringUtils.insecure().nextAlphanumeric( 5 ) );
        // To prevent VM from waiting on this thread to shut down (if shutting down).
        initializationThread.setDaemon( true );
        initializationThread.start();
    }

    @Override
    public boolean isInitializationThreadAlive() {
        return initializationThread != null && initializationThread.isAlive();
    }

    @Override
    public boolean isInitializationThreadCancelled() {
        return initializationThread != null && initializationThread.isInterrupted();
    }

    /**
     * Cancel the initialization thread.
     */
    @Override
    public void cancelInitializationThread() {
        if ( initializationThread == null ) {
            throw new IllegalStateException( "The initialization thread has not started. Invoke startInitializationThread() first." );
        }
        initializationThread.interrupt();
    }

    @Override
    public void waitForInitializationThread() throws InterruptedException {
        if ( initializationThread == null ) {
            throw new IllegalStateException( "The initialization thread has not started. Invoke startInitializationThread() first." );
        }
        initializationThread.join();
    }

    @Override
    public void loadTermsInNameSpace( InputStream is, boolean forceIndex ) {
        // wait for the initialization thread to finish
        if ( initializationThread != null && initializationThread.isAlive() ) {
            log.warn( "{} initialization is already running, trying to cancel ...", this );
            initializationThread.interrupt();
            // wait for the thread to die.
            int maxWait = 10;
            int wait = 0;
            while ( initializationThread.isAlive() ) {
                try {
                    initializationThread.join( 5000 );
                } catch ( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                    return;
                }
                log.warn( "Waiting for auto-initialization to stop so manual initialization can begin ..." );
                ++wait;
                if ( wait >= maxWait && !initializationThread.isAlive() ) {
                    throw new RuntimeException( String.format( "Got tired of waiting for %s's initialization thread.", this ) );
                }
            }
        }
        initialize( is, forceIndex );
    }

    @Override
    public String getName() {
        return getState().map( state -> {
            NodeIterator it = state.model.listObjectsOfProperty( DC_11.title );
            return it.hasNext() ? it.next().asLiteral().getString() : null;
        } ).orElse( null );
    }

    @Override
    public String getDescription() {
        return getState().map( state -> {
            NodeIterator it = state.model.listObjectsOfProperty( DC_11.description );
            return it.hasNext() ? it.next().asLiteral().getString() : null;
        } ).orElse( null );
    }

    @Override
    public LanguageLevel getLanguageLevel() {
        return getState().map( state -> state.languageLevel ).orElse( languageLevel );
    }

    @Override
    public void setLanguageLevel( LanguageLevel languageLevel ) {
        this.languageLevel = languageLevel;
    }

    @Override
    public InferenceMode getInferenceMode() {
        return getState().map( state -> state.inferenceMode ).orElse( inferenceMode );
    }

    @Override
    public void setInferenceMode( InferenceMode inferenceMode ) {
        this.inferenceMode = inferenceMode;
    }

    @Override
    public boolean getProcessImports() {
        return getState().map( state -> state.processImports ).orElse( processImports );
    }

    @Override
    public void setProcessImports( boolean processImports ) {
        this.processImports = processImports;
    }

    @Override
    public void setAllowedUriPrefixes( String... uriPrefixes ) {
        if ( uriPrefixes.length == 0 ) {
            throw new IllegalArgumentException( "At least one URI prefix must be supplied." );
        }
        this.allowedUriPrefixes = new HashSet<>( Arrays.asList( uriPrefixes ) );
    }

    @Override
    public void clearAllowedUriPrefixes() {
        this.allowedUriPrefixes = null;
    }

    @Override
    public boolean isSearchEnabled() {
        return getState().map( state -> state.index != null ).orElse( searchEnabled );
    }

    @Override
    public void setSearchEnabled( boolean searchEnabled ) {
        this.searchEnabled = searchEnabled;
    }

    @Override
    public Set<String> getExcludedWordsFromStemming() {
        return getState().map( state -> state.excludedWordsFromStemming ).orElse( excludedWordsFromStemming );
    }

    @Override
    public void setExcludedWordsFromStemming( Set<String> excludedWordsFromStemming ) {
        this.excludedWordsFromStemming = excludedWordsFromStemming;
    }

    @Override
    public Set<String> getAdditionalPropertyUris() {
        return getState().map( state -> state.additionalPropertyUris ).orElse( additionalPropertyUris );
    }

    @Override
    public void setAdditionalPropertyUris( Set<String> additionalPropertyUris ) {
        this.additionalPropertyUris = additionalPropertyUris;
    }

    public void initialize( boolean forceLoad, boolean forceIndexing ) {
        initialize( null, forceLoad, forceIndexing );
    }

    public void initialize( InputStream stream, boolean forceIndexing ) {
        initialize( stream, true, forceIndexing );
    }

    private synchronized void initialize( @Nullable InputStream stream, boolean forceLoad, boolean forceIndexing ) {
        if ( !forceLoad && state != null ) {
            log.warn( "{} is already loaded, and force=false, not restarting", this );
            return;
        }

        // making a copy of all we need
        String ontologyUrl = getOntologyUrl();
        String ontologyName = getOntologyName();
        String cacheName = getCacheName();
        LanguageLevel languageLevel = this.languageLevel;
        InferenceMode inferenceMode = this.inferenceMode;
        boolean processImports = this.processImports;
        boolean searchEnabled = this.searchEnabled;
        Set<String> excludedWordsFromStemming = this.excludedWordsFromStemming;
        Set<String> allowedUriPrefixes = this.allowedUriPrefixes;

        // Detect configuration problems.
        if ( StringUtils.isBlank( ontologyUrl ) ) {
            throw new IllegalStateException( "URL not defined for %s: ontology cannot be loaded. (" + this + ")" );
        }

        if ( cacheName == null && forceIndexing ) {
            throw new IllegalArgumentException( String.format( "No cache directory is set for %s, cannot force indexing.", this ) );
        }

        boolean loadOntology = isEnabled();

        // If loading ontologies is disabled in the configuration, return
        if ( !forceLoad && !loadOntology ) {
            log.debug( "Loading {} is disabled (force=false, Configuration load.{}=false)",
                    this, ontologyName );
            return;
        }

        log.info( "Loading ontology: {}...", this );
        StopWatch loadTime = StopWatch.createStarted();

        // use temporary variables, so that we can minimize the critical region for replacing the service's state
        OntModel model;
        SearchIndex index;

        // loading the model from disk or URL is lengthy
        if ( Thread.currentThread().isInterrupted() )
            return;

        try {
            OntologyModel m = stream != null ? loadModelFromStream( stream, processImports, languageLevel, inferenceMode ) : loadModel( processImports, languageLevel, inferenceMode ); // can take a while.
            if ( m instanceof OntologyModelImpl ) {
                model = ( ( OntologyModelImpl ) m ).getOntModel();
            } else {
                throw new RuntimeException( "Only Jena-based ontology models are supported." );
            }
        } catch ( Exception e ) {
            if ( isCausedByInterrupt( e ) ) {
                // make sure that the thread is interrupted
                Thread.currentThread().interrupt();
                return;
            } else {
                throw new RuntimeException( String.format( "Failed to load ontology model for %s.", this ), e );
            }
        }

        // retrieving restrictions is lengthy
        if ( Thread.currentThread().isInterrupted() )
            return;

        // compute additional restrictions
        Set<Property> additionalProperties = additionalPropertyUris.stream().map( model::getProperty ).collect( Collectors.toSet() );
        Set<Restriction> additionalRestrictions = JenaUtils.listRestrictionsOnProperties( model, additionalProperties, true ).toSet();


        // indexing is lengthy, don't bother if we're interrupted
        if ( Thread.currentThread().isInterrupted() )
            return;

        if ( searchEnabled && cacheName != null ) {
            //Checks if the current ontology has changed since it was last loaded.
            boolean changed = OntologyLoader.hasChanged( cacheName );
            boolean indexExists = OntologyIndexer.getSubjectIndex( cacheName, excludedWordsFromStemming ) != null;
            boolean forceReindexing = forceLoad && forceIndexing;
            // indexing is slow, don't do it if we don't have to.
            try {
                index = OntologyIndexer.indexOntology( cacheName, model, excludedWordsFromStemming, forceReindexing || changed || !indexExists );
            } catch ( Exception e ) {
                if ( isCausedByInterrupt( e ) ) {
                    return;
                } else {
                    throw new RuntimeException( String.format( "Failed to generate index for %s.", this ), e );
                }
            }
        } else {
            index = null;
        }

        // if interrupted, we don't need to replace the model and clear the *old* cache
        if ( Thread.currentThread().isInterrupted() )
            return;

        if ( this.state != null ) {
            try {
                this.state.close();
            } catch ( Exception e ) {
                log.error( "Failed to close current state.", e );
            }
        }

        this.state = new State( model, index, excludedWordsFromStemming, additionalRestrictions, languageLevel,
            inferenceMode, processImports, additionalProperties.stream().map( Property::getURI ).collect( Collectors.toSet() ),
            allowedUriPrefixes, null );
        if ( cacheName != null ) {
            // now that the terms have been replaced, we can clear old caches
            try {
                OntologyLoader.deleteOldCache( cacheName );
            } catch ( IOException e ) {
                log.error( "Failed to delete old cache directory for {}.", this, e );
            }
        }

        loadTime.stop();

        log.info( "Finished loading {} in {}s", this, String.format( "%.2f", loadTime.getTime() / 1000.0 ) );
    }

    private boolean isCausedByInterrupt( Exception e ) {
        return hasCauseMatching( e, cause -> ( ( cause instanceof ParseException ) && ( ( ParseException ) cause ).getErrorNumber() == ARPErrorNumbers.ERR_INTERRUPTED ) ) ||
                hasCause( e, InterruptedException.class ) ||
                hasCause( e, InterruptedIOException.class ) ||
                hasCause( e, ClosedByInterruptException.class );
    }

    private boolean hasCause( Throwable t, Class<? extends Throwable> clazz ) {
        return hasCauseMatching( t, clazz::isInstance );
    }

    private boolean hasCauseMatching( Throwable t, Predicate<Throwable> predicate ) {
        return predicate.test( t ) || ( t.getCause() != null && hasCauseMatching( t.getCause(), predicate ) );
    }

    @Override
    public Collection<OntologySearchResult<OntologyIndividual>> findIndividuals( String search, int maxResults, boolean keepObsoletes ) throws
            OntologySearchException {
        State state = this.state;
        if ( state == null ) {
            log.warn( "Ontology {} is not ready, no individuals will be returned.", this );
            return Collections.emptySet();
        }
        if ( state.index == null ) {
            log.warn( "Attempt to search {} when index is null, no results will be returned.", this );
            return Collections.emptySet();
        }
        return state.index.searchIndividuals( state.model, search, maxResults ).stream()
            .filter( i -> state.isUriAllowed( i.result.getURI() ) )
                .map( i -> as( i.result, Individual.class ).map( r -> new OntologySearchResult<>( ( OntologyIndividual ) new OntologyIndividualImpl( r, state.additionalRestrictions ), i.score ) ) )
                .filter( Optional::isPresent )
                .map( Optional::get )
                .filter( ontologyTerm -> keepObsoletes || !ontologyTerm.getResult().isObsolete() )
                .sorted( Comparator.comparing( OntologySearchResult::getScore, Comparator.reverseOrder() ) )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    @Override
    public Collection<OntologySearchResult<OntologyResource>> findResources( String searchString, int maxResults, boolean keepObsoletes ) throws
            OntologySearchException {
        State state = this.state;
        if ( state == null ) {
            log.warn( "Ontology {} is not ready, no resources will be returned.", this );
            return Collections.emptySet();
        }
        if ( state.index == null ) {
            log.warn( "Attempt to search {} when index is null, no results will be returned.", this );
            return Collections.emptySet();
        }
        return state.index.search( state.model, searchString, maxResults ).stream()
            .filter( i -> state.isUriAllowed( i.result.getURI() ) )
                .filter( ( r -> r.result.canAs( OntClass.class ) || r.result.canAs( Individual.class ) ) )
                .map( r -> {
                    if ( r.result.canAs( OntClass.class ) ) {
                        return as( r.result, OntClass.class )
                                .map( r2 -> new OntologySearchResult<>( ( OntologyResource ) new OntologyTermImpl( r2, state.additionalRestrictions ), r.score ) );
                    } else if ( r.result.canAs( Individual.class ) ) {
                        return as( r.result, Individual.class )
                                .map( r2 -> new OntologySearchResult<>( ( OntologyResource ) new OntologyIndividualImpl( r2, state.additionalRestrictions ), r.score ) );
                    } else {
                        return Optional.<OntologySearchResult<OntologyResource>>empty();
                    }
                } )
                .filter( Optional::isPresent )
                .map( Optional::get )
                .filter( ontologyTerm -> keepObsoletes || !ontologyTerm.getResult().isObsolete() )
                .sorted( Comparator.comparing( OntologySearchResult::getScore, Comparator.reverseOrder() ) )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    @Override
    public Collection<OntologySearchResult<OntologyTerm>> findTerm( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException {
        State state = this.state;
        if ( state == null ) {
            log.warn( "Ontology {} is not ready, no terms will be returned.", this );
            return Collections.emptySet();
        }
        if ( state.index == null ) {
            log.warn( "Attempt to search {} when index is null, no results will be returned.", this );
            return Collections.emptySet();
        }
        return state.index.searchClasses( state.model, search, maxResults ).stream()
            .filter( i -> state.isUriAllowed( i.result.getURI() ) )
                .map( r -> as( r.result, OntClass.class ).map( s -> new OntologySearchResult<>( ( OntologyTerm ) new OntologyTermImpl( s, state.additionalRestrictions ), r.score ) ) )
                .filter( Optional::isPresent )
                .map( Optional::get )
                .filter( ontologyTerm -> keepObsoletes || !ontologyTerm.getResult().isObsolete() )
                .sorted( Comparator.comparing( OntologySearchResult::getScore, Comparator.reverseOrder() ) )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    @Override
    public OntologyTerm findUsingAlternativeId( String alternativeId ) {
        State state = this.state;
        if ( state == null ) {
            log.warn( "Ontology {} is not ready, null will be returned for alternative ID match.", this );
            return null;
        }
        if ( state.alternativeIDs == null ) {
            log.info( "init search by alternativeID" );
            this.state = initSearchByAlternativeId( state );
        }
        assert state.alternativeIDs != null;
        String termUri = state.alternativeIDs.get( alternativeId );
        return termUri != null ? getTerm( termUri ) : null;
    }

    @Override
    public Set<String> getAllURIs() {
        return getState().map( state -> {
            Set<String> allUris = new HashSet<>();
            allUris.addAll( state.model.listClasses().mapWith( OntClass::getURI ).toSet() );
            allUris.addAll( state.model.listIndividuals().mapWith( Individual::getURI ).toSet() );
            return allUris;
        } ).orElseGet( () -> {
            log.warn( "Ontology {} is not ready, no term  URIs will be returned.", this );
            return Collections.emptySet();
        } );
    }

    @Override
    public OntologyResource getResource( String uri ) {
        return getState().map( state -> {
            if ( !state.isUriAllowed( uri ) ) {
                return null;
            }
            OntologyResource res;
            Resource resource = state.model.getResource( uri );
            if ( resource.getURI() == null ) {
                return null;
            }
            if ( resource instanceof OntClass ) {
                // use the cached term
                res = new OntologyTermImpl( ( OntClass ) resource, state.additionalRestrictions );
            } else if ( resource instanceof Individual ) {
                res = new OntologyIndividualImpl( ( Individual ) resource, state.additionalRestrictions );
            } else if ( resource instanceof OntProperty ) {
                res = PropertyFactory.asProperty( ( ObjectProperty ) resource, state.additionalRestrictions );
            } else {
                res = null;
            }
            return res;
        } ).orElse( null );
    }

    @Override
    public OntologyTerm getTerm( String uri ) {
        return getState().map( state -> {
            if ( !state.isUriAllowed( uri ) ) {
                return null;
            }
            OntClass ontCls = state.model.getOntClass( uri );
            // null or bnode
            if ( ontCls == null || ontCls.getURI() == null ) {
                return null;
            }
            return new OntologyTermImpl( ontCls, state.additionalRestrictions );
        } ).orElse( null );
    }

    @Override
    public Collection<OntologyIndividual> getTermIndividuals( String uri ) {
        OntologyTerm term = getTerm( uri );
        if ( term == null ) {
            /*
             * Either the ontology hasn't been loaded, or the id was not valid.
             */
            log.warn( "No term for URI={} in {}; make sure ontology is loaded and uri is valid", uri, this );
            return Collections.emptySet();
        }
        return term.getIndividuals( true );
    }

    @Override
    public Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct,
            boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return getState().map( state ->
                        JenaUtils.getParents( state.model, getOntClassesFromTerms( state.model, terms ), direct, includeAdditionalProperties ? state.additionalRestrictions : null )
                                .stream()
                            .filter( o -> state.isUriAllowed( o.getURI() ) )
                                .map( o -> ( OntologyTerm ) new OntologyTermImpl( o, state.additionalRestrictions ) )
                                .filter( o -> keepObsoletes || !o.isObsolete() )
                                .collect( Collectors.toSet() ) )
                .orElse( Collections.emptySet() );
    }

    @Override
    public Set<OntologyTerm> getChildren( Collection<OntologyTerm> terms, boolean direct,
            boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return getState().map( state ->
                JenaUtils.getChildren( state.model, getOntClassesFromTerms( state.model, terms ), direct, includeAdditionalProperties ? state.additionalRestrictions : null )
                        .stream()
                    .filter( o -> state.isUriAllowed( o.getURI() ) )
                        .map( o -> ( OntologyTerm ) new OntologyTermImpl( o, state.additionalRestrictions ) )
                        .filter( o -> keepObsoletes || !o.isObsolete() )
                        .collect( Collectors.toSet() )
        ).orElse( Collections.emptySet() );
    }

    @Override
    public boolean isEnabled() {
        // could have forced, without setting config
        return isOntologyEnabled() || isOntologyLoaded() || isInitializationThreadAlive();
    }

    @Override
    public boolean isOntologyLoaded() {
        // it's fine not to use the read lock here
        return state != null;
    }

    /**
     * Delegates the call as to load the model into memory or leave it on disk. Simply delegates to either
     * OntologyLoader.loadMemoryModel( url ); OR OntologyLoader.loadPersistentModel( url, spec );
     */
    protected abstract OntologyModel loadModel( boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) throws IOException;

    /**
     * Load a model from a given input stream.
     */
    protected abstract OntologyModel loadModelFromStream( InputStream is, boolean processImports, LanguageLevel languageLevel, InferenceMode inferenceMode ) throws IOException;

    protected OntModelSpec getSpec( LanguageLevel languageLevel, InferenceMode inferenceMode ) {
        String profile;
        switch ( languageLevel ) {
            case FULL:
                profile = ProfileRegistry.OWL_LANG;
                break;
            case DL:
                profile = ProfileRegistry.OWL_DL_LANG;
                break;
            case LITE:
                profile = ProfileRegistry.OWL_LITE_LANG;
                break;
            default:
                throw new UnsupportedOperationException( String.format( "Unsupported OWL language level %s.", languageLevel ) );
        }
        ReasonerFactory reasonerFactory;
        switch ( inferenceMode ) {
            case FULL:
                reasonerFactory = OWLFBRuleReasonerFactory.theInstance();
                break;
            case MINI:
                reasonerFactory = OWLMiniReasonerFactory.theInstance();
                break;
            case MICRO:
                reasonerFactory = OWLMicroReasonerFactory.theInstance();
                break;
            case TRANSITIVE:
                reasonerFactory = TransitiveReasonerFactory.theInstance();
                break;
            case NONE:
                reasonerFactory = null;
                break;
            default:
                throw new UnsupportedOperationException( String.format( "Unsupported inference level %s.", inferenceMode ) );
        }
        return new OntModelSpec( ModelFactory.createMemModelMaker(), null, reasonerFactory, profile );
    }

    @Override
    public synchronized void index( boolean force ) {
        String cacheName = getCacheName();
        if ( cacheName == null ) {
            log.warn( "This ontology does not support indexing; assign a cache name to be used." );
            return;
        }
        if ( !searchEnabled ) {
            log.warn( "Search is not enabled for this ontology." );
            return;
        }
        State state = this.state;
        if ( state == null ) {
            log.warn( "Ontology {} is not initialized, cannot index it.", this );
            return;
        }
        SearchIndex index;
        try {
            index = OntologyIndexer.indexOntology( cacheName, state.model, state.excludedWordsFromStemming, force );
        } catch ( IOException e ) {
            log.error( "Failed to generate index for {}.", this, e );
            return;
        }
        // now we replace the index
        this.state = new State( state.model, index, state.excludedWordsFromStemming, state.additionalRestrictions, state.languageLevel, state.inferenceMode, state.processImports, state.additionalPropertyUris, state.allowedUriPrefixes, state.alternativeIDs );
    }

    /**
     * Initialize alternative IDs mapping.
     * <p>
     * this add alternative id in 2 ways
     * <p>
     * Example :
     * <p>
     * <a href="http://purl.obolibrary.org/obo/HP_0000005">HP_0000005</a> with alternative id : HP:0001453
     * <p>
     * by default way use in file 1- HP:0001453 -----> <a href="http://purl.obolibrary.org/obo/HP_0000005">HP_0000005</a>
     * <p>
     * trying <a href=" to use the value uri 2- http://purl.obol">HP_0001453</a>ibrary.org/obo/HP_0001453 ----->
     * <a href="http://purl.obolibrary.org/obo/HP_0000005">HP_0000005</a>
     */
    private State initSearchByAlternativeId( State state ) {
        Map<String, String> alternativeIDs = new HashMap<>();
        // for all Ontology terms that exist in the tree
        ExtendedIterator<OntClass> iterator = state.model.listClasses();
        while ( iterator.hasNext() ) {
            OntologyTerm ontologyTerm = new OntologyTermImpl( iterator.next(), state.additionalRestrictions );
            if ( ontologyTerm.getUri() == null ) {
                continue;
            }
            // let's find the baseUri, to change to valueUri
            String baseOntologyUri = ontologyTerm.getUri().substring( 0, ontologyTerm.getUri().lastIndexOf( "/" ) + 1 );
            for ( String alternativeId : ontologyTerm.getAlternativeIds() ) {
                // first way
                alternativeIDs.put( alternativeId, ontologyTerm.getUri() );
                // second way
                String alternativeIdModified = alternativeId.replace( ':', '_' );
                alternativeIDs.put( baseOntologyUri + alternativeIdModified, ontologyTerm.getUri() );
            }
        }
        return new State( state.model, state.index, state.excludedWordsFromStemming, state.additionalRestrictions, state.languageLevel, state.inferenceMode, state.processImports, state.additionalPropertyUris, state.allowedUriPrefixes, alternativeIDs );
    }

    @Override
    public void close() throws Exception {
        if ( state != null ) {
            state.close();
        }
    }

    @Override
    public String toString() {
        return String.format( "%s [url=%s] [allowed prefixes=%s] [language level=%s] [inference mode=%s] [imports=%b] [search=%b]",
            getOntologyName(), getOntologyUrl(), allowedUriPrefixes != null ? String.join( ",", allowedUriPrefixes ) : "*", getLanguageLevel(), getInferenceMode(), getProcessImports(), isSearchEnabled() );
    }

    private Optional<State> getState() {
        return Optional.ofNullable( state );
    }

    private Set<OntClass> getOntClassesFromTerms( OntModel model, Collection<OntologyTerm> terms ) {
        return terms.stream()
                .map( o -> {
                    if ( o instanceof OntologyTermImpl ) {
                        return ( ( OntologyTermImpl ) o ).getOntClass();
                    } else {
                        return o.getUri() != null ? model.getOntClass( o.getUri() ) : null;
                    }
                } )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
    }

    private static class State implements AutoCloseable {
        private final OntModel model;
        @Nullable
        private final SearchIndex index;
        private final Set<String> excludedWordsFromStemming;
        private final Set<Restriction> additionalRestrictions;
        private final LanguageLevel languageLevel;
        private final InferenceMode inferenceMode;
        private final boolean processImports;
        private final Set<String> additionalPropertyUris;
        @Nullable
        private final Set<String> allowedUriPrefixes;
        @Nullable
        private final Map<String, String> alternativeIDs;

        private State( OntModel model, @Nullable SearchIndex index, Set<String> excludedWordsFromStemming, Set<Restriction> additionalRestrictions, @Nullable LanguageLevel languageLevel, InferenceMode inferenceMode, boolean processImports, Set<String> additionalPropertyUris, @Nullable Set<String> allowedUriPrefixes, @Nullable Map<String, String> alternativeIDs ) {
            this.model = model;
            this.index = index;
            this.excludedWordsFromStemming = excludedWordsFromStemming;
            this.additionalRestrictions = additionalRestrictions;
            this.languageLevel = languageLevel;
            this.inferenceMode = inferenceMode;
            this.processImports = processImports;
            this.additionalPropertyUris = additionalPropertyUris;
            this.allowedUriPrefixes = allowedUriPrefixes;
            this.alternativeIDs = alternativeIDs;
        }

        /**
         * Check if this particular state allows a given URI from being returned by the service.
         */
        public boolean isUriAllowed( @Nullable String uri ) {
            if ( allowedUriPrefixes == null ) {
                return true;
            }
            return uri == null || allowedUriPrefixes.stream().anyMatch( uri::startsWith );
        }

        @Override
        public void close() throws Exception {
            try {
                model.close();
            } finally {
                if ( index != null ) {
                    index.close();
                }
            }
        }
    }
}
