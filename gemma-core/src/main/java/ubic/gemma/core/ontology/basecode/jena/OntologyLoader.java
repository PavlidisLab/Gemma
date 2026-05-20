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
/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Reads ontologies from OWL resources
 *
 * @author paul
 */
class OntologyLoader {

    private static final Logger log = LoggerFactory.getLogger( OntologyLoader.class );

    private static final String OLD_CACHE_SUFFIX = ".old";
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
        URLConnection urlc = null;
        try {
            urlc = openConnection( url );
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

        if ( cacheName != null ) {
            File f = getDiskCachePath( cacheName );
            File tempFile = getTmpDiskCachePath( cacheName );
            File oldFile = getOldDiskCachePath( cacheName );
            if ( attemptToLoadFromDisk ) {
                // Attempt to load from disk cache
                if ( f.isFile() ) {
                    StopWatch timer = StopWatch.createStarted();
                    try ( BufferedReader buf = new BufferedReader( new FileReader( f ) ) ) {
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
        URLConnection urlc = openConnectionInternal( url );

        // this happens if there is a change of protocol (http:// -> https://)
        if ( urlc instanceof HttpURLConnection ) {
            int code = ( ( HttpURLConnection ) urlc ).getResponseCode();
            String newUrl = urlc.getHeaderField( "Location" );
            if ( code >= 300 && code < 400 ) {
                if ( StringUtils.isBlank( newUrl ) ) {
                    throw new RuntimeException( String.format( "Redirect response for %s is lacking a 'Location' header.", url ) );
                }
                log.debug( "Redirect to {} from {}", newUrl, url );
                urlc = openConnectionInternal( newUrl );
            }
        }

        return urlc;
    }

    private static URLConnection openConnectionInternal( String url ) throws IOException {
        URLConnection urlc = new URL( url ).openConnection();
        // help ensure mis-configured web servers aren't causing trouble.
        urlc.setRequestProperty( "Accept", "application/rdf+xml" );
        if ( urlc instanceof HttpURLConnection ) {
            ( ( HttpURLConnection ) urlc ).setInstanceFollowRedirects( true );
        }
        log.debug( "Connecting to {}", url );
        urlc.connect(); // Will error here on bad URL
        return urlc;
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

    static void deleteOldCache( String cacheName ) throws IOException {
        File dir = getOldDiskCachePath( cacheName );
        if ( dir.exists() ) {
            FileUtils.delete( dir );
        }
    }

    /**
     * Obtain the path for the ontology cache.
     */
    static File getDiskCachePath( String name ) {
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
