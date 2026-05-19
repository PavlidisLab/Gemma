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
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Typed factory for persistent {@link ArrayDesign} test fixtures.
 * <p>
 * This is the Phase 3 replacement for
 * {@code PersistentDummyObjectHelper.getTestPersistentArrayDesign(...)}.
 * It produces a persistent {@link ArrayDesign} with optional probes
 * ({@link CompositeSequence}) and optional sequences ({@link BioSequence}).
 * <p>
 * Usage:
 * <pre>
 *   // Empty AD, default mouse taxon, ONECOLOR
 *   ArrayDesign ad = arrayDesignFactory.oneColor().build();
 *
 *   // AD with 10 probes, no biosequences
 *   ArrayDesign ad = arrayDesignFactory.oneColor().withProbes(10).build();
 *
 *   // AD with 10 probes, each with a BioSequence attached (GenBank entry)
 *   ArrayDesign ad = arrayDesignFactory.oneColor()
 *                       .withProbes(10)
 *                       .withSequences(true)
 *                       .withTaxon(humanTaxon)
 *                       .build();
 *
 *   // Two-color (cdna probe) variant
 *   ArrayDesign ad = arrayDesignFactory.twoColor().withProbes(20).build();
 *
 *   // Affymetrix-style "GENE_CHIP" with deterministic probe naming
 *   ArrayDesign ad = arrayDesignFactory.geneChip()
 *                       .withProbes(50)
 *                       .withRandomProbeNames(false)
 *                       .build();
 * </pre>
 * <p>
 * Design notes (HB6-relevant):
 * <ul>
 *   <li>{@code build()} persists the AD via {@link
 *       ArrayDesignService#create(ArrayDesign)} so the ACL-creation
 *       listener fires through the production code path (mirrors how
 *       real ADs are created on ingest).</li>
 *   <li>CompositeSequences are attached to {@code ad.getCompositeSequences()}
 *       via {@code add(...)} (not via {@code setCompositeSequences(new
 *       HashSet&lt;&gt;(...))}). The CS&rarr;AD set has cascade=all on the AD
 *       side, so CSes persist as a side effect of AD persist.</li>
 *   <li>BioSequences are persisted FIRST via {@link
 *       BioSequenceService#findOrCreate(Collection)} because the
 *       CS&rarr;biologicalCharacteristic FK has no cascade. Skipping
 *       this caused HB5&rarr;HB6 breakage; doing it explicitly is the
 *       safe pattern.</li>
 *   <li>Taxa are resolved via {@link TaxonFactory} so they come from the
 *       seeded {@code init-data.sql} rows (never fabricated transient).</li>
 *   <li>GenBank {@link ExternalDatabase} is resolved from {@link
 *       ExternalDatabaseService#findByName(String)}; only the {@link
 *       DatabaseEntry} that links to it is transient.</li>
 * </ul>
 *
 * @author Phase 3 (Vision section 3 - test-fixture rewrite)
 */
@Component
public class ArrayDesignFactory {

    private static final int RANDOM_STRING_LENGTH = 10;
    private static final int BIOSEQUENCE_LEN = 40;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private TaxonFactory taxonFactory;

    /**
     * Start building a one-color (single-channel) array design.
     * Default flavour for most expression tests.
     */
    public Builder oneColor() {
        return new Builder( this, TechnologyType.ONECOLOR );
    }

    /**
     * Start building a two-color (dual-channel, cDNA) array design.
     */
    public Builder twoColor() {
        return new Builder( this, TechnologyType.TWOCOLOR );
    }

    /**
     * Start building a "GENE_CHIP" (Affymetrix-style) array design.
     */
    public Builder geneChip() {
        return new Builder( this, TechnologyType.GENELIST );
    }

    /**
     * Start building an array design with a caller-supplied technology
     * type. Prefer the named starters above when they fit.
     */
    public Builder withTechnologyType( TechnologyType tt ) {
        return new Builder( this, tt );
    }

    /**
     * Fluent builder for an {@link ArrayDesign}. All {@code withXxx(...)}
     * methods return {@code this} for chaining. Single-use; call
     * {@link #build()} once.
     */
    public static final class Builder {

        private final ArrayDesignFactory factory;
        private final TechnologyType technologyType;

        @Nullable
        private Taxon primaryTaxon;
        @Nullable
        private String shortName;
        @Nullable
        private String name;
        private int numProbes = 0;
        private boolean withSequences = false;
        private boolean randomProbeNames = true;

        private Builder( ArrayDesignFactory factory, TechnologyType technologyType ) {
            this.factory = factory;
            this.technologyType = technologyType;
        }

        public Builder withTaxon( Taxon t ) {
            this.primaryTaxon = t;
            return this;
        }

        public Builder withShortName( String s ) {
            this.shortName = s;
            return this;
        }

        public Builder withName( String n ) {
            this.name = n;
            return this;
        }

        /**
         * Attach N composite sequences (probes) to the array design.
         * Default 0 (empty AD).
         */
        public Builder withProbes( int n ) {
            if ( n < 0 ) {
                throw new IllegalArgumentException( "withProbes requires n >= 0, got " + n );
            }
            this.numProbes = n;
            return this;
        }

        /**
         * If true, each probe is also given a {@link BioSequence}
         * (persisted ahead of the AD). Default false - tests that just
         * need probes for FK-shape don't pay the cost.
         */
        public Builder withSequences( boolean v ) {
            this.withSequences = v;
            return this;
        }

        /**
         * If false, probes are named {@code probeset_0}, {@code probeset_1}, ...
         * Useful for tests that want deterministic names. Default true
         * (random numeric suffix).
         */
        public Builder withRandomProbeNames( boolean v ) {
            this.randomProbeNames = v;
            return this;
        }

        /**
         * Build, persist, and return the AD. Goes through {@link
         * ArrayDesignService#create(ArrayDesign)} so the ACL-creation
         * listener fires.
         */
        public ArrayDesign build() {
            Taxon t = ( this.primaryTaxon != null )
                    ? this.primaryTaxon
                    : factory.taxonFactory.mouse();

            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            String shortNm = ( this.shortName != null )
                    ? this.shortName
                    : "AD_" + RandomStringUtils.insecure().nextAlphabetic( 5 );
            ad.setShortName( shortNm );
            ad.setName( ( this.name != null )
                    ? this.name
                    : "arrayDesign_" + RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) );
            ad.setDescription( "A test array design (ArrayDesignFactory, " + technologyType + ")" );
            ad.setTechnologyType( technologyType );
            ad.setPrimaryTaxon( t );

            if ( this.numProbes > 0 ) {
                // BioSequences first (no cascade from CS), then CSes.
                Iterator<BioSequence> bsIter = Collections.emptyIterator();
                if ( this.withSequences ) {
                    ExternalDatabase genbank = factory.externalDatabaseService.findByName( ExternalDatabases.GENBANK );
                    if ( genbank == null ) {
                        throw new IllegalStateException(
                                "ExternalDatabase '" + ExternalDatabases.GENBANK
                                        + "' not found; init-entities.sql not loaded?" );
                    }
                    java.util.List<BioSequence> transients = new java.util.ArrayList<>( this.numProbes );
                    for ( int i = 0; i < this.numProbes; i++ ) {
                        transients.add( newTransientBioSequence( t, genbank ) );
                    }
                    Collection<BioSequence> persisted = factory.bioSequenceService.findOrCreate( transients );
                    bsIter = persisted.iterator();
                }

                for ( int i = 0; i < this.numProbes; i++ ) {
                    CompositeSequence cs = CompositeSequence.Factory.newInstance();
                    cs.setName( this.randomProbeNames
                            ? RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) + "_testcs"
                            : "probeset_" + i );
                    cs.setArrayDesign( ad );
                    if ( this.withSequences && bsIter.hasNext() ) {
                        cs.setBiologicalCharacteristic( bsIter.next() );
                    }
                    // Mutate, don't replace - HB6 PersistentSet safety.
                    ad.getCompositeSequences().add( cs );
                }
            }

            return factory.arrayDesignService.create( ad );
        }

        private BioSequence newTransientBioSequence( Taxon taxon, ExternalDatabase genbank ) {
            BioSequence bs = BioSequence.Factory.newInstance();
            bs.setName( RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) + "_testbiosequence" );
            bs.setSequence( RandomStringUtils.insecure().next( BIOSEQUENCE_LEN, "ATCG" ) );
            bs.setTaxon( taxon );

            DatabaseEntry de = DatabaseEntry.Factory.newInstance();
            de.setExternalDatabase( genbank );
            de.setAccession( RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH ) );
            bs.setSequenceDatabaseEntry( de );
            return bs;
        }
    }
}
