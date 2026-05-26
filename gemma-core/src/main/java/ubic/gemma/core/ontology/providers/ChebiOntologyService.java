/*
 * The basecode project
 *
 * Copyright (c) 2007-2019 University of British Columbia
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

package ubic.gemma.core.ontology.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ubic.gemma.core.config.Configuration;
import ubic.gemma.core.ontology.jena.OntologyLoader;
import ubic.gemma.core.ontology.jena.RO;
import ubic.gemma.core.ontology.jena.UrlOntologyService;
import ubic.gemma.core.ontology.model.OntologyModel;
import ubic.gemma.core.ontology.providers.chebi.ChebiSeedResolver;
import ubic.gemma.core.ontology.providers.chebi.ChebiSlimExtractor;
import ubic.gemma.core.ontology.providers.chebi.ChebiSlimMeta;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <a href="https://obofoundry.org/ontology/chebi.html">Chemical Entities of Biological Interest</a>
 *
 * <p>CHEBI's chemistry-based {@code subClassOf} hierarchy ("estradiol is-a steroid is-a lipid")
 * isn't what we usually want for treatment classification — we care about pharmacological
 * <em>roles</em> ("estradiol has-role hormone", "sorafenib has-role kinase inhibitor"). CHEBI
 * captures those via the {@code RO:0000087 has_role} object property. Adding it to the
 * loader's additional-property set means {@code getChildren(roleClass, includeAdditionalProperties=true)}
 * walks both {@code subClassOf} (to subroles) AND the inverse of {@code has_role} (to chemicals
 * bearing the role), returning a unified set that callers can intersect with the corpus.
 *
 * <p>Extends {@link UrlOntologyService} directly (not via {@link AbstractDelegatingOntologyService})
 * so {@link #loadModel} can be overridden for the slim-CHEBI cache: a STAR-extracted subset
 * (~5-15 MB) covering the corpus seed plus role-closure ancestors, written to disk on first
 * boot and consumed directly on subsequent boots so warm time drops from minutes to seconds.
 * When {@link #slimExtractor} and {@link #seedResolver} are wired (production path) the slim
 * is built/consumed; when they're null (test contexts, dev without the slim cache dir
 * configured) the service falls back to {@link UrlOntologyService}'s full-URL load.
 *
 * @author klc
 */
public class ChebiOntologyService extends UrlOntologyService {

    private static final Logger log = LoggerFactory.getLogger( ChebiOntologyService.class );

    /**
     * Default freshness window for the slim cache. Beyond this age the slim is re-extracted
     * even when seed coverage hasn't grown. Aligned with the weekly slim cadence agreed in
     * the Phase 4 design discussion; a future {@code .meta.json}-based check (Phase 4c) will
     * narrow this so unchanged source + unchanged corpus stays fresh indefinitely.
     */
    private static final Duration DEFAULT_SLIM_MAX_AGE = Duration.ofDays( 7 );

    private static final String SLIM_FILE_NAME = "chebiOntology-slim.owl";
    private static final String SLIM_META_NAME = "chebiOntology-slim.meta.json";

    @Nullable
    private ChebiSlimExtractor slimExtractor;
    @Nullable
    private ChebiSeedResolver seedResolver;
    @Nullable
    private File slimCacheDir;
    private Duration slimMaxAge = DEFAULT_SLIM_MAX_AGE;

    public ChebiOntologyService() {
        super( "CHEBI",
            requireNonNull( Configuration.getString( "url.chebiOntology" ) ),
            Boolean.TRUE.equals( Configuration.getBoolean( "load.chebiOntology" ) ),
            "chebiOntology" );
        Set<String> props = new HashSet<>( getAdditionalPropertyUris() );
        props.add( RO.hasRole.getURI() );
        setAdditionalPropertyUris( props );
    }

    public void setSlimExtractor( @Nullable ChebiSlimExtractor slimExtractor ) {
        this.slimExtractor = slimExtractor;
    }

    public void setSeedResolver( @Nullable ChebiSeedResolver seedResolver ) {
        this.seedResolver = seedResolver;
    }

    /**
     * Directory under which the slim file and its sidecar meta land. Typically
     * {@code ${gemma.appdata.home}/ontologyCache/ontology}. Null disables the slim path
     * entirely (test fallback).
     */
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
        // Compute current seeds once: needed both for freshness check (compare to meta) and
        // for rebuild on a miss. If the resolver fails (e.g. DB not up yet), fall back to
        // the legacy full load.
        Set<String> currentSeeds = null;
        if ( seedResolver != null ) {
            try {
                currentSeeds = seedResolver.resolveCorpusSeeds();
            } catch ( Exception e ) {
                log.warn( "Seed resolver failed; this boot serves the full ontology.", e );
            }
        }

        if ( slim != null && slimMeta != null && currentSeeds != null
                && isSlimFresh( slim, slimMeta, currentSeeds ) ) {
            log.info( "Loading CHEBI from slim cache {} ({} bytes); skipping full source parse.",
                    slim, slim.length() );
            return loadFromFile( slim, processImports, languageLevel, inferenceMode );
        }

        if ( slim != null ) {
            log.info( "Slim CHEBI cache missing or stale at {}; loading full source.", slim );
        }
        OntologyModel full = super.loadModel( processImports, languageLevel, inferenceMode );

        if ( slim != null && slimMeta != null && slimExtractor != null && currentSeeds != null ) {
            try {
                rebuildSlim( slim, slimMeta, currentSeeds );
            } catch ( Exception e ) {
                // Don't fail the boot if slim extraction has a bad day — log and continue
                // serving the freshly-loaded full ontology. Next boot will retry.
                log.warn( "Slim CHEBI extraction failed; this boot serves the full ontology, "
                        + "next boot will re-attempt extraction.", e );
            }
        }
        return full;
    }

    private OntologyModel loadFromFile( File source, boolean processImports,
                                        LanguageLevel languageLevel, InferenceMode inferenceMode )
            throws IOException {
        try ( InputStream in = new FileInputStream( source ) ) {
            return loadModelFromStream( in, processImports, languageLevel, inferenceMode );
        }
    }

    /**
     * Where the slim file lives. Returns null if no cache dir is configured (slim path
     * disabled — fall back to full load).
     */
    @Nullable
    private File resolveSlimFile() {
        if ( slimCacheDir == null ) {
            return null;
        }
        return new File( slimCacheDir, SLIM_FILE_NAME );
    }

    @Nullable
    private File resolveSlimMetaFile() {
        if ( slimCacheDir == null ) {
            return null;
        }
        return new File( slimCacheDir, SLIM_META_NAME );
    }

    /**
     * Freshness check (Phase 4c):
     * <ul>
     *     <li>slim file exists and is non-empty;</li>
     *     <li>meta.json exists and parses;</li>
     *     <li>meta {@code seedHash} matches the hash of the current corpus seeds — so any
     *         curator-driven seed-set drift forces re-extraction;</li>
     *     <li>slim age is within {@link #slimMaxAge} — a belt-and-suspenders ceiling so a
     *         long-running container eventually picks up upstream source updates even if
     *         seeds haven't changed.</li>
     * </ul>
     */
    private boolean isSlimFresh( File slim, File meta, Set<String> currentSeeds ) {
        if ( !slim.isFile() || slim.length() == 0 ) {
            log.debug( "Slim freshness: file missing or empty at {}", slim );
            return false;
        }
        if ( !meta.isFile() ) {
            log.info( "Slim freshness: meta sidecar missing at {} — will rebuild.", meta );
            return false;
        }
        ChebiSlimMeta cached;
        try {
            cached = ChebiSlimMeta.readFrom( meta );
        } catch ( IOException e ) {
            log.warn( "Slim freshness: meta sidecar unreadable at {} — will rebuild.", meta, e );
            return false;
        }
        String currentHash = ChebiSlimMeta.hashSeeds( currentSeeds );
        if ( !currentHash.equals( cached.seedHash ) ) {
            log.info( "Slim freshness: corpus seed set drift ({} seeds in meta, {} now); "
                    + "will rebuild.", cached.seedCount, currentSeeds.size() );
            return false;
        }
        long ageMillis = System.currentTimeMillis() - slim.lastModified();
        if ( ageMillis >= slimMaxAge.toMillis() ) {
            log.info( "Slim freshness: slim is {} days old (max {} days); will rebuild to "
                    + "pick up any upstream source changes.",
                    ageMillis / 86_400_000L, slimMaxAge.toDays() );
            return false;
        }
        return true;
    }

    private void rebuildSlim( File slimOut, File metaOut, Set<String> seeds ) throws IOException {
        File source = OntologyLoader.getDiskCachePath( requireNonNull( getCacheName() ) );
        if ( !source.isFile() ) {
            log.warn( "Cannot extract slim: source CHEBI not on disk at {}. The upstream "
                    + "OntologyLoader should have cached it during super.loadModel(); skipping "
                    + "slim build for this boot.", source );
            return;
        }
        if ( seeds.isEmpty() ) {
            log.warn( "Skipping slim extraction: no CHEBI seeds in the corpus. The slim "
                    + "would collapse to an empty ontology; falling back to the full load." );
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
            throw new IOException( "ChebiSlimExtractor failed on " + source, e );
        }
        long elapsedMs = System.currentTimeMillis() - start;

        ChebiSlimMeta meta = ChebiSlimMeta.create(
                getOntologyUrl(), seeds, slimOut.length(),
                result.getClassCount(), result.getAxiomCount() );
        meta.writeTo( metaOut );

        log.info( "Slim CHEBI extracted in {} ms: {} (seeds covered: {} / {}). Meta sidecar "
                        + "written to {}. Subsequent boots will load the slim directly.",
                elapsedMs, result, result.getCoveredSeedUris().size(), seeds.size(), metaOut );
    }
}
