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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import javax.annotation.Nullable;

/**
 * Typed factory for persistent {@link BioAssay} test fixtures.
 * <p>
 * Phase 3 split-out from the inline BA construction inside
 * {@link ExperimentFactory.Builder#build()}. Use this factory when a test
 * needs an isolated, persistent {@link BioAssay} (not necessarily attached
 * to an {@link ubic.gemma.model.expression.experiment.ExpressionExperiment});
 * for the EE-attached case keep using {@link ExperimentFactory}.
 * <p>
 * Usage:
 * <pre>
 *   // Fresh BA with auto-created BioMaterial + ArrayDesign on mouse
 *   BioAssay ba = bioAssayFactory.builder().build();
 *
 *   // BA wired to a specific BM and AD
 *   BioAssay ba = bioAssayFactory.builder()
 *                      .withBioMaterial(bm)
 *                      .withArrayDesign(ad)
 *                      .build();
 *
 *   // Named BA
 *   BioAssay ba = bioAssayFactory.builder()
 *                      .withName("GSM12345_ba")
 *                      .build();
 * </pre>
 * <p>
 * Design notes (HB6-relevant):
 * <ul>
 *   <li>The BA mapping does NOT cascade from {@code BioAssay} to
 *       {@code sampleUsed} / {@code arrayDesignUsed}. Both must be
 *       persistent before {@link BioAssayService#create(BioAssay)} is
 *       called. This factory pre-persists both via their respective
 *       services so the production code path fires (ACL listeners on the
 *       BM and AD).</li>
 *   <li>{@code BioMaterial.getBioAssaysUsedIn().add(ba)} is the
 *       collection mutation that keeps the inverse side in sync. We do
 *       this BEFORE the BA persist so subsequent loads see the
 *       relationship.</li>
 *   <li>A transient GEO {@link DatabaseEntry} accession is attached to
 *       every BA - mirrors the inline pattern in ExperimentFactory and
 *       satisfies tests that filter on accession.</li>
 *   <li>{@code BioMaterialFactory} does not yet exist as a separate
 *       component; until it lands, this factory inlines BM creation
 *       (same shape as {@link ExperimentFactory}'s inline BM block).
 *       The {@link Builder#withBioMaterial(BioMaterial)} hook is the
 *       seam to swap in once {@code BioMaterialFactory} lands.</li>
 * </ul>
 *
 * @author Phase 3 (Vision section 3 - test-fixture rewrite)
 */
@Component
public class BioAssayFactory {

    private static final int RANDOM_STRING_LENGTH = 10;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private ArrayDesignFactory arrayDesignFactory;

    @Autowired
    private TaxonFactory taxonFactory;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    /**
     * Start building a {@link BioAssay}. All defaults can be overridden
     * via {@code withXxx(...)} methods.
     */
    public Builder builder() {
        return new Builder( this );
    }

    /**
     * Fluent builder for a {@link BioAssay}. All {@code withXxx(...)}
     * methods return {@code this} for chaining. Single-use; call
     * {@link #build()} once.
     */
    public static final class Builder {

        private final BioAssayFactory factory;

        @Nullable
        private String name;
        @Nullable
        private BioMaterial bioMaterial;
        @Nullable
        private ArrayDesign arrayDesign;
        @Nullable
        private Taxon taxon;
        private boolean attachAccession = true;
        private boolean isOutlier = false;
        private boolean sequencePairedReads = false;

        private Builder( BioAssayFactory factory ) {
            this.factory = factory;
        }

        public Builder withName( String n ) {
            this.name = n;
            return this;
        }

        public Builder withBioMaterial( BioMaterial bm ) {
            this.bioMaterial = bm;
            return this;
        }

        public Builder withArrayDesign( ArrayDesign ad ) {
            this.arrayDesign = ad;
            return this;
        }

        /**
         * Set the taxon to use when auto-creating the {@link BioMaterial}
         * and {@link ArrayDesign} defaults. Ignored if both are supplied
         * via {@link #withBioMaterial(BioMaterial)} and
         * {@link #withArrayDesign(ArrayDesign)}.
         */
        public Builder withTaxon( Taxon t ) {
            this.taxon = t;
            return this;
        }

        public Builder withAccession( boolean v ) {
            this.attachAccession = v;
            return this;
        }

        public Builder withIsOutlier( boolean v ) {
            this.isOutlier = v;
            return this;
        }

        public Builder withSequencePairedReads( boolean v ) {
            this.sequencePairedReads = v;
            return this;
        }

        /**
         * Build, persist, and return the BA. The {@link BioMaterial} and
         * {@link ArrayDesign} are persisted first (no cascade from BA);
         * the BA itself is persisted via
         * {@link BioAssayService#create(BioAssay)} so the ACL-creation
         * listener fires.
         */
        public BioAssay build() {
            BioAssay ba = this.buildTransient();
            return factory.bioAssayService.create( ba );
        }

        /**
         * Build the BA WITHOUT persisting it. The {@link BioMaterial}
         * and {@link ArrayDesign} are still persisted (no cascade from
         * BA), and the inverse side {@code BM.bioAssaysUsedIn} is
         * updated. The returned BA is transient and ready to be added
         * to an {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}'s
         * {@code bioAssays} collection (which cascades the persist).
         * <p>
         * This is the path {@link ExperimentFactory} uses when building
         * EE-attached BAs; the EE cascade handles the BA persist.
         */
        public BioAssay buildTransient() {
            Taxon t = ( this.taxon != null )
                    ? this.taxon
                    : factory.taxonFactory.mouse();

            String nm = ( this.name != null )
                    ? this.name
                    : "ba_" + RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH );

            BioMaterial bm = ( this.bioMaterial != null )
                    ? this.bioMaterial
                    : factory.createDefaultBioMaterial( nm, t );

            ArrayDesign ad = ( this.arrayDesign != null )
                    ? this.arrayDesign
                    : factory.arrayDesignFactory.oneColor().withTaxon( t ).build();

            BioAssay ba = BioAssay.Factory.newInstance( nm, ad, bm );
            ba.setIsOutlier( this.isOutlier );
            ba.setSequencePairedReads( this.sequencePairedReads );

            if ( this.attachAccession ) {
                ExternalDatabase geo = factory.externalDatabaseService.findByName( ExternalDatabases.GEO );
                if ( geo == null ) {
                    throw new IllegalStateException(
                            "ExternalDatabase '" + ExternalDatabases.GEO
                                    + "' not found; init-entities.sql not loaded?" );
                }
                DatabaseEntry acc = DatabaseEntry.Factory.newInstance();
                acc.setAccession( nm );
                acc.setExternalDatabase( geo );
                ba.setAccession( acc );
            }

            // Keep inverse side in sync before persist.
            bm.getBioAssaysUsedIn().add( ba );
            return ba;
        }
    }

    /* ------------------------------------------------------------------ */
    /* internals                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Inline BM creation pending {@code BioMaterialFactory}. Persists
     * the BM through {@link BioMaterialService#create(BioMaterial)} so
     * the ACL listener on BM fires.
     */
    private BioMaterial createDefaultBioMaterial( String suffix, Taxon t ) {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( "bm_" + suffix );
        bm.setSourceTaxon( t );
        return bioMaterialService.create( bm );
    }
}
