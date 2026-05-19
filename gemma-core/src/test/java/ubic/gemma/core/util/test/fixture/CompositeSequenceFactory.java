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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import javax.annotation.Nullable;

/**
 * Typed factory for persistent {@link CompositeSequence} (probe) test
 * fixtures.
 * <p>
 * Phase 3 split-out from the inline CS construction inside
 * {@link ArrayDesignFactory.Builder#build()}. Use this factory when a test
 * needs an isolated, persistent {@link CompositeSequence} (e.g.
 * gene-expression-vector tests that need probes independent of an AD-wide
 * build, or to attach an extra probe to an existing AD after the AD is
 * persisted).
 * <p>
 * Usage:
 * <pre>
 *   // Default: fresh CS on a freshly-built one-color AD, with attached BioSequence
 *   CompositeSequence cs = compositeSequenceFactory.builder().build();
 *
 *   // CS attached to a specific AD, with no biosequence
 *   CompositeSequence cs = compositeSequenceFactory.builder()
 *                              .withArrayDesign(ad)
 *                              .withBioSequence(false)
 *                              .build();
 *
 *   // Named CS
 *   CompositeSequence cs = compositeSequenceFactory.builder()
 *                              .withName("probe_001")
 *                              .build();
 * </pre>
 * <p>
 * Design notes (HB6-relevant):
 * <ul>
 *   <li>The CS mapping does NOT cascade from {@code CompositeSequence} to
 *       {@code biologicalCharacteristic}. The BS is therefore persisted
 *       through {@link BioSequenceService#findOrCreate(BioSequence)} BEFORE
 *       {@link CompositeSequenceService#create(CompositeSequence)} runs.
 *       Skipping this caused HB5&rarr;HB6 breakage; doing it explicitly is
 *       the safe pattern.</li>
 *   <li>The {@link ArrayDesign} is created via {@link ArrayDesignFactory}
 *       (default: {@code oneColor()}) when not supplied. The CS is added
 *       to {@code ad.getCompositeSequences()} via {@code add(...)} on the
 *       managed collection - never via
 *       {@code setCompositeSequences(new HashSet&lt;&gt;(...))}.</li>
 *   <li>{@link Taxon} on the BS is resolved via {@link TaxonFactory}
 *       (default: mouse).</li>
 *   <li>The attached BioSequence carries a GenBank {@link DatabaseEntry}
 *       so tests filtering on the sequence-database link have something
 *       sensible to find. Override via {@link Builder#withBioSequence(boolean)}
 *       to skip.</li>
 * </ul>
 *
 * @author Phase 3 (Vision section 3 - test-fixture rewrite)
 */
@Component
public class CompositeSequenceFactory {

    private static final int RANDOM_STRING_LENGTH = 10;
    private static final int BIOSEQUENCE_LEN = 40;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private ArrayDesignFactory arrayDesignFactory;

    @Autowired
    private TaxonFactory taxonFactory;

    /**
     * Start building a {@link CompositeSequence}. All defaults can be
     * overridden via {@code withXxx(...)} methods.
     */
    public Builder builder() {
        return new Builder( this );
    }

    /**
     * Fluent builder for a {@link CompositeSequence}. All
     * {@code withXxx(...)} methods return {@code this} for chaining.
     * Single-use; call {@link #build()} once.
     */
    public static final class Builder {

        private final CompositeSequenceFactory factory;

        @Nullable
        private String name;
        @Nullable
        private String description;
        @Nullable
        private ArrayDesign arrayDesign;
        @Nullable
        private Taxon taxon;
        private boolean withBioSequence = true;

        private Builder( CompositeSequenceFactory factory ) {
            this.factory = factory;
        }

        public Builder withName( String n ) {
            this.name = n;
            return this;
        }

        public Builder withDescription( String d ) {
            this.description = d;
            return this;
        }

        public Builder withArrayDesign( ArrayDesign ad ) {
            this.arrayDesign = ad;
            return this;
        }

        /**
         * Set the taxon used for the auto-created {@link BioSequence}.
         * Ignored if {@link #withBioSequence(boolean)} is false or if the
         * AD has a primaryTaxon already (the BS taxon then mirrors that).
         */
        public Builder withTaxon( Taxon t ) {
            this.taxon = t;
            return this;
        }

        /**
         * If true (default), attach a persistent {@link BioSequence} (with
         * a GenBank {@link DatabaseEntry}) to the CS. If false, the CS is
         * persisted without a biological characteristic.
         */
        public Builder withBioSequence( boolean v ) {
            this.withBioSequence = v;
            return this;
        }

        /**
         * Build, persist, and return the CS. The {@link ArrayDesign} and
         * optional {@link BioSequence} are persisted first (no cascade
         * from CS); the CS itself is persisted via
         * {@link CompositeSequenceService#create(CompositeSequence)}.
         */
        public CompositeSequence build() {
            ArrayDesign ad = ( this.arrayDesign != null )
                    ? this.arrayDesign
                    : factory.arrayDesignFactory.oneColor().build();

            String nm = ( this.name != null )
                    ? this.name
                    : "test_cs_" + RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH );

            CompositeSequence cs = CompositeSequence.Factory.newInstance( nm, ad );
            if ( this.description != null ) {
                cs.setDescription( this.description );
            }

            if ( this.withBioSequence ) {
                Taxon t = ( this.taxon != null )
                        ? this.taxon
                        : ( ad.getPrimaryTaxon() != null
                                ? ad.getPrimaryTaxon()
                                : factory.taxonFactory.mouse() );

                ExternalDatabase genbank = factory.externalDatabaseService.findByName( ExternalDatabases.GENBANK );
                if ( genbank == null ) {
                    throw new IllegalStateException(
                            "ExternalDatabase '" + ExternalDatabases.GENBANK
                                    + "' not found; init-entities.sql not loaded?" );
                }

                BioSequence bs = BioSequence.Factory.newInstance();
                bs.setName( RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) + "_testbiosequence" );
                bs.setSequence( RandomStringUtils.insecure().next( BIOSEQUENCE_LEN, "ATCG" ) );
                bs.setTaxon( t );

                DatabaseEntry de = DatabaseEntry.Factory.newInstance();
                de.setExternalDatabase( genbank );
                de.setAccession( RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH ) );
                bs.setSequenceDatabaseEntry( de );

                BioSequence persistedBs = factory.bioSequenceService.findOrCreate( bs );
                cs.setBiologicalCharacteristic( persistedBs );
            }

            return factory.compositeSequenceService.create( cs );
        }
    }
}
