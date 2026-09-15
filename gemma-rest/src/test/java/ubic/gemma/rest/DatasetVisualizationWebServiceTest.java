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
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private RawExpressionDataVectorService rawExpressionDataVectorService;
    @Mock
    private CompositeSequenceService compositeSequenceService;
    @Mock
    private GeneService geneService;
    @Mock
    private SVDService svdService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

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
                ee, Arrays.asList( 672L ), null, null, 0.01, null, 20, 20, "json", null, null, true );

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
                ee, null, Arrays.asList( 101L, 102L ), null, 0.01, null, 20, 20, "json", null, null, true );

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
                ee, null, null, null, 0.01, null, 20, 20, "json", null, null, true );

        assertThat( payload.getMatrix().getRowsCount() ).isEqualTo( 1 );
    }

    @Test
    public void testHeatmapBase64Encoding_roundTrips() {
        double[] row = { 1.5, -2.25, 3.125, 0.0 };
        when( processedExpressionDataVectorService.getRandomProcessedDataArrays( any( ExpressionExperiment.class ), org.mockito.ArgumentMatchers.eq( 20 ) ) )
                .thenReturn( Collections.singletonList(
                        buildVector( probe1, row, Collections.emptyList(), null ) ) );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, null, null, null, 0.01, null, 20, 20, "base64f32", null, null, true );

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
                ee, Arrays.asList( 672L ), null, null, 0.01, null, 20, 20, "json", null, null, true );

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

    @Test
    public void testHeatmapSubSet_columnsAndMatrixFilteredToSubsetBioAssays() {
        // Subset contains BAs 8001 and 8003 (samples i=1 and i=3 → columns 1 and 3 of the full dim).
        ExpressionExperimentSubSet subSet = new ExpressionExperimentSubSet();
        subSet.setId( 555L );
        subSet.setSourceExperiment( ee );
        Set<BioAssay> subBas = new HashSet<>();
        subBas.add( bioAssays.get( 1 ) );
        subBas.add( bioAssays.get( 3 ) );
        subSet.setBioAssays( subBas );
        when( expressionExperimentSubSetService.loadWithBioAssays( 555L ) ).thenReturn( subSet );

        Collection<DoubleVectorValueObject> vectors = Arrays.asList(
                buildVector( probe1, new double[] { 1.0, 2.0, 3.0, 4.0 }, Collections.emptyList(), null ),
                buildVector( probe2, new double[] { 5.0, 6.0, 7.0, 8.0 }, Collections.emptyList(), null ) );
        when( processedExpressionDataVectorService.getProcessedDataArrays( any( ExpressionExperiment.class ), any( Collection.class ) ) )
                .thenReturn( vectors );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, Arrays.asList( 672L ), null, null, 0.01, null, 20, 20, "json", 555L, null, true );

        // Column axis shrank to 2 entries — the BAs that belong to the subset.
        assertThat( payload.getMatrix().getColsCount() ).isEqualTo( 2 );
        assertThat( payload.getMatrix().getRowsCount() ).isEqualTo( 2 );
        assertThat( payload.getColumns() ).hasSize( 2 );
        assertThat( payload.getColumns().get( 0 ).getBioAssayId() ).isEqualTo( 8001L );
        assertThat( payload.getColumns().get( 1 ).getBioAssayId() ).isEqualTo( 8003L );

        // Matrix cells project the kept source-column indices (1 and 3) into output positions 0 and 1.
        double[][] mat = ( double[][] ) payload.getMatrix().getValues();
        assertThat( mat[0] ).containsExactly( 2.0, 4.0 );
        assertThat( mat[1] ).containsExactly( 6.0, 8.0 );
    }

    @Test
    public void testHeatmapSubSet_rejectsForeignSubset() {
        ExpressionExperiment otherEe = ExpressionExperiment.Factory.newInstance();
        otherEe.setId( 99999L );
        ExpressionExperimentSubSet subSet = new ExpressionExperimentSubSet();
        subSet.setId( 777L );
        subSet.setSourceExperiment( otherEe );
        subSet.setBioAssays( new HashSet<>( bioAssays ) );
        when( expressionExperimentSubSetService.loadWithBioAssays( 777L ) ).thenReturn( subSet );

        try {
            heatmapDataService.buildHeatmapData(
                    ee, null, null, null, 0.01, null, 20, 20, "json", 777L, null, true );
            assertThat( false ).as( "expected IllegalArgumentException" ).isTrue();
        } catch ( IllegalArgumentException expected ) {
            assertThat( expected.getMessage() ).contains( "does not belong to dataset" );
        }
    }

    private QuantitationType rawCountsQt( long id ) {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setId( id );
        qt.setName( "Counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        return qt;
    }

    private RawExpressionDataVector rawVector( CompositeSequence probe, QuantitationType qt, double[] data ) {
        RawExpressionDataVector v = RawExpressionDataVector.Factory.newInstance();
        v.setId( 900000L + probe.getId() );
        v.setDesignElement( probe );
        v.setBioAssayDimension( bad );
        v.setQuantitationType( qt );
        v.setDataAsDoubles( data );
        return v;
    }

    private ProcessedExpressionDataVector processedVector( CompositeSequence probe, QuantitationType qt, double[] data ) {
        ProcessedExpressionDataVector v = ProcessedExpressionDataVector.Factory.newInstance();
        v.setId( 800000L + probe.getId() );
        v.setDesignElement( probe );
        v.setBioAssayDimension( bad );
        v.setQuantitationType( qt );
        v.setDataAsDoubles( data );
        return v;
    }

    @Test
    public void testHeatmapNonProcessedQt_servesFromRawVectors() {
        QuantitationType countsQt = rawCountsQt( 5555L );
        // No processed QT resolvable → the requested QT is treated as non-processed and routed to the raw path.
        when( expressionExperimentService.getProcessedQuantitationType( ee ) ).thenReturn( Optional.empty() );
        when( compositeSequenceService.load( any( Collection.class ) ) ).thenReturn( Arrays.asList( probe1, probe2 ) );
        when( rawExpressionDataVectorService.find( any( Collection.class ), any( QuantitationType.class ) ) )
                .thenReturn( Arrays.asList(
                        rawVector( probe1, countsQt, new double[] { 10.0, 20.0, 30.0, 40.0 } ),
                        rawVector( probe2, countsQt, new double[] { 1.0, 2.0, 3.0, 4.0 } ) ) );
        when( compositeSequenceService.getGenes( any( Collection.class ), eq( true ) ) )
                .thenReturn( Collections.singletonMap( probe1, Collections.singletonList( brca1 ) ) );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, null, Arrays.asList( 101L, 102L ), null, 0.01, null, 20, 20, "json", null, countsQt, true );

        assertThat( payload.getMatrix().getRowsCount() ).isEqualTo( 2 );
        assertThat( payload.getMatrix().getColsCount() ).isEqualTo( 4 );
        double[][] mat = ( double[][] ) payload.getMatrix().getValues();
        assertThat( mat[0] ).containsExactly( 10.0, 20.0, 30.0, 40.0 );
        // QT metadata carries the requested (non-processed) QT.
        assertThat( payload.getQuantitationType().getName() ).isEqualTo( "Counts" );
        // Gene mapping still populates row metadata.
        assertThat( payload.getRows().get( 0 ).getGenes() ).hasSize( 1 );
        assertThat( payload.getRows().get( 0 ).getGenes().get( 0 ).getId() ).isEqualTo( 672L );
    }

    @Test
    public void testHeatmapNonProcessedQt_rejectsResultSetMode() {
        QuantitationType countsQt = rawCountsQt( 5555L );
        when( expressionExperimentService.getProcessedQuantitationType( ee ) ).thenReturn( Optional.empty() );
        try {
            heatmapDataService.buildHeatmapData(
                    ee, null, null, 42L, 0.01, null, 20, 20, "json", null, countsQt, true );
            assertThat( false ).as( "expected IllegalArgumentException" ).isTrue();
        } catch ( IllegalArgumentException expected ) {
            assertThat( expected.getMessage() ).contains( "resultSet" );
        }
    }

    @Test
    public void testHeatmapNonProcessedQt_servesRandomFallbackFromRawVectors() {
        // A non-processed QT with no gene/probe selection falls back to a DB-side random sample of its raw vectors
        // (see commit "allow sampleSize for alternate qt heatmap-data outputs").
        QuantitationType countsQt = rawCountsQt( 5555L );
        when( expressionExperimentService.getProcessedQuantitationType( ee ) ).thenReturn( Optional.empty() );
        when( rawExpressionDataVectorService.getRandomRawVectors( any( QuantitationType.class ), eq( 20 ) ) )
                .thenReturn( Collections.singletonList(
                        rawVector( probe1, countsQt, new double[] { 10.0, 20.0, 30.0, 40.0 } ) ) );
        when( compositeSequenceService.getGenes( any( Collection.class ), eq( true ) ) )
                .thenReturn( Collections.emptyMap() );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, null, null, null, 0.01, null, 20, 20, "json", null, countsQt, true );

        assertThat( payload.getMatrix().getRowsCount() ).isEqualTo( 1 );
        assertThat( payload.getMatrix().getColsCount() ).isEqualTo( 4 );
        assertThat( payload.getQuantitationType().getName() ).isEqualTo( "Counts" );
    }

    @Test
    public void testHeatmapNonProcessedQt_maskOutliersFalse_returnsStoredOutlierValues() {
        // Flag sample i=2 (column index 2) as an outlier on the shared dimension.
        bioAssays.get( 2 ).setIsOutlier( true );

        QuantitationType countsQt = rawCountsQt( 5555L );
        when( expressionExperimentService.getProcessedQuantitationType( ee ) ).thenReturn( Optional.empty() );
        when( compositeSequenceService.load( any( Collection.class ) ) ).thenReturn( Arrays.asList( probe1 ) );
        when( rawExpressionDataVectorService.find( any( Collection.class ), any( QuantitationType.class ) ) )
                .thenReturn( Collections.singletonList(
                        rawVector( probe1, countsQt, new double[] { 10.0, 20.0, 30.0, 40.0 } ) ) );
        when( compositeSequenceService.getGenes( any( Collection.class ), eq( true ) ) )
                .thenReturn( Collections.emptyMap() );

        // maskOutliers = false → the stored value 30.0 at the outlier column is preserved.
        HeatmapDataValueObject unmasked = heatmapDataService.buildHeatmapData(
                ee, null, Arrays.asList( 101L ), null, 0.01, null, 20, 20, "json", null, countsQt, false );
        double[][] unmaskedMat = ( double[][] ) unmasked.getMatrix().getValues();
        assertThat( unmaskedMat[0] ).containsExactly( 10.0, 20.0, 30.0, 40.0 );

        // maskOutliers = true → the outlier column is NaN, the rest untouched.
        HeatmapDataValueObject masked = heatmapDataService.buildHeatmapData(
                ee, null, Arrays.asList( 101L ), null, 0.01, null, 20, 20, "json", null, countsQt, true );
        double[][] maskedMat = ( double[][] ) masked.getMatrix().getValues();
        assertThat( maskedMat[0][0] ).isEqualTo( 10.0 );
        assertThat( maskedMat[0][2] ).isNaN();
        assertThat( maskedMat[0][3] ).isEqualTo( 40.0 );
    }

    @Test
    public void testHeatmapProcessedQt_maskOutliersFalse_restoresFromStoredEntityWhenValuePresent() {
        // Processed path with maskOutliers=false re-fetches the stored entity and restores outlier columns from it.
        // NOTE: today's processed-data creation masks outliers on disk, so in production find(...) returns a NaN at the
        // outlier column and this path is a no-op. This test exercises the restoration LOGIC for the scenario where the
        // stored value survives (processing changed to not mask on disk, or a post-flag reprocess that failed): the
        // mocked entity carries the real value 2.0, which must land in the output.
        bioAssays.get( 1 ).setIsOutlier( true );

        // The processed cache serves the vector masked at the outlier column (read-time masking).
        when( processedExpressionDataVectorService.getProcessedDataArraysByProbe( any( ExpressionExperiment.class ), any( Collection.class ) ) )
                .thenReturn( Collections.singletonList(
                        buildVector( probe1, new double[] { 1.0, Double.NaN, 3.0, 4.0 }, Collections.emptyList(), null ) ) );
        when( compositeSequenceService.load( any( Collection.class ) ) ).thenReturn( Arrays.asList( probe1 ) );

        QuantitationType processedQt = rawCountsQt( 4242L );
        when( expressionExperimentService.getProcessedQuantitationType( ee ) ).thenReturn( Optional.of( processedQt ) );
        when( processedExpressionDataVectorService.find( any( Collection.class ), any( QuantitationType.class ) ) )
                .thenReturn( Collections.singletonList(
                        processedVector( probe1, processedQt, new double[] { 1.0, 2.0, 3.0, 4.0 } ) ) );

        HeatmapDataValueObject unmasked = heatmapDataService.buildHeatmapData(
                ee, null, Arrays.asList( 101L ), null, 0.01, null, 20, 20, "json", null, null, false );
        double[][] mat = ( double[][] ) unmasked.getMatrix().getValues();
        assertThat( mat[0] ).containsExactly( 1.0, 2.0, 3.0, 4.0 );
    }

    @Test
    public void testHeatmapProcessedQt_maskOutliersFalse_noOpWhenStoredValueMasked() {
        // Mirrors today's production reality: the stored entity is ALSO NaN at the outlier column, so restoration
        // leaves it NaN (restores NaN over NaN).
        bioAssays.get( 1 ).setIsOutlier( true );
        when( processedExpressionDataVectorService.getProcessedDataArraysByProbe( any( ExpressionExperiment.class ), any( Collection.class ) ) )
                .thenReturn( Collections.singletonList(
                        buildVector( probe1, new double[] { 1.0, Double.NaN, 3.0, 4.0 }, Collections.emptyList(), null ) ) );
        when( compositeSequenceService.load( any( Collection.class ) ) ).thenReturn( Arrays.asList( probe1 ) );

        QuantitationType processedQt = rawCountsQt( 4242L );
        when( expressionExperimentService.getProcessedQuantitationType( ee ) ).thenReturn( Optional.of( processedQt ) );
        when( processedExpressionDataVectorService.find( any( Collection.class ), any( QuantitationType.class ) ) )
                .thenReturn( Collections.singletonList(
                        processedVector( probe1, processedQt, new double[] { 1.0, Double.NaN, 3.0, 4.0 } ) ) );

        HeatmapDataValueObject payload = heatmapDataService.buildHeatmapData(
                ee, null, Arrays.asList( 101L ), null, 0.01, null, 20, 20, "json", null, null, false );
        double[][] mat = ( double[][] ) payload.getMatrix().getValues();
        assertThat( mat[0][1] ).isNaN();
    }
}
