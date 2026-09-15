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
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;

/**
 * Typed factory for persistent {@link BioMaterial} test fixtures.
 * <p>
 * Phase 3 replacement for
 * {@code PersistentDummyObjectHelper.getTestPersistentBioMaterial(...)}. Same
 * shape as {@link ExperimentFactory}: fluent builder, sensible defaults,
 * persists through the production service so the ACL-creation listener fires.
 * <pre>
 *   var bm = bioMaterialFactory.build();
 *   var bm = bioMaterialFactory.withTaxon(rat).build();
 *   var child = bioMaterialFactory.withSourceBioMaterial(parent).build();
 *   var bm = bioMaterialFactory.withExternalAccession().build();
 * </pre>
 * <p>
 * Design notes (HB6-relevant):
 * <ul>
 *   <li>{@code build()} persists via {@link BioMaterialService#create(BioMaterial)}.</li>
 *   <li>Taxon is resolved from {@code init-data.sql}-seeded rows via
 *       {@link TaxonService#findByCommonName(String)} — never fabricated
 *       transient.</li>
 *   <li>If a source {@link BioMaterial} is supplied, its id is required (i.e.
 *       it must be persistent already); the factory does not silently
 *       cascade-persist a transient parent.</li>
 *   <li>The optional external accession resolves the {@link ExternalDatabase}
 *       through the service rather than instantiating it transient — the same
 *       pattern as {@link ExperimentFactory}.</li>
 *   <li>The builder is single-use; {@code withXxx} returns the factory itself
 *       (not a separate Builder type) because BM construction is a one-shot
 *       and an extra Builder class would be noise. Each call to {@code build()}
 *       produces a fresh persistent BM but reuses any sticky configuration —
 *       so prefer building each BM with a fresh
 *       {@code bioMaterialFactory.withXxx(...).build()} chain.</li>
 * </ul>
 *
 * @author Phase 3 (Vision §3 — test-fixture rewrite)
 */
@Component
public class BioMaterialFactory {

    private static final int RANDOM_STRING_LENGTH = 10;
    private static final String DEFAULT_TAXON_COMMON_NAME = "mouse";

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    /**
     * Start building a {@link BioMaterial} with defaults: mouse taxon, no
     * source biomaterial, no external accession, auto-generated name.
     */
    public Builder builder() {
        return new Builder( this );
    }

    /**
     * Convenience: build with all defaults.
     */
    public BioMaterial build() {
        return builder().build();
    }

    /**
     * Convenience for the common "specify taxon, defaults elsewhere" case.
     */
    public Builder withTaxon( Taxon t ) {
        return builder().withTaxon( t );
    }

    /**
     * Convenience for the common "specify name, defaults elsewhere" case.
     */
    public Builder withName( String name ) {
        return builder().withName( name );
    }

    /**
     * Convenience for the common "specify parent, defaults elsewhere" case.
     */
    public Builder withSourceBioMaterial( BioMaterial parent ) {
        return builder().withSourceBioMaterial( parent );
    }

    /**
     * Convenience: attach a GEO external accession (defaults elsewhere).
     */
    public Builder withExternalAccession() {
        return builder().withExternalAccession();
    }

    /**
     * Fluent builder. All {@code withXxx(...)} return {@code this}.
     * Single-use; call {@link #build()} exactly once.
     */
    public static final class Builder {

        private final BioMaterialFactory factory;

        @Nullable
        private Taxon taxon;
        @Nullable
        private BioMaterial sourceBioMaterial;
        @Nullable
        private String name;
        private boolean includeExternalAccession;

        private Builder( BioMaterialFactory factory ) {
            this.factory = factory;
        }

        public Builder withTaxon( Taxon t ) {
            this.taxon = t;
            return this;
        }

        public Builder withSourceBioMaterial( BioMaterial parent ) {
            if ( parent == null ) {
                throw new IllegalArgumentException( "withSourceBioMaterial requires non-null parent" );
            }
            if ( parent.getId() == null ) {
                throw new IllegalArgumentException(
                        "withSourceBioMaterial parent must already be persistent (have an id)" );
            }
            this.sourceBioMaterial = parent;
            return this;
        }

        public Builder withName( String name ) {
            this.name = name;
            return this;
        }

        public Builder withExternalAccession() {
            this.includeExternalAccession = true;
            return this;
        }

        /**
         * Build, persist, and return the BM. Goes through
         * {@link BioMaterialService#create(BioMaterial)} so the ACL-creation
         * listener fires.
         */
        public BioMaterial build() {
            Taxon t = ( this.taxon != null )
                    ? this.taxon
                    : factory.requireTaxon( DEFAULT_TAXON_COMMON_NAME );

            BioMaterial bm = BioMaterial.Factory.newInstance();
            String nm = ( this.name != null )
                    ? this.name
                    : RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) + "_testbm";
            bm.setName( nm );
            bm.setSourceTaxon( t );

            if ( this.sourceBioMaterial != null ) {
                bm.setSourceBioMaterial( this.sourceBioMaterial );
            }

            if ( this.includeExternalAccession ) {
                ExternalDatabase geo = factory.externalDatabaseService.findByName( ExternalDatabases.GEO );
                if ( geo == null ) {
                    throw new IllegalStateException(
                            "ExternalDatabase '" + ExternalDatabases.GEO + "' not found; init-entities.sql not loaded?" );
                }
                DatabaseEntry acc = DatabaseEntry.Factory.newInstance();
                acc.setAccession( RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH ) );
                acc.setExternalDatabase( geo );
                bm.setExternalAccession( acc );
            }

            return factory.bioMaterialService.create( bm );
        }
    }

    /* ------------------------------------------------------------------ */
    /* internals                                                          */
    /* ------------------------------------------------------------------ */

    private Taxon requireTaxon( String commonName ) {
        Taxon t = taxonService.findByCommonName( commonName );
        if ( t == null ) {
            throw new IllegalStateException(
                    "Taxon '" + commonName + "' not found; init-data.sql not loaded?" );
        }
        return t;
    }
}
