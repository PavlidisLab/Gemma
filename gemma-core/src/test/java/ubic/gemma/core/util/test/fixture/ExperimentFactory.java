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
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import org.springframework.lang.Nullable;

/**
 * Typed factory for persistent {@link ExpressionExperiment} test fixtures.
 * <p>
 * This is the Phase 3 replacement for the bulkier
 * {@code PersistentDummyObjectHelper.getTestPersistentBasicExpressionExperiment(...)}
 * family. It exists to make test fixtures Hibernate-6-safe by construction
 * and to keep test setup boilerplate down to one line:
 * <pre>
 *   var ee = experimentFactory.bulkRna().build();
 *   var ee = experimentFactory.bulkRna().withSamples(50).withArrayDesign(ad).build();
 *   var ee = experimentFactory.singleCell().build();
 * </pre>
 * <p>
 * Design notes (HB6-relevant):
 * <ul>
 *   <li>{@code build()} persists the experiment via
 *       {@link ExpressionExperimentService#create(ExpressionExperiment)} so the
 *       ACL-creation listener fires through the production code path (mirrors
 *       how real EEs are created).</li>
 *   <li>The transient EE is constructed with all associations attached BEFORE
 *       persist. Collections on the entity are mutated via
 *       {@code getXxx().add(...)} rather than replaced via {@code setXxx(new
 *       HashSet<>(...))}; the latter pattern is the classic HB6
 *       {@code PersistentSet} replacement pitfall on managed entities and the
 *       habit is enforced here so that future
 *       "withXxx().withYyy().postPersistMutation()" extensions remain safe.</li>
 *   <li>Taxa are resolved from {@code init-data.sql}-seeded rows via
 *       {@link TaxonService#findByCommonName(String)}; the factory never
 *       fabricates a transient {@link Taxon} (that pattern relied on HB5's
 *       loose findOrCreate in the persister and broke in HB6).</li>
 *   <li>External databases (GEO, PubMed) are resolved from
 *       {@link ExternalDatabaseService#findByName(String)} for the same
 *       reason; only the {@link DatabaseEntry} that links to them is
 *       transient.</li>
 *   <li>If no {@link ArrayDesign} is supplied, one is created and persisted
 *       up-front through {@link ArrayDesignService#create(ArrayDesign)} so
 *       that bioassays attach to a managed AD.</li>
 * </ul>
 *
 * @author Phase 3 (Vision §3 — test-fixture rewrite)
 */
@Component
public class ExperimentFactory {

    private static final int RANDOM_STRING_LENGTH = 10;
    private static final int DEFAULT_SAMPLES = 8;
    private static final String DEFAULT_TAXON_COMMON_NAME = "mouse";

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private BioAssayFactory bioAssayFactory;

    /**
     * Start building a bulk-RNA expression experiment. Defaults:
     * mouse taxon, 8 samples, freshly-minted single-platform AD with zero
     * composite sequences, one preferred raw {@link QuantitationType}.
     */
    public Builder bulkRna() {
        return new Builder( this, Modality.BULK_RNA );
    }

    /**
     * Start building a single-cell expression experiment. Same defaults as
     * {@link #bulkRna()} except that the EE is flagged single-cell and no
     * raw bulk QT is added by default. Single-cell vector population is the
     * caller's responsibility (use {@code SingleCellExpressionExperimentService}
     * after {@link Builder#build()} returns).
     */
    public Builder singleCell() {
        return new Builder( this, Modality.SINGLE_CELL );
    }

    /**
     * Modality of the experiment being built.
     */
    public enum Modality {
        BULK_RNA,
        SINGLE_CELL
    }

    /**
     * Fluent builder for an {@link ExpressionExperiment}. All
     * {@code withXxx(...)} methods return {@code this} for chaining. The
     * builder is single-use; call {@link #build()} exactly once.
     */
    public static final class Builder {

        private final ExperimentFactory factory;
        private final Modality modality;

        @Nullable
        private ArrayDesign arrayDesign;
        @Nullable
        private Taxon taxon;
        @Nullable
        private String shortName;
        private int numSamples = DEFAULT_SAMPLES;
        private boolean includeRawQt;

        private Builder( ExperimentFactory factory, Modality modality ) {
            this.factory = factory;
            this.modality = modality;
            // bulk EEs get a preferred raw QT by default; single-cell get
            // none (the SC vector workflow attaches its own).
            this.includeRawQt = ( modality == Modality.BULK_RNA );
        }

        public Builder withSamples( int n ) {
            if ( n < 1 ) {
                throw new IllegalArgumentException( "withSamples requires n >= 1, got " + n );
            }
            this.numSamples = n;
            return this;
        }

        public Builder withArrayDesign( ArrayDesign ad ) {
            this.arrayDesign = ad;
            return this;
        }

        public Builder withTaxon( Taxon t ) {
            this.taxon = t;
            return this;
        }

        public Builder withShortName( String name ) {
            this.shortName = name;
            return this;
        }

        public Builder includeRawDataQt( boolean v ) {
            this.includeRawQt = v;
            return this;
        }

        /**
         * Build, persist, and return the EE. Goes through
         * {@link ExpressionExperimentService#create(ExpressionExperiment)} so
         * the ACL-creation listener fires.
         */
        public ExpressionExperiment build() {
            Taxon t = ( this.taxon != null )
                    ? this.taxon
                    : factory.requireTaxon( DEFAULT_TAXON_COMMON_NAME );

            ArrayDesign ad = ( this.arrayDesign != null )
                    ? this.arrayDesign
                    : factory.createDefaultArrayDesign( t );

            ExternalDatabase geo = factory.externalDatabaseService.findByName( ExternalDatabases.GEO );
            if ( geo == null ) {
                throw new IllegalStateException(
                        "ExternalDatabase '" + ExternalDatabases.GEO + "' not found; init-entities.sql not loaded?" );
            }

            ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
            String shortNm = ( this.shortName != null )
                    ? this.shortName
                    : RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH );
            ee.setShortName( shortNm );
            ee.setName( "Expression Experiment " + shortNm );
            ee.setDescription( "A test expression experiment (ExperimentFactory, " + modality + ")" );
            ee.setSource( "https://www.ncbi.nlm.nih.gov/geo/" );
            ee.setTaxon( t );

            DatabaseEntry acc = DatabaseEntry.Factory.newInstance();
            acc.setAccession( RandomStringUtils.insecure().nextAlphanumeric( RANDOM_STRING_LENGTH ) );
            acc.setExternalDatabase( geo );
            ee.setAccession( acc );

            // ExperimentalDesign is mandatory (1:1, cascade ALL on EE).
            ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
            ed.setName( "ED " + shortNm );
            ed.setDescription( "Test experimental design" );
            ee.setExperimentalDesign( ed );

            // BioMaterials + BioAssays. The HBM mapping does NOT cascade from
            // EE.bioAssays → BioAssay.sampleUsed (BioMaterial), so the BM must
            // be persistent BEFORE the EE.create() session sees the graph.
            // BioAssayFactory.buildTransient() persists the BM (and AD if it
            // had to create one) but leaves the BA transient so EE's cascade
            // handles it. BAs cascade from EE.
            for ( int i = 0; i < this.numSamples; i++ ) {
                BioMaterial bm = BioMaterial.Factory.newInstance();
                bm.setName( shortNm + "_bm_" + i );
                bm.setSourceTaxon( t );
                bm = factory.bioMaterialService.create( bm );

                BioAssay ba = factory.bioAssayFactory.builder()
                        .withName( shortNm + "_ba_" + i )
                        .withBioMaterial( bm )
                        .withArrayDesign( ad )
                        .withTaxon( t )
                        .buildTransient();

                ee.getBioAssays().add( ba );
            }
            ee.setNumberOfSamples( this.numSamples );

            if ( this.includeRawQt ) {
                ee.getQuantitationTypes().add( factory.preferredRawQt() );
            }

            return factory.expressionExperimentService.create( ee );
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

    private ArrayDesign createDefaultArrayDesign( Taxon primaryTaxon ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "arrayDesign_" + RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) );
        ad.setShortName( "AD_" + RandomStringUtils.insecure().nextAlphabetic( 5 ) );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        ad.setPrimaryTaxon( primaryTaxon );
        return arrayDesignService.create( ad );
    }

    private QuantitationType preferredRawQt() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( RandomStringUtils.insecure().nextNumeric( RANDOM_STRING_LENGTH ) + "_testqt" );
        qt.setDescription( "Test raw QT (preferred)" );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LINEAR );
        qt.setIsBackground( false );
        qt.setIsBackgroundSubtracted( false );
        qt.setIsNormalized( false );
        qt.setIsRatio( false );
        qt.setIsMaskedPreferred( false );
        qt.setIsPreferred( true );
        return qt;
    }
}
