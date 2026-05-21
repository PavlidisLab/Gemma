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
package ubic.gemma.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HeatmapDataService} (the assembler that backs
 * {@link DatasetVisualizationWebService}). Stays out of the JerseyTest5 + Spring-context
 * integration path so it runs in the default {@code mvn -pl gemma-rest test} surface without
 * gemdtest, mirroring the rest-module unit-test pattern.
 * <p>
 * Verifies for each major param mode: payload shape, row metadata, factor catalogue with
 * statements + measurements, and base64f32 encoding round-trip.
 *
 * @author claude
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DatasetVisualizationWebServiceTest {

    @Mock
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Mock
    private CompositeSequenceService compositeSequenceService;
    @Mock
    private GeneService geneService;
    @Mock
    private SVDService svdService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @InjectMocks
    private HeatmapDataService heatmapDataService;

    private ExpressionExperiment ee;
    private BioAssayDimension bad;
    private List<BioAssay> bioAssays;
    private List<BioMaterial> bioMaterials;
    private ExperimentalFactor categoricalFactor;
    private ExperimentalFactor continuousFactor;
    private FactorValue treatedFv;
    private FactorValue controlFv;
    private CompositeSequence probe1;
    private CompositeSequence probe2;
    private CompositeSequence probe3;
    private Gene brca1;

    @BeforeEach
    public void setUpFixture() {
        // 3 probes × 4 samples × 1 categorical factor × 2 levels + 1 continuous factor.
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( 1L );
        ad.setShortName( "GPL_TEST" );

        probe1 = CompositeSequence.Factory.newInstance();
        probe1.setId( 101L );
        probe1.setName( "ILMN_1" );
        probe1.setArrayDesign( ad );
        probe2 = CompositeSequence.Factory.newInstance();
        probe2.setId( 102L );
        probe2.setName( "ILMN_2" );
        probe2.setArrayDesign( ad );
        probe3 = CompositeSequence.Factory.newInstance();
        probe3.setId( 103L );
        probe3.setName( "ILMN_3" );
        probe3.setArrayDesign( ad );

        brca1 = Gene.Factory.newInstance();
        brca1.setId( 672L );
        brca1.setOfficialSymbol( "BRCA1" );
        brca1.setOfficialName( "BRCA1 DNA repair associated" );

        // Factor: tissue (categorical) with treated/control levels.
        categoricalFactor = ExperimentalFactor.Factory.newInstance( "tissue", FactorType.categorical );
        categoricalFactor.setId( 78L );
        Characteristic cat = Characteristic.Factory.newInstance();
        cat.setCategory( "OrganismPart" );
        cat.setCategoryUri( "http://purl.obolibrary.org/obo/CARO_0000000" );
        categoricalFactor.setCategory( cat );

        treatedFv = FactorValue.Factory.newInstance();
        treatedFv.setId( 1234L );
        treatedFv.setExperimentalFactor( categoricalFactor );
        Characteristic treatedC = Characteristic.Factory.newInstance();
        treatedC.setCategory( "OrganismPart" );
        treatedC.setValue( "liver" );
        treatedFv.getCharacteristics().add(
                ubic.gemma.model.expression.experiment.Statement.Factory.newInstance( treatedC ) );

        controlFv = FactorValue.Factory.newInstance();
        controlFv.setId( 1235L );
        controlFv.setExperimentalFactor( categoricalFactor );
        Characteristic controlC = Characteristic.Factory.newInstance();
        controlC.setCategory( "OrganismPart" );
        controlC.setValue( "kidney" );
        controlFv.getCharacteristics().add(
                ubic.gemma.model.expression.experiment.Statement.Factory.newInstance( controlC ) );
        controlFv.setIsBaseline( true );

        categoricalFactor.getFactorValues().add( treatedFv );
        categoricalFactor.getFactorValues().add( controlFv );
        categoricalFactor.setBaselineRelevance( "required" );

        // Factor: age (continuous), per-sample measurements via the FV's measurement field.
        continuousFactor = ExperimentalFactor.Factory.newInstance( "age", FactorType.continuous );
        continuousFactor.setId( 92L );
        continuousFactor.setBaselineRelevance( "not_applicable" );
        continuousFactor.setBaselineRelevanceReason( "Continuous factors have no baseline." );

        // Build 4 BioMaterials, each with the categorical FV and a unique continuous FV (age value).
        bioMaterials = new ArrayList<>();
        bioAssays = new ArrayList<>();
        double[] ages = { 24.0, 36.0, 48.0, 60.0 };
        for ( int i = 0; i < 4; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setId( 7000L + i );
            bm.setName( "BM" + i );

            // FV assignment for categorical factor: alternate treated/control.
            FactorValue fv = ( i % 2 == 0 ) ? controlFv : treatedFv;
            bm.getFactorValues().add( fv );

            // Per-sample continuous FV.
            FactorValue ageFv = FactorValue.Factory.newInstance();
            ageFv.setId( 5000L + i );
            ageFv.setExperimentalFactor( continuousFactor );
            Measurement m = Measurement.Factory.newInstance();
            m.setId( 6000L + i );
            m.setValue( String.valueOf( ages[i] ) );
            ageFv.setMeasurement( m );
            continuousFactor.getFactorValues().add( ageFv );
            bm.getFactorValues().add( ageFv );
            bioMaterials.add( bm );

            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setId( 8000L + i );
            ba.setName( "GSM" + ( 12345 + i ) );
            ba.setSampleUsed( bm );
            ba.setIsOutlier( false );
            ba.setArrayDesignUsed( ad );
            bioAssays.add( ba );
        }

        bad = BioAssayDimension.Factory.newInstance();
        bad.setId( 9000L );
        bad.setBioAssays( bioAssays );

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setId( 5L );
        Set<ExperimentalFactor> efs = new HashSet<>();
        efs.add( categoricalFactor );
        efs.add( continuousFactor );
        ed.setExperimentalFactors( efs );

        ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 12345L );
        ee.setShortName( "GSE6789" );
        ee.setExperimentalDesign( ed );
        ee.setBioAssays( new HashSet<>( bioAssays ) );

        when( expressionExperimentService.loadAndThawLite( 12345L ) ).thenReturn( ee );
    }

    private DoubleVectorValueObject buildVector( CompositeSequence probe, double[] data, Collection<Long> geneIds, Double pvalue ) {
        DoubleVectorValueObject v = new DoubleVectorValueObject();
        v.setId( probe.getId() );
        // Build the design-element VO with a stub array design so we avoid the ACL-aware
        // ArrayDesignValueObject(ArrayDesign) path (it pulls a SecurityContext that the
        // unit-test JVM doesn't carry).
        CompositeSequenceValueObject csvo = new CompositeSequenceValueObject( probe.getId() );
        csvo.setName( probe.getName() );
        v.setDesignElement( csvo );
        // Hand-build the BioAssayDimensionValueObject so we avoid the entity-aware constructor's
        // ArrayDesignValueObject construction path; we only need the BioAssay list in original order.
        BioAssayDimensionValueObject badVo = new BioAssayDimensionValueObject( bad.getId() );
        List<BioAssayValueObject> bavos = new ArrayList<>( bad.getBioAssays().size() );
        for ( BioAssay ba : bad.getBioAssays() ) {
            BioAssayValueObject bavo = new BioAssayValueObject( ba.getId() );
            bavo.setName( ba.getName() );
            bavo.setOutlier( ba.getIsOutlier() );
            bavos.add( bavo );
        }
        badVo.addBioAssays( bavos );
        v.setBioAssayDimension( badVo );
        v.setData( data );
        v.setGenes( geneIds );
        v.setPvalue( pvalue );
        return v;
    }

    @Test
    public void testHeatmapByGenes_shapesAndFactorCoverage() {
        Collection<DoubleVectorValueObject> vectors = Arrays.asList(
                buildVector( probe1, new double[] { 1.0, 2.0, 3.0, 4.0 }, Collections.singleton( 672L ), null ),
                buildVector( probe2, new double[] { 0.5, 0.6, 0.7, 0.8 }, Collections.singleton( 672L ), null ),
                buildVector( probe3, new double[] { 5.0, 6.0, 7.0, 8.0 }, Collections.emptyList(), null ) );
        when( processedExpressionDataVectorService.getProcessedDataArrays( any( ExpressionExperiment.class ), any( Collection.class ) ) )
                .thenReturn( vectors );
        when( geneService.loadThawedLiter( any( Collection.class ) ) ).thenReturn( Collections.singletonList( brca1 ) );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, Arrays.asList( 672L ), null, null, 0.01, null, 20, 20, "json" );

        assertThat( payload.getDatasetId() ).isEqualTo( 12345L );
        assertThat( payload.getDatasetShortName() ).isEqualTo( "GSE6789" );
        assertThat( payload.getMatrix().getRowsCount() ).isEqualTo( 3 );
        assertThat( payload.getMatrix().getColsCount() ).isEqualTo( 4 );
        assertThat( payload.getMatrix().getEncoding() ).isEqualTo( "json" );
        assertThat( payload.getMatrix().getValues() ).isInstanceOf( double[][].class );
        double[][] mat = ( double[][] ) payload.getMatrix().getValues();
        assertThat( mat[0] ).containsExactly( 1.0, 2.0, 3.0, 4.0 );
        assertThat( mat[2] ).containsExactly( 5.0, 6.0, 7.0, 8.0 );

        // Row metadata: probes 101/102 have BRCA1, probe 103 has no genes.
        assertThat( payload.getRows() ).hasSize( 3 );
        assertThat( payload.getRows().get( 0 ).getDesignElementId() ).isEqualTo( 101L );
        assertThat( payload.getRows().get( 0 ).getDesignElementName() ).isEqualTo( "ILMN_1" );
        assertThat( payload.getRows().get( 0 ).getGenes() ).hasSize( 1 );
        assertThat( payload.getRows().get( 0 ).getGenes().get( 0 ).getId() ).isEqualTo( 672L );
        assertThat( payload.getRows().get( 0 ).getGenes().get( 0 ).getOfficialSymbol() ).isEqualTo( "BRCA1" );
        assertThat( payload.getRows().get( 0 ).getGenes().get( 0 ).getName() ).isEqualTo( "BRCA1 DNA repair associated" );
        assertThat( payload.getRows().get( 2 ).getGenes() ).isNullOrEmpty();
        // Non-diffex: validated and pvalue both null.
        assertThat( payload.getRows().get( 0 ).getValidated() ).isNull();
        assertThat( payload.getRows().get( 0 ).getPvalue() ).isNull();

        // Column metadata: 4 columns, each carrying bioAssayId + factorValueId for the categorical factor.
        assertThat( payload.getColumns() ).hasSize( 4 );
        assertThat( payload.getColumns().get( 0 ).getBioAssayId() ).isEqualTo( 8000L );
        assertThat( payload.getColumns().get( 0 ).getFactorValueIds() ).containsEntry( 78L, 1235L ); // control
        assertThat( payload.getColumns().get( 1 ).getFactorValueIds() ).containsEntry( 78L, 1234L ); // treated
        // Continuous factor IDs should NOT appear in the column.factorValueIds map.
        assertThat( payload.getColumns().get( 0 ).getFactorValueIds() ).doesNotContainKey( 92L );

        // Factors[] catalogue: both categorical and continuous, with statements + measurements.
        assertThat( payload.getFactors() ).hasSize( 2 );
        HeatmapDataValueObject.FactorEntry catEntry = findFactor( payload, 78L );
        assertThat( catEntry ).isNotNull();
        assertThat( catEntry.getFactor().getType() ).isEqualTo( "categorical" );
        assertThat( catEntry.getFactor().getBaselineRelevance() ).isEqualTo( "required" );
        assertThat( catEntry.getFactor().getValues() ).hasSize( 2 );
        // Each FV carries its statements.
        assertThat( catEntry.getFactor().getValues() )
                .allSatisfy( fv -> assertThat( fv.getStatements() ).isNotNull().isNotEmpty() );
        assertThat( catEntry.getMeasurements() ).isNull();

        HeatmapDataValueObject.FactorEntry contEntry = findFactor( payload, 92L );
        assertThat( contEntry ).isNotNull();
        assertThat( contEntry.getFactor().getType() ).isEqualTo( "continuous" );
        assertThat( contEntry.getFactor().getBaselineRelevance() ).isEqualTo( "not_applicable" );
        // Per-sample measurements: keyed by bioAssayId, one entry per of 4 samples.
        assertThat( contEntry.getMeasurements() ).hasSize( 4 );
        assertThat( contEntry.getMeasurements() ).containsEntry( 8000L, 24.0 );
        assertThat( contEntry.getMeasurements() ).containsEntry( 8003L, 60.0 );
    }

    @Test
    public void testHeatmapByProbes_routesThroughProbeSelector() {
        when( compositeSequenceService.load( any( Collection.class ) ) )
                .thenReturn( Arrays.asList( probe1, probe2 ) );
        List<DoubleVectorValueObject> vectors = Arrays.asList(
                buildVector( probe1, new double[] { 1.0, 2.0, 3.0, 4.0 }, Collections.emptyList(), null ),
                buildVector( probe2, new double[] { 5.0, 6.0, 7.0, 8.0 }, Collections.emptyList(), null ) );
        when( processedExpressionDataVectorService.getProcessedDataArraysByProbe( any( ExpressionExperiment.class ), any( Collection.class ) ) )
                .thenReturn( vectors );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, null, Arrays.asList( 101L, 102L ), null, 0.01, null, 20, 20, "json" );

        assertThat( payload.getMatrix().getRowsCount() ).isEqualTo( 2 );
        assertThat( payload.getMatrix().getColsCount() ).isEqualTo( 4 );
        assertThat( payload.getRows() ).hasSize( 2 );
    }

    @Test
    public void testHeatmapDefault_routesThroughRandomSample() {
        when( processedExpressionDataVectorService.getRandomProcessedDataArrays( any( ExpressionExperiment.class ), org.mockito.ArgumentMatchers.eq( 20 ) ) )
                .thenReturn( Collections.singletonList(
                        buildVector( probe1, new double[] { 1.0, 2.0, 3.0, 4.0 }, Collections.emptyList(), null ) ) );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, null, null, null, 0.01, null, 20, 20, "json" );

        assertThat( payload.getMatrix().getRowsCount() ).isEqualTo( 1 );
    }

    @Test
    public void testHeatmapBase64Encoding_roundTrips() {
        double[] row = { 1.5, -2.25, 3.125, 0.0 };
        when( processedExpressionDataVectorService.getRandomProcessedDataArrays( any( ExpressionExperiment.class ), org.mockito.ArgumentMatchers.eq( 20 ) ) )
                .thenReturn( Collections.singletonList(
                        buildVector( probe1, row, Collections.emptyList(), null ) ) );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, null, null, null, 0.01, null, 20, 20, "base64f32" );

        assertThat( payload.getMatrix().getEncoding() ).isEqualTo( "base64f32" );
        assertThat( payload.getMatrix().getValues() ).isInstanceOf( String.class );
        String b64 = ( String ) payload.getMatrix().getValues();
        double[][] decoded = HeatmapDataService.decodeBase64Float32(
                b64, payload.getMatrix().getRowsCount(), payload.getMatrix().getColsCount() );
        // float32 → double round-trip: tolerate ~1e-6 precision loss.
        assertThat( decoded[0][0] ).isCloseTo( 1.5, org.assertj.core.data.Offset.offset( 1e-6 ) );
        assertThat( decoded[0][1] ).isCloseTo( -2.25, org.assertj.core.data.Offset.offset( 1e-6 ) );
        assertThat( decoded[0][2] ).isCloseTo( 3.125, org.assertj.core.data.Offset.offset( 1e-6 ) );
        assertThat( decoded[0][3] ).isCloseTo( 0.0, org.assertj.core.data.Offset.offset( 1e-6 ) );
    }

    @Test
    public void testHeatmapFactorsHaveStatementsAndMeasurements() {
        // Reuse the genes path to get a populated factors[] block. Same fixture as the first test;
        // this isolates the assertion that statements + measurements both arrive on the wire.
        Collection<DoubleVectorValueObject> vectors = Collections.singletonList(
                buildVector( probe1, new double[] { 1.0, 2.0, 3.0, 4.0 }, Collections.singleton( 672L ), null ) );
        when( processedExpressionDataVectorService.getProcessedDataArrays( any( ExpressionExperiment.class ), any( Collection.class ) ) )
                .thenReturn( vectors );
        when( geneService.loadThawedLiter( any( Collection.class ) ) ).thenReturn( Collections.singletonList( brca1 ) );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, Arrays.asList( 672L ), null, null, 0.01, null, 20, 20, "json" );

        assertThat( payload.getFactors() ).hasSize( 2 );
        for ( HeatmapDataValueObject.FactorEntry fe : payload.getFactors() ) {
            assertThat( fe.getFactor() ).isNotNull();
            assertThat( fe.getFactor().getId() ).isNotNull();
            assertThat( fe.getFactor().getValues() ).isNotEmpty();
            if ( "continuous".equals( fe.getFactor().getType() ) ) {
                assertThat( fe.getMeasurements() ).isNotNull().isNotEmpty();
            } else {
                // categorical FVs carry statements (mirrors curation-ui Factor.factor_values[].statements).
                assertThat( fe.getFactor().getValues() )
                        .allSatisfy( fv -> assertThat( fv.getStatements() ).isNotNull() );
            }
        }
    }

    private HeatmapDataValueObject.FactorEntry findFactor( HeatmapDataValueObject payload, Long factorId ) {
        return payload.getFactors().stream()
                .filter( fe -> factorId.equals( fe.getFactor().getId() ) )
                .findFirst()
                .orElse( null );
    }
}
