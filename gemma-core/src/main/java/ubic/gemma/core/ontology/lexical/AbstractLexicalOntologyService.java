package ubic.gemma.core.ontology.lexical;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.ontology.model.OntologyIndividual;
import ubic.gemma.core.ontology.model.OntologyResource;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.search.OntologySearchResult;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for flat lexical vocabularies served as an {@link OntologyService}.
 * <p>
 * Cellosaurus and MGI strains are not ontologies — they are large, flat, actively-maintained
 * catalogues of names + synonyms with no subsumption hierarchy. Rather than shoehorn them into
 * Jena's {@code OntModel} machinery (which exists to answer parent/child/inference queries they
 * don't have), this base owns a small Jena-agnostic {@link LexicalOntologyIndex} and implements the
 * {@code OntologyService} contract directly. It therefore plugs into the existing search fan-out,
 * the {@code OntologyCache}, and the {@code /admin/ontologies/{name}/refresh} lifecycle with no
 * changes to those layers, while paying only a Lucene index for the parsed {@code (uri, label,
 * synonyms)} terms — not a hierarchy the vocabulary lacks.
 * <p>
 * Subclasses supply the parsed term stream via {@link #parse(InputStream)} and (usually) a source to
 * {@link #openSource() open}. Hierarchy operations return empty; there is nothing to walk.
 */
public abstract class AbstractLexicalOntologyService implements OntologyService {

    protected final Logger log = LoggerFactory.getLogger( getClass() );

    private static final AtomicInteger LOAD_THREAD_COUNTER = new AtomicInteger();

    private final String name;
    private final String cacheName;
    @Nullable
    private final String url;
    private final boolean enabled;

    private boolean searchEnabled = true;
    private boolean processImports = false;
    private LanguageLevel languageLevel = LanguageLevel.FULL;
    private InferenceMode inferenceMode = InferenceMode.NONE;
    private Set<String> excludedWordsFromStemming = Collections.emptySet();
    private Set<String> additionalPropertyUris = Collections.emptySet();
    @Nullable
    private Set<String> allowedUriPrefixes = null;

    @Nullable
    protected volatile String version = null;

    /** Atomically-swapped loaded state; null until the vocabulary is loaded. */
    @Nullable
    private volatile State state = null;

    private volatile Thread initializationThread = null;

    protected AbstractLexicalOntologyService( String name, String cacheName, @Nullable String url, boolean enabled ) {
        this.name = name;
        this.cacheName = cacheName;
        this.url = url;
        this.enabled = enabled;
    }

    /**
     * Parse the vocabulary from a stream into its terms. Implementations may set {@link #version}.
     */
    protected abstract Collection<LexicalTerm> parse( InputStream is ) throws IOException;

    /**
     * Open the vocabulary source. The default opens {@link #getUrl()} directly; subclasses should
     * override to add disk caching so re-initialization does not re-download the source.
     *
     * @param forceReload if true, bypass any cached copy and re-fetch from the source (used by admin
     *                    refresh, which passes {@code forceLoad=true})
     */
    protected InputStream openSource( boolean forceReload ) throws IOException {
        if ( url == null ) {
            throw new IOException( "No source URL configured for " + name + "." );
        }
        return URI.create( url ).toURL().openStream();
    }

    protected String getCacheName() {
        return cacheName;
    }

    @Nullable
    protected String getUrl() {
        return url;
    }

    // ---------------------------------------------------------------------
    // Initialization / lifecycle
    // ---------------------------------------------------------------------

    @Override
    public synchronized void initialize( boolean forceLoad, boolean forceIndexing ) {
        if ( !enabled ) {
            log.debug( "{} is disabled; not loading.", name );
            return;
        }
        if ( state != null && !forceLoad ) {
            return;
        }
        try ( InputStream is = openSource( forceLoad ) ) {
            log.info( "Loading {} from {} ...", name, url );
            doInitialize( parse( is ) );
        } catch ( IOException e ) {
            log.error( "Failed to load {} from {}.", name, url, e );
        }
    }

    @Override
    public synchronized void initialize( InputStream stream, boolean forceIndexing ) {
        try {
            doInitialize( parse( stream ) );
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to load " + name + " from stream.", e );
        }
    }

    private void doInitialize( Collection<LexicalTerm> terms ) throws IOException {
        Map<String, String> uriToLabel = new HashMap<>();
        List<LexicalTerm> kept = new ArrayList<>();
        for ( LexicalTerm t : terms ) {
            if ( t.uri() == null || !isAllowed( t.uri() ) ) {
                continue;
            }
            uriToLabel.put( t.uri(), t.label() );
            kept.add( t );
        }
        LexicalOntologyIndex index = searchEnabled ? LexicalOntologyIndex.build( kept, excludedWordsFromStemming ) : null;
        State newState = new State( index, Collections.unmodifiableMap( uriToLabel ) );
        State old = this.state;
        this.state = newState;
        if ( old != null ) {
            old.close();
        }
        log.info( "{} loaded: {} terms{}.", name, uriToLabel.size(), index != null ? " (indexed)" : "" );
    }

    private boolean isAllowed( String uri ) {
        Set<String> prefixes = allowedUriPrefixes;
        if ( prefixes == null || prefixes.isEmpty() ) {
            return true;
        }
        for ( String p : prefixes ) {
            if ( uri.startsWith( p ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void index( boolean force ) {
        // The lexical index is rebuilt from the source as part of (re)initialization; there is no
        // separately-persisted index to refresh.
        initialize( true, force );
    }

    @Override
    public synchronized void startInitializationThread( boolean forceLoad, boolean forceIndexing ) {
        if ( initializationThread != null && initializationThread.isAlive() ) {
            log.warn( "Initialization thread for {} is currently running, not restarting.", name );
            return;
        }
        initializationThread = new Thread( () -> {
            try {
                initialize( forceLoad, forceIndexing );
            } catch ( Exception e ) {
                log.error( "Initialization of {} failed.", name, e );
            }
        }, name + "_load_thread_" + LOAD_THREAD_COUNTER.incrementAndGet() );
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

    // ---------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------

    @Override
    public Collection<OntologySearchResult<OntologyTerm>> findTerm( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException {
        List<OntologySearchResult<OntologyTerm>> out = new ArrayList<>();
        for ( LexicalOntologyIndex.Hit hit : doSearch( search, maxResults ) ) {
            out.add( new OntologySearchResult<>( toTerm( hit.uri() ), hit.score() ) );
        }
        return out;
    }

    @Override
    public Collection<OntologySearchResult<OntologyResource>> findResources( String search, int maxResults, boolean keepObsoletes ) throws OntologySearchException {
        List<OntologySearchResult<OntologyResource>> out = new ArrayList<>();
        for ( LexicalOntologyIndex.Hit hit : doSearch( search, maxResults ) ) {
            out.add( new OntologySearchResult<>( toTerm( hit.uri() ), hit.score() ) );
        }
        return out;
    }

    @Override
    public Collection<OntologySearchResult<OntologyIndividual>> findIndividuals( String search, int maxResults, boolean keepObsoletes ) {
        // A flat lexical vocabulary has no individuals.
        return Collections.emptyList();
    }

    private List<LexicalOntologyIndex.Hit> doSearch( String search, int maxResults ) throws OntologySearchException {
        State s = this.state;
        if ( s == null || s.index == null || search == null || search.trim().isEmpty() ) {
            return Collections.emptyList();
        }
        try {
            return s.index.search( search, maxResults );
        } catch ( IOException e ) {
            throw new OntologySearchException( "Lexical ontology query failure.", search, e );
        }
    }

    private OntologyTerm toTerm( String uri ) {
        State s = this.state;
        String label = s != null ? s.uriToLabel.get( uri ) : null;
        return new LexicalOntologyTerm( uri, label );
    }

    // ---------------------------------------------------------------------
    // Term lookup
    // ---------------------------------------------------------------------

    @Nullable
    @Override
    public OntologyTerm getTerm( String uri ) {
        State s = this.state;
        if ( s == null || !s.uriToLabel.containsKey( uri ) ) {
            return null;
        }
        return new LexicalOntologyTerm( uri, s.uriToLabel.get( uri ) );
    }

    @Nullable
    @Override
    public OntologyResource getResource( String uri ) {
        return getTerm( uri );
    }

    @Override
    public Set<String> getAllURIs() {
        State s = this.state;
        return s == null ? Collections.emptySet() : new LinkedHashSet<>( s.uriToLabel.keySet() );
    }

    @Override
    public Collection<OntologyIndividual> getTermIndividuals( String uri ) {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public OntologyTerm findUsingAlternativeId( String alternativeId ) {
        return null;
    }

    @Override
    public Set<OntologyTerm> getParents( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return Collections.emptySet();
    }

    @Override
    public Set<OntologyTerm> getChildren( Collection<OntologyTerm> terms, boolean direct, boolean includeAdditionalProperties, boolean keepObsoletes ) {
        return Collections.emptySet();
    }

    // ---------------------------------------------------------------------
    // Metadata / configuration
    // ---------------------------------------------------------------------

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isOntologyLoaded() {
        return state != null;
    }

    @Override
    public boolean getProcessImports() {
        return processImports;
    }

    @Override
    public void setProcessImports( boolean processImports ) {
        this.processImports = processImports;
    }

    @Override
    public void setAllowedUriPrefixes( String... uriPrefixes ) {
        this.allowedUriPrefixes = uriPrefixes.length == 0 ? null : new LinkedHashSet<>( List.of( uriPrefixes ) );
    }

    @Override
    public void clearAllowedUriPrefixes() {
        this.allowedUriPrefixes = null;
    }

    @Override
    public LanguageLevel getLanguageLevel() {
        return languageLevel;
    }

    @Override
    public void setLanguageLevel( LanguageLevel languageLevel ) {
        this.languageLevel = languageLevel;
    }

    @Override
    public InferenceMode getInferenceMode() {
        return inferenceMode;
    }

    @Override
    public void setInferenceMode( InferenceMode inferenceMode ) {
        this.inferenceMode = inferenceMode;
    }

    @Override
    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    @Override
    public void setSearchEnabled( boolean searchEnabled ) {
        this.searchEnabled = searchEnabled;
    }

    @Override
    public Set<String> getExcludedWordsFromStemming() {
        return excludedWordsFromStemming;
    }

    @Override
    public void setExcludedWordsFromStemming( Set<String> excludedWordsFromStemming ) {
        this.excludedWordsFromStemming = excludedWordsFromStemming != null ? excludedWordsFromStemming : Collections.emptySet();
    }

    @Override
    public Set<String> getAdditionalPropertyUris() {
        return additionalPropertyUris;
    }

    @Override
    public void setAdditionalPropertyUris( Set<String> additionalPropertyUris ) {
        this.additionalPropertyUris = additionalPropertyUris != null ? additionalPropertyUris : Collections.emptySet();
    }

    @Deprecated
    @Override
    public void loadTermsInNameSpace( InputStream is, boolean forceIndex ) {
        initialize( is, forceIndex );
    }

    @Override
    public void close() throws Exception {
        State s = this.state;
        this.state = null;
        if ( s != null ) {
            s.close();
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Immutable, atomically-swapped loaded state: the search index (nullable when search is disabled)
     * plus the URI → label map used for {@code getTerm}/{@code getAllURIs} and hit rehydration.
     */
    private static final class State {
        @Nullable
        private final LexicalOntologyIndex index;
        private final Map<String, String> uriToLabel;

        private State( @Nullable LexicalOntologyIndex index, Map<String, String> uriToLabel ) {
            this.index = index;
            this.uriToLabel = uriToLabel;
        }

        private void close() {
            if ( index != null ) {
                try {
                    index.close();
                } catch ( IOException e ) {
                    LoggerFactory.getLogger( AbstractLexicalOntologyService.class )
                            .warn( "Failed to close a lexical ontology index during state swap.", e );
                }
            }
        }
    }
}
