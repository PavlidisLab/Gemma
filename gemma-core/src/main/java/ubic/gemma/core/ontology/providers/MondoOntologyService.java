package ubic.gemma.core.ontology.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.ontology.jena.OntologyLoader;
import ubic.gemma.core.ontology.jena.UrlOntologyService;
import ubic.gemma.core.ontology.model.OntologyModel;
import ubic.gemma.core.ontology.providers.chebi.ChebiSlimExtractor;
import ubic.gemma.core.ontology.providers.chebi.ChebiSlimMeta;
import ubic.gemma.core.ontology.providers.mondo.MondoSeedResolver;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * <a href="https://obofoundry.org/ontology/mondo.html">Mondo Disease Ontology</a>
 *
 * <p>MONDO is the disease-vocabulary integrator — ~25,000 classes with massive
 * cross-reference annotations to DOID, OMIM, NCIt, MESH, Orphanet, ICD, MedDRA, etc.
 * The full {@code mondo.owl} is ~231 MB and {@code mondo-base}/{@code mondo-simple}
 * only shave 10-15% because the bulk is MONDO's own xref + synonym + definition
 * annotations, not imported axioms.
 *
 * <p>Extends {@link UrlOntologyService} directly (not via {@code AbstractDelegatingOntologyService})
 * so {@link #loadModel} can be overridden for a corpus-tailored slim cache. Same
 * machinery as {@code ChebiOntologyService}: {@code ChebiSlimExtractor} (mis-named
 * — it's actually a generic OWL-API STAR extractor) runs against the cached source
 * with a seed of every MONDO URI from {@code Characteristic.valueUri}. Expected
 * reduction: ~10-20× because the corpus uses only a few thousand of the 25K MONDO
 * classes, and STAR drops the xref/synonym overhead on unreferenced terms.
 *
 * <p>Auto-rebuild on first boot is intentionally disabled — same OOM risk as CHEBI
 * before chebi_lite. Trigger via {@code POST /admin/ontologies/MONDO/rebuild-slim}
 * once the service is loaded and the host has memory headroom.
 *
 * @author poirigui
 */
public class MondoOntologyService extends UrlOntologyService implements SlimmableOntologyService {

    private static final Logger log = LoggerFactory.getLogger( MondoOntologyService.class );

    private static final Duration DEFAULT_SLIM_MAX_AGE = Duration.ofDays( 7 );
    private static final String SLIM_FILE_NAME = "mondoOntology-slim.owl";
    private static final String SLIM_META_NAME = "mondoOntology-slim.meta.json";

    @Nullable
    private ChebiSlimExtractor slimExtractor;
    @Nullable
    private MondoSeedResolver seedResolver;
    @Nullable
    private File slimCacheDir;
    private Duration slimMaxAge = DEFAULT_SLIM_MAX_AGE;
    private final AtomicReference<Thread> slimRebuildThread = new AtomicReference<>();

    public MondoOntologyService() {
        super( "Mondo Disease Ontology",
                Settings.getString( "url.mondoOntology" ),
                Boolean.TRUE.equals( Settings.getBoolean( "load.mondoOntology" ) ),
                "mondoOntology" );
    }

    public void setSlimExtractor( @Nullable ChebiSlimExtractor slimExtractor ) {
        this.slimExtractor = slimExtractor;
    }

    public void setSeedResolver( @Nullable MondoSeedResolver seedResolver ) {
        this.seedResolver = seedResolver;
    }

    public void setSlimCacheDir( @Nullable File slimCacheDir ) {
        this.slimCacheDir = slimCacheDir;
    }

    public void setSlimMaxAge( Duration slimMaxAge ) {
        this.slimMaxAge = requireNonNull( slimMaxAge );
    }

    @Override
    protected OntologyModel loadModel( boolean processImports, LanguageLevel languageLevel,
                                       InferenceMode inferenceMode ) throws IOException {
        File slim = resolveSlimFile();
        File slimMeta = resolveSlimMetaFile();
        Set<String> currentSeeds = null;
        if ( seedResolver != null ) {
            try {
                currentSeeds = seedResolver.resolveCorpusSeeds();
            } catch ( Exception e ) {
                log.warn( "MONDO seed resolver failed; this boot serves the full ontology.", e );
            }
        }

        if ( slim != null && slimMeta != null && currentSeeds != null
                && isSlimFresh( slim, slimMeta, currentSeeds ) ) {
            log.info( "Loading MONDO from slim cache {} ({} bytes); skipping full source parse.",
                    slim, slim.length() );
            return loadFromFile( slim, processImports, languageLevel, inferenceMode );
        }

        if ( slim != null ) {
            log.info( "Slim MONDO cache missing or stale at {}; loading full source. "
                    + "Rebuild via POST /admin/ontologies/MONDO/rebuild-slim once loaded.", slim );
        }
        return super.loadModel( processImports, languageLevel, inferenceMode );
    }

    @Override
    public boolean triggerSlimRebuildAsync() {
        if ( !isOntologyLoaded() ) {
            throw new IllegalStateException( "Cannot rebuild slim: MONDO is not loaded yet." );
        }
        if ( slimExtractor == null || seedResolver == null || slimCacheDir == null ) {
            throw new IllegalStateException( "Slim plumbing is not wired on this bean "
                    + "(extractor / resolver / cache dir absent). Check OntologyConfig." );
        }
        File slim = resolveSlimFile();
        File slimMeta = resolveSlimMetaFile();
        Thread existing = slimRebuildThread.get();
        if ( existing != null && existing.isAlive() ) {
            return false;
        }
        Thread t = new Thread( () -> {
            try {
                Set<String> seeds = seedResolver.resolveCorpusSeeds();
                rebuildSlim( slim, slimMeta, seeds );
            } catch ( Throwable e ) {
                log.warn( "Slim rebuild failed.", e );
            } finally {
                slimRebuildThread.compareAndSet( Thread.currentThread(), null );
            }
        }, "mondo-slim-rebuild" );
        t.setDaemon( true );
        if ( !slimRebuildThread.compareAndSet( null, t ) ) {
            return false;
        }
        t.start();
        return true;
    }

    @Override
    public boolean isSlimRebuildInFlight() {
        Thread t = slimRebuildThread.get();
        return t != null && t.isAlive();
    }

    private OntologyModel loadFromFile( File source, boolean processImports,
                                        LanguageLevel languageLevel, InferenceMode inferenceMode )
            throws IOException {
        try ( InputStream in = new FileInputStream( source ) ) {
            return loadModelFromStream( in, processImports, languageLevel, inferenceMode );
        }
    }

    @Nullable
    private File resolveSlimFile() {
        return slimCacheDir == null ? null : new File( slimCacheDir, SLIM_FILE_NAME );
    }

    @Nullable
    private File resolveSlimMetaFile() {
        return slimCacheDir == null ? null : new File( slimCacheDir, SLIM_META_NAME );
    }

    private boolean isSlimFresh( File slim, File meta, Set<String> currentSeeds ) {
        if ( !slim.isFile() || slim.length() == 0 ) return false;
        if ( !meta.isFile() ) {
            log.info( "Slim freshness: MONDO meta sidecar missing at {} — will rebuild.", meta );
            return false;
        }
        ChebiSlimMeta cached;
        try {
            cached = ChebiSlimMeta.readFrom( meta );
        } catch ( IOException e ) {
            log.warn( "Slim freshness: MONDO meta sidecar unreadable at {} — will rebuild.", meta, e );
            return false;
        }
        String currentHash = ChebiSlimMeta.hashSeeds( currentSeeds );
        if ( !currentHash.equals( cached.seedHash ) ) {
            log.info( "Slim freshness: MONDO corpus seed set drift ({} -> {}); will rebuild.",
                    cached.seedCount, currentSeeds.size() );
            return false;
        }
        long ageMillis = System.currentTimeMillis() - slim.lastModified();
        if ( ageMillis >= slimMaxAge.toMillis() ) {
            log.info( "Slim freshness: MONDO slim is {} days old (max {}); will rebuild.",
                    ageMillis / 86_400_000L, slimMaxAge.toDays() );
            return false;
        }
        return true;
    }

    private void rebuildSlim( File slimOut, File metaOut, Set<String> seeds ) throws IOException {
        File source = OntologyLoader.getDiskCachePath( requireNonNull( getCacheName() ) );
        if ( !source.isFile() ) {
            log.warn( "Cannot extract slim: source MONDO not on disk at {}. Skipping.", source );
            return;
        }
        if ( seeds.isEmpty() ) {
            log.warn( "Skipping MONDO slim extraction: no MONDO seeds in the corpus." );
            return;
        }
        File parent = slimOut.getParentFile();
        if ( parent != null && !parent.isDirectory() ) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        long start = System.currentTimeMillis();
        ChebiSlimExtractor.ExtractResult result;
        try {
            result = slimExtractor.extract( source, seeds, slimOut );
        } catch ( Exception e ) {
            throw new IOException( "OntologySlimExtractor failed on MONDO " + source, e );
        }
        long elapsedMs = System.currentTimeMillis() - start;

        ChebiSlimMeta meta = ChebiSlimMeta.create(
                getOntologyUrl(), seeds, slimOut.length(),
                result.getClassCount(), result.getAxiomCount() );
        meta.writeTo( metaOut );

        log.info( "Slim MONDO extracted in {} ms: {} (seeds covered: {} / {}). Meta sidecar "
                        + "written to {}. Subsequent boots will load the slim directly.",
                elapsedMs, result, result.getCoveredSeedUris().size(), seeds.size(), metaOut );
    }
}
