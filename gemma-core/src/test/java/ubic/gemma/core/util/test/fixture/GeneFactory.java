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
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Typed factory for persistent {@link Gene} test fixtures.
 * <p>
 * Phase 3 replacement for
 * {@code PersistentDummyObjectHelper.getTestPersistentGene(...)}. Produces a
 * persistent {@link Gene} on a seeded {@link Taxon} (default: mouse), with an
 * NCBI id in the synthetic 500k+ range, optionally accompanied by
 * {@link GeneProduct}s.
 * <p>
 * Usage:
 * <pre>
 *   // Default: mouse gene, random name, synthetic NCBI id, no products
 *   Gene g = geneFactory.builder().build();
 *
 *   // Human gene with a specific official symbol
 *   Gene g = geneFactory.builder()
 *                .withTaxon(humanTaxon)
 *                .withOfficialSymbol("BRCA1")
 *                .build();
 *
 *   // Mouse gene with 3 attached gene products
 *   Gene g = geneFactory.builder().withGeneProducts(3).build();
 *
 *   // Fully specified
 *   Gene g = geneFactory.builder()
 *                .withName("brca1_test")
 *                .withOfficialSymbol("BRCA1")
 *                .withOfficialName("breast cancer 1")
 *                .withNcbiId(672)
 *                .withTaxon(taxonFactory.human())
 *                .build();
 * </pre>
 * <p>
 * Design notes (HB6-relevant):
 * <ul>
 *   <li>{@code build()} persists the gene via
 *       {@link GeneService#create(Gene)} so ACL / audit listeners fire on the
 *       production code path.</li>
 *   <li>{@link GeneProduct}s are attached via
 *       {@code gene.getProducts().add(gp)} (mutate, don't replace) before
 *       the gene persist; the Gene&rarr;GeneProduct cascade handles them.</li>
 *   <li>{@link Taxon} is resolved via {@link TaxonFactory} so it comes from
 *       the seeded {@code init-data.sql} rows (never fabricated transient).
 *       Default taxon is mouse; override with
 *       {@link GeneBuilder#withTaxon(Taxon)}.</li>
 *   <li>Synthetic NCBI ids land in {@code [500_000, 1_500_000)} so they
 *       never collide with real assignments. Override with
 *       {@link GeneBuilder#withNcbiId(int)} if a specific value is needed.</li>
 * </ul>
 *
 * @author Phase 3 (Vision section 3 - test-fixture rewrite)
 */
@Component
public class GeneFactory {

    private static final int RANDOM_STRING_LENGTH = 8;
    /** Floor for synthetic NCBI gene ids; well above any real assignment. */
    private static final int SYNTHETIC_NCBI_FLOOR = 500_000;
    private static final int SYNTHETIC_NCBI_RANGE = 1_000_000;

    @Autowired
    private GeneService geneService;

    @Autowired
    private TaxonFactory taxonFactory;

    /**
     * Start building a {@link Gene}. All defaults can be overridden via
     * {@code withXxx(...)} methods.
     */
    public GeneBuilder builder() {
        return new GeneBuilder( this );
    }

    /**
     * Fluent builder for a {@link Gene}. All {@code withXxx(...)} methods
     * return {@code this} for chaining. Single-use; call {@link #build()}
     * once.
     */
    public static final class GeneBuilder {

        private final GeneFactory factory;

        @Nullable
        private String name;
        @Nullable
        private String officialSymbol;
        @Nullable
        private String officialName;
        @Nullable
        private Integer ncbiId;
        @Nullable
        private Taxon taxon;
        private int geneProductCount = 0;

        private GeneBuilder( GeneFactory factory ) {
            this.factory = factory;
        }

        public GeneBuilder withName( String n ) {
            this.name = n;
            return this;
        }

        public GeneBuilder withOfficialSymbol( String s ) {
            this.officialSymbol = s;
            return this;
        }

        public GeneBuilder withOfficialName( String n ) {
            this.officialName = n;
            return this;
        }

        public GeneBuilder withNcbiId( int id ) {
            this.ncbiId = id;
            return this;
        }

        public GeneBuilder withTaxon( Taxon t ) {
            this.taxon = t;
            return this;
        }

        /**
         * Attach N {@link GeneProduct}s to the gene. Default 0. Each
         * product is given a random name and NCBI GI; they cascade-persist
         * as a side effect of the gene persist.
         */
        public GeneBuilder withGeneProducts( int n ) {
            if ( n < 0 ) {
                throw new IllegalArgumentException( "withGeneProducts requires n >= 0, got " + n );
            }
            this.geneProductCount = n;
            return this;
        }

        /**
         * Build, persist, and return the gene. Goes through
         * {@link GeneService#create(Gene)} so the production code path
         * fires (audit + ACL listeners).
         */
        public Gene build() {
            Taxon t = ( this.taxon != null )
                    ? this.taxon
                    : factory.taxonFactory.mouse();

            Gene gene = Gene.Factory.newInstance();
            gene.setName( ( this.name != null )
                    ? this.name
                    : "test_gene_" + RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH ) );
            gene.setOfficialSymbol( ( this.officialSymbol != null )
                    ? this.officialSymbol
                    : RandomStringUtils.insecure().nextAlphabetic( RANDOM_STRING_LENGTH ).toUpperCase() );
            gene.setOfficialName( ( this.officialName != null )
                    ? this.officialName
                    : "test_official_name_" + RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH ) );
            gene.setNcbiGeneId( ( this.ncbiId != null )
                    ? this.ncbiId
                    : SYNTHETIC_NCBI_FLOOR + ThreadLocalRandom.current().nextInt( SYNTHETIC_NCBI_RANGE ) );
            gene.setTaxon( t );

            for ( int i = 0; i < this.geneProductCount; i++ ) {
                GeneProduct gp = GeneProduct.Factory.newInstance();
                gp.setName( "gp_" + RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH ) );
                gp.setNcbiGi( RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH + 2 ) );
                gp.setGene( gene );
                // Mutate, don't replace - HB6 PersistentSet safety.
                gene.getProducts().add( gp );
            }

            return factory.geneService.create( gene );
        }
    }
}
