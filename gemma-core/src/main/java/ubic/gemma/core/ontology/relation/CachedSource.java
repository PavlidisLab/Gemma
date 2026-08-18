/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology.relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.config.Configuration;
import ubic.gemma.core.ontology.jena.OntologyLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Opens a third-party file a relation producer reads, from the shared ontology cache, downloading it
 * once if it is not there.
 *
 * <p>The same arrangement the lexical ontology services use — {@code url.<name>} for where it comes
 * from, {@link OntologyLoader#getDiskCachePath} for where it lands — extracted here because two
 * producers now need it and a second copy would be a second place for the cache path to be got wrong.
 * On a deployment where the webapp already holds the artifact, this reads the very same bytes rather
 * than fetching its own.</p>
 *
 * <p>🛑 No conditional GET and no expiry. These are release artifacts rather than live endpoints, and
 * the producer that reads one is a rebuild: re-fetching on a schedule would change what the table says
 * without anyone asking it to. To take a new release, delete the cached file.</p>
 */
class CachedSource {

    private static final Logger log = LoggerFactory.getLogger( CachedSource.class );

    /**
     * @param name both the {@code url.<name>} configuration key and the cache file name
     */
    static InputStream open( String name ) throws IOException {
        String url = Configuration.getString( "url." + name );
        if ( url == null ) {
            throw new IOException( "No url." + name + " configured." );
        }
        File cache = OntologyLoader.getDiskCachePath( name );
        if ( cache == null ) {
            log.info( "No ontology cache dir configured; streaming {} directly from {}.", name, url );
            return URI.create( url ).toURL().openStream();
        }
        if ( cache.isFile() && cache.length() > 0 ) {
            log.info( "Using cached {} at {} ({} bytes).", name, cache, cache.length() );
            return new FileInputStream( cache );
        }
        File parent = cache.getParentFile();
        if ( parent != null ) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        log.info( "Downloading {} from {} to {} ...", name, url, cache );
        try ( InputStream in = URI.create( url ).toURL().openStream() ) {
            Files.copy( in, cache.toPath(), StandardCopyOption.REPLACE_EXISTING );
        } catch ( FileSystemException e ) {
            // 🛑 The cache is somewhere this process may not write, and that is a normal way to run
            // rather than a broken one: gemma-cli mounts the ontology volume `:ro` on purpose, so a
            // producer whose artifact happens not to be seeded yet cannot cache it. On 2026-08-18
            // that turned into "MGI wrote 0 relation rows" -- diagnosable only from a stack trace,
            // and indistinguishable at the table from MGI having nothing to say.
            //
            // Not being able to KEEP the file is no reason not to READ it. Fall through to the same
            // direct stream used when no cache dir is configured at all; the cost is re-fetching on
            // the next run, which for a report measured in megabytes is the cheaper failure.
            //
            // Narrow on purpose: FileSystemException is the filesystem refusing (read-only mount,
            // permissions, quota). A download that fails on the network throws plain IOException and
            // still propagates, because retrying that as a stream would only fail again, less clearly.
            log.warn( "Could not cache {} at {} ({}); streaming it directly from {} instead."
                    + " It will be re-fetched on every run until that path is writable.",
                    name, cache, e.getMessage(), url );
            deletePartial( cache );
            return URI.create( url ).toURL().openStream();
        }
        return new FileInputStream( cache );
    }

    /**
     * A failed copy can leave a truncated file behind, and a truncated file is worse than none: the
     * length check above would accept it on the next run and hand a producer half a report.
     */
    private static void deletePartial( File cache ) {
        try {
            Files.deleteIfExists( cache.toPath() );
        } catch ( IOException suppressed ) {
            log.debug( "Could not remove the partial cache file at {}.", cache, suppressed );
        }
    }
}
