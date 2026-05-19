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
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nullable;

/**
 * Typed factory for persistent {@link ArrayDesign} test fixtures.
 * <p>
 * Phase 3 replacement for the
 * {@code PersistentDummyObjectHelper.getTestPersistentArrayDesign(...)} family.
 * Same shape as {@link ExperimentFactory}: fluent builder, sensible defaults,
 * persists through the production service so the ACL-creation listener fires.
 * <pre>
 *   var ad = arrayDesignFactory.build();
 *   var ad = arrayDesignFactory.builder().withCompositeSequences(5).build();
 *   var ad = arrayDesignFactory.builder().withTaxon(rat).withShortName("AD_X").build();
 * </pre>
 * <p>
 * Design notes (HB6-relevant):
 * <ul>
 *   <li>{@code build()} persists via
 *       {@link ArrayDesignService#create(ArrayDesign)}.</li>
 *   <li>Taxon is resolved from {@code init-data.sql}-seeded rows via
 *       {@link TaxonService#findByCommonName(String)} — never fabricated
 *       transient.</li>
 *   <li>{@link CompositeSequence}s are attached to the AD's collection via
 *       {@code getCompositeSequences().add(...)} on the transient AD, then
 *       persisted as part of the AD via the {@code create(...)} cascade — we
 *       never replace the collection with a fresh {@code HashSet}.</li>
 *   <li>Composite sequences here are skeletal: name + back-reference to AD.
 *       No biosequence / probe-mapping data is attached; tests that need
 *       sequences should compose a follow-on factory (planned:
 *       {@code CompositeSequenceFactory}) rather than overload this one.</li>
 * </ul>
 *
 * @author Phase 3 (Vision §3 — test-fixture rewrite)
 */
@Component
public class ArrayDesignFactory {

    private static final int RANDOM_STRING_LENGTH = 10;
    private static final String DEFAULT_TAXON_COMMON_NAME = "mouse";

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private TaxonService taxonService;

    /**
     * Start building an {@link ArrayDesign} with defaults: mouse primary
     * taxon, {@link TechnologyType#GENELIST}, auto-generated short name and
     * long name, zero composite sequences.
     */
    public Builder builder() {
        return new Builder( this );
    }

    /**
     * Convenience: build an AD with all defaults.
     */
    public ArrayDesign build() {
        return builder().build();
    }

    /**
     * Fluent builder. All {@code withXxx(...)} return {@code this}.
     * Single-use; call {@link #build()} exactly once.
     */
    public static final class Builder {

        private final ArrayDesignFactory factory;

        @Nullable
        private Taxon taxon;
        @Nullable
        private String shortName;
        @Nullable
        private String name;
        private int numCompositeSequences;
        private TechnologyType technologyType = TechnologyType.GENELIST;

        private Builder( ArrayDesignFactory factory ) {
            this.factory = factory;
        }

        public Builder withTaxon( Taxon t ) {
            this.taxon = t;
            return this;
        }

        public Builder withShortName( String s ) {
            this.shortName = s;
            return this;
        }

        public Builder withName( String s ) {
            this.name = s;
            return this;
        }

        public Builder withCompositeSequences( int n ) {
            if ( n < 0 ) {
                throw new IllegalArgumentException( "withCompositeSequences requires n >= 0, got " + n );
            }
            this.numCompositeSequences = n;
            return this;
        }

        public Builder withTechnologyType( TechnologyType tt ) {
            this.technologyType = tt;
            return this;
        }

        /**
         * Build, persist, and return the AD. Goes through
         * {@link ArrayDesignService#create(ArrayDesign)} so the ACL-creation
         * listener fires.
         */
        public ArrayDesign build() {
            Taxon t = ( this.taxon != null )
                    ? this.taxon
                    : factory.requireTaxon( DEFAULT_TAXON_COMMON_NAME );

            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            String sn = ( this.shortName != null )
                    ? this.shortName
                    : "AD_" + RandomStringUtils.insecure().nextAlphabetic( 5 );
            String fn = ( this.name != null )
                    ? this.name
                    : "arrayDesign_" + RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH );
            ad.setShortName( sn );
            ad.setName( fn );
            ad.setTechnologyType( this.technologyType );
            ad.setPrimaryTaxon( t );

            for ( int i = 0; i < this.numCompositeSequences; i++ ) {
                CompositeSequence cs = CompositeSequence.Factory.newInstance();
                cs.setName( RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) + "_testcs" );
                cs.setArrayDesign( ad );
                ad.getCompositeSequences().add( cs );
            }

            return factory.arrayDesignService.create( ad );
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
