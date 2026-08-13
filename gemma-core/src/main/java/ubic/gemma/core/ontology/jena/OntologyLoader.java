/*
 * The baseCode project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.ontology.jena;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.CannotCreateException;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.graph.GraphReadOnly;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.config.Configuration;

import javax.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Reads ontologies from OWL resources
 *
 * @author paul
 */
public class OntologyLoader {

    private static final Logger log = LoggerFactory.getLogger( OntologyLoader.class );

    private static final String OLD_CACHE_SUFFIX = ".old";
    /** Sidecar recording which source URL the on-disk index was built from. @see #hasSourceChanged */
    private static final String SOURCE_MARKER_SUFFIX = ".source";
    /**
     * Sidecar holding the HTTP validator (ETag / Last-Modified) of the cached download, so a
     * re-init can ask the server "still this one?" instead of re-fetching. Deliberately NOT stored
     * in {@link #SOURCE_MARKER_SUFFIX}, whose entire contents are compared verbatim against the
     * configured URL by {@link #hasSourceChanged} — adding fields there would make every ontology
     * look like it changed source and reindex the lot.
     */
    private static final String VALIDATOR_MARKER_SUFFIX = ".validator";
    private static final String TMP_CACHE_SUFFIX = ".tmp";

    /**
     * Load an ontology model into memory from a stream.
     * <p>
     * Uses {@link OntModelSpec#OWL_MEM_TRANS_INF}.
     */
    static OntModel createMemoryModel( InputStream is, String name, boolean processImports, OntModelSpec spec ) throws JenaException {
        OntModel model = getModel( name, processImports, spec );
        model.read( is, name );
        return model;
    }

    /**
     * Load an ontology from a URL and store it in memory.
     * <p>
     * Use this type of model when fast access is critical and memory is available. If load from URL fails, attempt to
     * load from disk cache under @cacheName.
     *
     * @param url            a URL where the OWL file is stored
     * @param cacheName      unique name of this ontology, will be used to load from disk in case of failed url connection
     * @param processImports process imports
     * @param spec           spec to use as a basis
     */
    static OntModel createMemoryModel( String url, String name, @Nullable String cacheName, boolean processImports, OntModelSpec spec ) throws JenaException, IOException {
        StopWatch timer = StopWatch.createStarted();
        OntModel model = getModel( name, processImports, spec );
        readModelFromUrl( model, url, cacheName );
        log.debug( "Loading ontology model for {} took {} ms", url, timer.getTime() );
        return model;
    }

    /**
     * ModelFactory.createMemModelMaker()
     * Get model that is entirely in memory.
     */
    private static OntModel getModel( String name, boolean processImports, OntModelSpec spec ) {
        ModelMaker maker = ModelFactory.createMemModelMaker();
        Model base = maker.createModel( name, false );
        return getModel( maker, base, processImports, spec );
    }

    private static void readModelFromUrl( OntModel model, String url, @Nullable String cacheName ) throws IOException {
        boolean attemptToLoadFromDisk = false;
        // Set when the server answered 304: the cached copy is current, so there is nothing to
        // download and nothing to rotate. Distinct from attemptToLoadFromDisk, which means the
        // fetch FAILED and we are falling back.
        boolean cacheIsCurrent = false;
        // Captured inside the try but consumed after it, once the download is safely in place.
        String urlcEtag = null, urlcLastModified = null;
        URLConnection urlc = null;
        try {
            // Only send conditional headers when there is actually a cached body to fall back on;
            // a 304 with no cached file would leave us with nothing to read.
            Validator validator = null;
            if ( cacheName != null && getDiskCachePath( cacheName ).isFile() ) {
                validator = readValidator( cacheName );
                if ( validator == null ) {
                    // We have the bytes but no validator -- every deployment predating this, and
                    // the one that matters: frink holds a complete 826 MB chebiOntology right now.
                    // Downloading it again purely to learn its ETag would be absurd, so adopt the
                    // copy on disk if the server agrees about its size.
                    validator = adoptCachedCopy( url, cacheName );
                }
            }
            urlc = openConnection( url, validator );

            if ( validator != null && urlc instanceof HttpURLConnection
                    && ( ( HttpURLConnection ) urlc ).getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED ) {
                // Unchanged upstream. Read the copy we already have instead of pulling it again --
                // for CHEBI that is 826 MB and roughly half an hour saved per boot.
                cacheIsCurrent = true;
                File cached = getDiskCachePath( cacheName );
                StopWatch timer = StopWatch.createStarted();
                try ( InputStream is = Files.newInputStream( cached.toPath() ) ) {
                    model.read( is, url );
                }
                log.info( "{} is unchanged upstream (304); loaded the cached copy in {} ms instead of re-downloading.",
                        cacheName, timer.getTime() );
            } else {
                urlcEtag = urlc.getHeaderField( "ETag" );
                urlcLastModified = urlc.getHeaderField( "Last-Modified" );
                try ( InputStream in = urlc.getInputStream() ) {
                    if ( cacheName != null ) {
                        // write tmp to disk
                        File tempFile = getTmpDiskCachePath( cacheName );
                        FileUtils.createParentDirectories( tempFile );
                        Files.copy( in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
                        // read from disk
                        try ( InputStream is = Files.newInputStream( tempFile.toPath() ) ) {
                            model.read( is, url );
                        }
                    } else {
                        // skip the cache and simply read the stream into the model
                        model.read( in, url );
                    }
                }
            }
        } catch ( ClosedByInterruptException e ) {
            throw e;
        } catch ( IOException e ) {
            log.error( "Failed to load ontology model for {}, will attempt to load from disk.", url, e );
            attemptToLoadFromDisk = true;
        } finally {
            if ( urlc instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) urlc ).disconnect();
            }
        }

        if ( cacheName != null && cacheIsCurrent ) {
            // Nothing was downloaded, so leave the cache alone -- but mirror the disk-fallback path
            // and sync `.old` to the current file, otherwise hasChanged() compares this boot's file
            // against a stale predecessor and reports a change that did not happen, triggering a
            // needless reindex.
            File f = getDiskCachePath( cacheName );
            File oldFile = getOldDiskCachePath( cacheName );
            try {
                FileUtils.createParentDirectories( oldFile );
                Files.copy( f.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
            } catch ( IOException e ) {
                log.warn( "Could not refresh the previous-copy marker for {}.", cacheName, e );
            }
            return;
        }

        if ( cacheName != null ) {
            File f = getDiskCachePath( cacheName );
            File tempFile = getTmpDiskCachePath( cacheName );
            File oldFile = getOldDiskCachePath( cacheName );
            if ( attemptToLoadFromDisk ) {
                // Attempt to load from disk cache
                if ( f.isFile() ) {
                    StopWatch timer = StopWatch.createStarted();
                    try ( BufferedReader buf = Files.newBufferedReader( f.toPath(), StandardCharsets.UTF_8 ) ) {
                        model.read( buf, url );
                        // We successfully loaded the cached ontology. Copy the loaded ontology to oldFile
                        // so that we don't recreate indices during initialization based on a false change in
                        // the ontology.
                        FileUtils.createParentDirectories( oldFile );
                        Files.copy( f.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
                        log.debug( "Load model from disk took {} ms", timer.getTime() );
                    }
                } else {
                    throw new RuntimeException(
                        "Ontology failed load from URL (" + url + ") and disk cache does not exist: " + cacheName );
                }
            } else if ( tempFile.exists() ) {
                // Model was successfully loaded into memory from URL with given cacheName
                // Save cache to disk (rename temp file)
                log.debug( "Caching ontology to disk: {} under {}", cacheName, f.getAbsolutePath() );
                try {
                    // Need to compare previous to current so instead of overwriting we'll move the old file
                    if ( f.exists() ) {
                        FileUtils.createParentDirectories( oldFile );
                        Files.move( f.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING );
                    } else {
                        FileUtils.createParentDirectories( f );
                    }
                    Files.move( tempFile.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING );
                    // Record what we just stored so the next boot can ask "still this one?".
                    // Written only after the file is in place, so a validator never describes a
                    // copy that is not on disk.
                    if ( urlcEtag != null || urlcLastModified != null ) {
                        writeValidator( cacheName, urlcEtag, urlcLastModified );
                    }
                } catch ( IOException e ) {
                    log.error( "Failed to cache ontology {} to disk.", url, e );
                }
            }
        }
    }

    /**
     * Create an ontology model for a TDB.
     * @param dataset        TDB dataset
     * @param name           name of the model to load, or null for the default model
     * @param processImports whether to process imports or not, it is preferable not to if your TDB directory already
     *                       contains all the necessary definitions.
     * @param spec           spec to use to create the ontology model
     */
    public static OntModel createTdbModel( Dataset dataset, @Nullable String name, boolean processImports, OntModelSpec spec, boolean readOnly ) {
        ModelMaker maker = ModelFactory.createMemModelMaker();
        Model base;
        if ( name != null ) {
            base = dataset.getNamedModel( name );
        } else {
            base = dataset.getDefaultModel();
        }
        if ( base.isEmpty() ) {
            throw new IllegalStateException( String.format( "The %s at %s is empty.",
                name != null ? "named model " + name : "default model", dataset ) );
        }
        if ( readOnly ) {
            base = ModelFactory.createModelForGraph( new GraphReadOnly( base.getGraph() ) );
        }
        return getModel( maker, base, processImports, spec );
    }

    private static OntModel getModel( ModelMaker maker, Model base, boolean processImports, OntModelSpec spec ) {
        // the spec is a shallow copy, so we need to copy the document manager as well to modify it
        spec = new OntModelSpec( spec );
        spec.setImportModelMaker( maker );
        spec.setDocumentManager( new OntDocumentManager() );
        spec.getDocumentManager().setProcessImports( processImports );
        spec.setImportModelGetter( new ModelGetter() {
            @Override
            public Model getModel( String URL ) {
                return null;
            }

            @Override
            public Model getModel( String URL, ModelReader loadIfAbsent ) {
                Model model = maker.createModel( URL );
                URLConnection urlc = null;
                try {
                    urlc = openConnection( URL );
                    try ( InputStream in = urlc.getInputStream() ) {
                        return model.read( in, URL );
                    }
                } catch ( JenaException | IOException e ) {
                    throw new CannotCreateException( String.format( "Failed to resolve import for %s.", URL ), e );
                } finally {
                    if ( urlc instanceof HttpURLConnection ) {
                        ( ( HttpURLConnection ) urlc ).disconnect();
                    }
                }
            }
        } );
        OntModel model = ModelFactory.createOntologyModel( spec, base );
        model.setStrictMode( false ); // fix for owl2 files
        return model;
    }

    private static URLConnection openConnection( String url ) throws IOException {
        return openConnection( url, null );
    }

    /**
     * @param validator previously-recorded ETag / Last-Modified for the cached copy, or null to
     *                  fetch unconditionally. When supplied, the server may answer
     *                  {@code 304 Not Modified} and send no body at all.
     */
    private static URLConnection openConnection( String url, @Nullable Validator validator ) throws IOException {
        URLConnection urlc = openConnectionInternal( url, validator );

        // this happens if there is a change of protocol (http:// -> https://)
        if ( urlc instanceof HttpURLConnection ) {
            int code = ( ( HttpURLConnection ) urlc ).getResponseCode();
            String newUrl = urlc.getHeaderField( "Location" );
            if ( code >= 300 && code < 400 && code != HttpURLConnection.HTTP_NOT_MODIFIED ) {
                if ( StringUtils.isBlank( newUrl ) ) {
                    throw new RuntimeException( String.format( "Redirect response for %s is lacking a 'Location' header.", url ) );
                }
                log.debug( "Redirect to {} from {}", newUrl, url );
                // Re-apply the conditional headers: the redirect target is the origin that holds
                // the validator, so dropping them here would silently defeat the whole mechanism.
                urlc = openConnectionInternal( newUrl, validator );
            }
        }

        return urlc;
    }

    private static URLConnection openConnectionInternal( String url, @Nullable Validator validator ) throws IOException {
        URLConnection urlc = new URL( url ).openConnection();
        // help ensure mis-configured web servers aren't causing trouble.
        urlc.setRequestProperty( "Accept", "application/rdf+xml" );
        if ( validator != null ) {
            if ( StringUtils.isNotBlank( validator.etag ) ) {
                urlc.setRequestProperty( "If-None-Match", validator.etag );
            }
            if ( StringUtils.isNotBlank( validator.lastModified ) ) {
                urlc.setRequestProperty( "If-Modified-Since", validator.lastModified );
            }
        }
        if ( urlc instanceof HttpURLConnection ) {
            ( ( HttpURLConnection ) urlc ).setInstanceFollowRedirects( true );
        }
        log.debug( "Connecting to {}", url );
        urlc.connect(); // Will error here on bad URL
        return urlc;
    }

    /**
     * Bootstrap a validator for a cached file we already hold but never recorded one for.
     *
     * <p>Asks the server for the metadata only (HEAD) and compares {@code Content-Length} against
     * the file on disk. On a match we record the server's current ETag / Last-Modified as
     * describing our copy, which lets the very next request be conditional and answer 304 — so an
     * existing cache is adopted without transferring it.
     *
     * <p>Size equality is strong evidence rather than proof: a different release of the same
     * ontology at the same URL with a byte-identical length is conceivable. It is also
     * self-limiting — the guess only ever applies to the single boot that has a cache and no
     * validator, after which a real ETag is on file. A mismatched size, an absent
     * {@code Content-Length}, or a server that rejects HEAD all fall through to the normal
     * download.
     *
     * @return a validator describing the cached copy, or null to download as before
     */
    @Nullable
    private static Validator adoptCachedCopy( String url, String cacheName ) {
        File cached = getDiskCachePath( cacheName );
        long localSize = cached.length();
        if ( localSize <= 0 ) {
            return null;
        }
        HttpURLConnection headConnection = null;
        try {
            URLConnection urlc = openConnectionForHead( url );
            if ( !( urlc instanceof HttpURLConnection ) ) {
                return null;
            }
            headConnection = ( HttpURLConnection ) urlc;
            int code = headConnection.getResponseCode();
            if ( code != HttpURLConnection.HTTP_OK ) {
                log.debug( "HEAD for {} returned {}; will download instead of adopting the cache.", url, code );
                return null;
            }
            long remoteSize = headConnection.getHeaderFieldLong( "Content-Length", -1L );
            if ( remoteSize < 0 ) {
                return null;
            }
            if ( remoteSize != localSize ) {
                log.info( "Cached {} is {} bytes but upstream reports {}; re-downloading.",
                        cacheName, localSize, remoteSize );
                return null;
            }
            String etag = headConnection.getHeaderField( "ETag" );
            String lastModified = headConnection.getHeaderField( "Last-Modified" );
            Validator v = new Validator( etag, lastModified );
            if ( !v.isUsable() ) {
                // Same size but nothing to revalidate with next time; still worth skipping this
                // download, but there is no validator to persist.
                return null;
            }
            writeValidator( cacheName, etag, lastModified );
            log.info( "Adopted the existing {} cache ({} bytes, matching upstream) instead of re-downloading it.",
                    cacheName, localSize );
            return v;
        } catch ( IOException e ) {
            log.debug( "Could not check {} with HEAD; will download.", url, e );
            return null;
        } finally {
            if ( headConnection != null ) {
                headConnection.disconnect();
            }
        }
    }

    /** HEAD variant of {@link #openConnectionInternal}: metadata only, no body. */
    private static URLConnection openConnectionForHead( String url ) throws IOException {
        URLConnection urlc = new URL( url ).openConnection();
        urlc.setRequestProperty( "Accept", "application/rdf+xml" );
        if ( urlc instanceof HttpURLConnection ) {
            ( ( HttpURLConnection ) urlc ).setRequestMethod( "HEAD" );
            ( ( HttpURLConnection ) urlc ).setInstanceFollowRedirects( true );
        }
        urlc.connect();
        return urlc;
    }

    /** ETag / Last-Modified pair for a cached ontology download. */
    private static final class Validator {
        @Nullable
        private final String etag;
        @Nullable
        private final String lastModified;

        private Validator( @Nullable String etag, @Nullable String lastModified ) {
            this.etag = etag;
            this.lastModified = lastModified;
        }

        private boolean isUsable() {
            return StringUtils.isNotBlank( etag ) || StringUtils.isNotBlank( lastModified );
        }
    }

    /**
     * Read the recorded validator for a cached ontology, or null when there is none (every
     * deployment predating this, and any ontology whose server sends neither header).
     */
    @Nullable
    private static Validator readValidator( String cacheName ) {
        File marker = getValidatorMarkerPath( cacheName );
        if ( !marker.isFile() ) {
            return null;
        }
        try {
            String etag = null, lastModified = null;
            for ( String line : FileUtils.readLines( marker, StandardCharsets.UTF_8 ) ) {
                int eq = line.indexOf( '=' );
                if ( eq < 0 ) continue;
                String key = line.substring( 0, eq ).trim();
                String value = line.substring( eq + 1 ).trim();
                if ( "etag".equals( key ) ) etag = value;
                else if ( "lastModified".equals( key ) ) lastModified = value;
            }
            Validator v = new Validator( etag, lastModified );
            return v.isUsable() ? v : null;
        } catch ( IOException e ) {
            // Unreadable validator just costs a full download, which is the old behaviour.
            log.warn( "Could not read the cache validator for {}; will re-download.", cacheName, e );
            return null;
        }
    }

    /**
     * Record the validator of the copy now on disk. Swallowed on failure for the same reason as
     * {@link #recordSource}: losing the sidecar costs one extra download, failing the load costs
     * the ontology.
     */
    private static void writeValidator( String cacheName, @Nullable String etag, @Nullable String lastModified ) {
        if ( StringUtils.isBlank( etag ) && StringUtils.isBlank( lastModified ) ) {
            return;
        }
        File marker = getValidatorMarkerPath( cacheName );
        try {
            StringBuilder sb = new StringBuilder();
            if ( StringUtils.isNotBlank( etag ) ) sb.append( "etag=" ).append( etag.trim() ).append( '\n' );
            if ( StringUtils.isNotBlank( lastModified ) ) sb.append( "lastModified=" ).append( lastModified.trim() ).append( '\n' );
            FileUtils.forceMkdirParent( marker );
            FileUtils.writeStringToFile( marker, sb.toString(), StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            log.warn( "Could not record the cache validator for {}; the next load will re-download.", cacheName, e );
        }
    }

    static File getValidatorMarkerPath( String name ) {
        return new File( getDiskCachePath( name ).getAbsolutePath() + VALIDATOR_MARKER_SUFFIX );
    }

    static boolean hasChanged( String cacheName ) {
        // default
        if ( StringUtils.isBlank( cacheName ) ) {
            return false;
        }
        try {
            File newFile = getDiskCachePath( cacheName );
            File oldFile = getOldDiskCachePath( cacheName );
            // This might be slow considering it calls IOUtils.contentsEquals which compares byte-by-byte
            // in the worst case scenario.
            // In this case consider using NIO for higher-performance IO using Channels and Buffers.
            // Ex. Use a 4MB Memory-Mapped IO operation.
            return !FileUtils.contentEquals( newFile, oldFile );
        } catch ( IOException e ) {
            log.error( "Failed to compare current and previous cached ontologies, will report as not changed.", e );
            return false;
        }
    }

    /**
     * Whether the ontology's configured source differs from the one its on-disk index was built
     * from.
     *
     * <p>{@link #hasChanged(String)} compares the cached download to the previous copy of ITSELF,
     * which detects a new upstream release at the same URL. It cannot detect the URL being pointed
     * somewhere else, because the index is keyed by cacheName (the ontology) and survives the
     * swap intact. That gap shipped a live outage on 2026-08-09: CHEBI moved from
     * {@code chebi_lite.owl} to {@code chebi.owl} to recover synonyms, the Jena model picked them
     * up, and search kept answering from the synonym-less index across restarts — so
     * {@code /annotations/term} showed a term's synonyms while {@code /annotations/search} could
     * not find it by any of them, which reads like a ranking bug and is not one.
     *
     * <p>A missing marker reports NOT changed. Every existing deployment lacks one, and treating
     * absence as a mismatch would reindex every ontology on the next boot — an expensive answer to
     * a question we cannot actually answer for indexes built before this existed. The marker is
     * written after each successful index, so the protection starts one load later.
     *
     * @param cacheName the ontology's cache name; blank disables the check
     * @param url       the source URL currently configured
     */
    static boolean hasSourceChanged( String cacheName, @Nullable String url ) {
        if ( StringUtils.isBlank( cacheName ) || StringUtils.isBlank( url ) ) {
            return false;
        }
        File marker = getSourceMarkerPath( cacheName );
        if ( !marker.isFile() ) {
            return false;
        }
        try {
            String recorded = FileUtils.readFileToString( marker, StandardCharsets.UTF_8 ).trim();
            if ( recorded.isEmpty() || recorded.equals( url.trim() ) ) {
                return false;
            }
            log.info( "Source for {} changed from {} to {}; the existing index was built from the former and will be rebuilt.",
                    cacheName, recorded, url );
            return true;
        } catch ( IOException e ) {
            // Reporting "unchanged" here would silently reinstate the very bug this guards, so
            // prefer the expensive-but-correct answer.
            log.warn( "Could not read the source marker for {}; assuming the source changed and reindexing.", cacheName, e );
            return true;
        }
    }

    /**
     * Record the source URL an ontology's index was just built from, for {@link #hasSourceChanged}.
     * Failure is logged and swallowed: a missing marker costs a later reindex, whereas failing the
     * load would take the ontology down over bookkeeping.
     */
    static void recordSource( String cacheName, @Nullable String url ) {
        if ( StringUtils.isBlank( cacheName ) || StringUtils.isBlank( url ) ) {
            return;
        }
        File marker = getSourceMarkerPath( cacheName );
        try {
            FileUtils.forceMkdirParent( marker );
            FileUtils.writeStringToFile( marker, url.trim(), StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            log.warn( "Could not record the source marker for {}; a future source change may not trigger a reindex.", cacheName, e );
        }
    }

    static File getSourceMarkerPath( String name ) {
        return new File( getDiskCachePath( name ).getAbsolutePath() + SOURCE_MARKER_SUFFIX );
    }

    static void deleteOldCache( String cacheName ) throws IOException {
        File dir = getOldDiskCachePath( cacheName );
        if ( dir.exists() ) {
            FileUtils.delete( dir );
        }
    }

    /**
     * Obtain the on-disk path where the cached source OWL for an ontology lives.
     * <p>
     * Public so per-ontology customizations (e.g.
     * {@code ubic.gemma.core.ontology.providers.OntologySlimExtractor}) can read the cached
     * source without having to know the path computation; the path is a stable function of
     * {@code ontology.cache.dir} plus the cacheName.
     */
    public static File getDiskCachePath( String name ) {
        if ( StringUtils.isBlank( name ) ) {
            throw new IllegalArgumentException( "The ontology must have a suitable name for being loaded from cache." );
        }
        String ontologyDir = Configuration.getString( "ontology.cache.dir" ); // e.g., /something/gemmaData/ontologyCache
        if ( StringUtils.isBlank( ontologyDir ) ) {
            return Paths.get( System.getProperty( "java.io.tmpdir" ), "ontologyCache", "ontology", name ).toFile();
        }
        return Paths.get( ontologyDir, "ontology", name ).toFile();
    }

    static File getOldDiskCachePath( String name ) {
        File indexFile = getDiskCachePath( name );
        return new File( indexFile.getAbsolutePath() + OLD_CACHE_SUFFIX );
    }

    static File getTmpDiskCachePath( String name ) {
        File indexFile = getDiskCachePath( name );
        return new File( indexFile.getAbsolutePath() + TMP_CACHE_SUFFIX );
    }
}
