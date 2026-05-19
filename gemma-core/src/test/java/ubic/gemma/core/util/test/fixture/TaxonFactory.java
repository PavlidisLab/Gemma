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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ubic.gemma.core.util.test.fixture;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;

/**
 * Typed factory for persistent {@link Taxon} test fixtures.
 * <p>
 * This is the Phase 3 replacement for
 * {@code PersistentDummyObjectHelper.getTestPersistentTaxon()} and the
 * private {@code getTestNonPersistentTaxon()} helper. It exists primarily
 * to make HB6-safe "resolve existing seed, don't fabricate transient"
 * the easy default — the old pattern of {@code Taxon.Factory.newInstance()}
 * + {@code setCommonName("mouse")} and hoping the persister deduplicates
 * worked under HB5 but broke under HB6.
 * <p>
 * Usage:
 * <pre>
 *   Taxon mouse = taxonFactory.mouse();            // seeded taxon (NCBI 10090)
 *   Taxon human = taxonFactory.human();            // seeded taxon (NCBI 9606)
 *   Taxon rat   = taxonFactory.byCommonName("rat").build();
 *   Taxon adhoc = taxonFactory.adHoc().build();    // freshly-created (admin-only)
 *   Taxon adhoc = taxonFactory.adHoc()
 *                    .withNcbiId(99999)
 *                    .withScientificName("Testus testus")
 *                    .withCommonName("testbeast")
 *                    .build();
 * </pre>
 * <p>
 * Design notes:
 * <ul>
 *   <li>The seeded-taxon shortcuts ({@link #mouse()}, {@link #human()},
 *       {@link #rat()}, {@link #zebrafish()}, {@link #fly()}, {@link #worm()},
 *       {@link #yeast()}) resolve straight to the seeded row from
 *       {@code init-data.sql}. They throw if the seed isn't there.</li>
 *   <li>{@link #byCommonName(String)} / {@link #byNcbiId(Integer)} / {@link
 *       #byScientificName(String)} are the same idea, parameterized.</li>
 *   <li>{@link #adHoc()} is the only path that ever creates a new {@link
 *       Taxon} row. It is intended for tests that need to verify behaviour
 *       on a taxon distinct from the seeded set (e.g. yeast-second-NCBI
 *       cases, taxon admin tests). Generates a randomized NCBI id (50000+
 *       to stay clear of any real assignment) and a random scientific +
 *       common name. The factory persists through {@link
 *       TaxonService#findOrCreate(Taxon)} so the path mirrors the
 *       production admin flow.</li>
 *   <li>Default {@code isGenesUsable=true} for ad-hoc taxa — this is what
 *       almost every test wants. Use {@link AdHocBuilder#withGenesUsable(boolean)}
 *       to override.</li>
 * </ul>
 *
 * @author Phase 3 (Vision section 3 - test-fixture rewrite)
 */
@Component
public class TaxonFactory {

    private static final int RANDOM_STRING_LENGTH = 8;
    /** Floor for ad-hoc NCBI ids; well above any real assignment. */
    private static final int AD_HOC_NCBI_FLOOR = 500_000;
    private static final int AD_HOC_NCBI_RANGE = 1_000_000;

    @Autowired
    private TaxonService taxonService;

    /* ------------------------------------------------------------------ */
    /* Seeded-taxon shortcuts                                             */
    /* ------------------------------------------------------------------ */

    /** Resolve the seeded mouse taxon (NCBI 10090). */
    public Taxon mouse() {
        return requireByCommonName( "mouse" );
    }

    /** Resolve the seeded human taxon (NCBI 9606). */
    public Taxon human() {
        return requireByCommonName( "human" );
    }

    /** Resolve the seeded rat taxon (NCBI 10116). */
    public Taxon rat() {
        return requireByCommonName( "rat" );
    }

    /** Resolve the seeded zebrafish taxon (NCBI 7955). */
    public Taxon zebrafish() {
        return requireByCommonName( "zebrafish" );
    }

    /** Resolve the seeded fly taxon (NCBI 7227). */
    public Taxon fly() {
        return requireByCommonName( "fly" );
    }

    /** Resolve the seeded worm taxon (NCBI 6239). */
    public Taxon worm() {
        return requireByCommonName( "worm" );
    }

    /** Resolve the seeded yeast taxon (NCBI 4932, secondary 559292). */
    public Taxon yeast() {
        return requireByCommonName( "yeast" );
    }

    /* ------------------------------------------------------------------ */
    /* Parameterized resolvers (still seed-only, no fabrication)          */
    /* ------------------------------------------------------------------ */

    /**
     * Resolve a seeded taxon by its common name. Throws if not present.
     */
    public ResolveBuilder byCommonName( String commonName ) {
        return new ResolveBuilder( this, ResolveBy.COMMON_NAME, commonName );
    }

    /**
     * Resolve a seeded taxon by its scientific name. Throws if not present.
     */
    public ResolveBuilder byScientificName( String scientificName ) {
        return new ResolveBuilder( this, ResolveBy.SCIENTIFIC_NAME, scientificName );
    }

    /**
     * Resolve a seeded taxon by its NCBI id. Throws if not present.
     */
    public ResolveBuilder byNcbiId( Integer ncbiId ) {
        return new ResolveBuilder( this, ResolveBy.NCBI_ID, ncbiId );
    }

    /* ------------------------------------------------------------------ */
    /* Ad-hoc taxon creation (admin-only)                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Start building a freshly-created (non-seeded) taxon. Defaults to a
     * randomized scientific name, common name, and NCBI id; {@code
     * isGenesUsable=true}. Caller is responsible for cleaning up unless
     * the test extends a context that does it for them.
     */
    public AdHocBuilder adHoc() {
        return new AdHocBuilder( this );
    }

    /* ------------------------------------------------------------------ */
    /* Builders                                                           */
    /* ------------------------------------------------------------------ */

    enum ResolveBy {
        COMMON_NAME,
        SCIENTIFIC_NAME,
        NCBI_ID
    }

    /**
     * Builder for a resolved (seeded) taxon. No state to configure - the
     * builder exists for stylistic consistency with the other factories
     * and to make the call site read {@code taxonFactory.byCommonName("mouse").build()}.
     */
    public static final class ResolveBuilder {

        private final TaxonFactory factory;
        private final ResolveBy by;
        private final Object key;

        private ResolveBuilder( TaxonFactory factory, ResolveBy by, Object key ) {
            this.factory = factory;
            this.by = by;
            this.key = key;
        }

        public Taxon build() {
            Taxon t;
            switch ( by ) {
                case COMMON_NAME:
                    t = factory.taxonService.findByCommonName( ( String ) key );
                    break;
                case SCIENTIFIC_NAME:
                    t = factory.taxonService.findByScientificName( ( String ) key );
                    break;
                case NCBI_ID:
                    t = factory.taxonService.findByNcbiId( ( Integer ) key );
                    break;
                default:
                    throw new IllegalStateException( "unknown ResolveBy: " + by );
            }
            if ( t == null ) {
                throw new IllegalStateException(
                        "Taxon not found by " + by + "=" + key
                                + "; init-data.sql not loaded or key misspelled?" );
            }
            return t;
        }
    }

    /**
     * Builder for a newly-created ad-hoc taxon. Persists via {@link
     * TaxonService#findOrCreate(Taxon)} so the production admin path
     * fires. Single-use; call {@link #build()} once.
     */
    public static final class AdHocBuilder {

        private final TaxonFactory factory;

        @Nullable
        private String scientificName;
        @Nullable
        private String commonName;
        @Nullable
        private Integer ncbiId;
        private boolean genesUsable = true;

        private AdHocBuilder( TaxonFactory factory ) {
            this.factory = factory;
        }

        public AdHocBuilder withScientificName( String name ) {
            this.scientificName = name;
            return this;
        }

        public AdHocBuilder withCommonName( String name ) {
            this.commonName = name;
            return this;
        }

        public AdHocBuilder withNcbiId( Integer id ) {
            this.ncbiId = id;
            return this;
        }

        public AdHocBuilder withGenesUsable( boolean v ) {
            this.genesUsable = v;
            return this;
        }

        public Taxon build() {
            String sci = ( this.scientificName != null )
                    ? this.scientificName
                    : "Testus_" + RandomStringUtils.insecure().nextAlphabetic( RANDOM_STRING_LENGTH );
            String common = ( this.commonName != null )
                    ? this.commonName
                    : "testbeast_" + RandomStringUtils.insecure().nextAlphabetic( RANDOM_STRING_LENGTH );
            Integer nc = ( this.ncbiId != null )
                    ? this.ncbiId
                    : AD_HOC_NCBI_FLOOR
                            + java.util.concurrent.ThreadLocalRandom.current().nextInt( AD_HOC_NCBI_RANGE );

            Taxon t = Taxon.Factory.newInstance( sci, common, nc, this.genesUsable );
            return factory.taxonService.findOrCreate( t );
        }
    }

    /* ------------------------------------------------------------------ */
    /* internals                                                          */
    /* ------------------------------------------------------------------ */

    private Taxon requireByCommonName( String commonName ) {
        Taxon t = taxonService.findByCommonName( commonName );
        if ( t == null ) {
            throw new IllegalStateException(
                    "Seeded taxon '" + commonName + "' not found; init-data.sql not loaded?" );
        }
        return t;
    }
}
