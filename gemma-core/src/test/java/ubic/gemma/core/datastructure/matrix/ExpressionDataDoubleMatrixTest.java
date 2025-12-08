package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.experiment.RandomExpressionExperimentUtils.randomExpressionExperiment;

public class ExpressionDataDoubleMatrixTest {

    private static final Random random = new Random( 123L );

    @Test
    public void testSliceRows() {
        Taxon taxon = Taxon.Factory.newInstance( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad );
            ad.getCompositeSequences().add( cs );
        }
        ExpressionExperiment ee = randomExpressionExperiment( taxon, 8, ad );
        ExpressionDataDoubleMatrix matrix = RandomExpressionDataMatrixUtils.randomCountMatrix( ee );

        List<CompositeSequence> rowsToKeep = matrix.getDesignElements().stream()
                .sorted( Comparator.comparing( CompositeSequence::getName ) )
                .limit( 10 ).collect( Collectors.toList() );

        assertThat( matrix.sliceRows( rowsToKeep ) )
                .satisfies( slicedMatrix -> {
                    assertThat( slicedMatrix.rows() ).isEqualTo( 10 );
                    assertThat( slicedMatrix.columns() ).isEqualTo( 8 );
                    assertThat( slicedMatrix.getDesignElements() )
                            .containsExactlyElementsOf( rowsToKeep );
                } );

        assertThat( matrix.sliceRows( Collections.emptyList() ) )
                .satisfies( sm -> {
                    assertThat( sm.getBioMaterials() ).hasSize( 8 );
                    assertThat( sm.getBioAssaysForColumn( 0 ) ).isEmpty();
                    assertThat( sm.getBioAssayDimensions() ).isEmpty();
                    assertThat( sm.getQuantitationTypes() ).isEmpty();
                    assertThat( sm.rows() ).isZero();
                } );
    }

    @Test
    public void testSliceColumns() {
        Taxon taxon = Taxon.Factory.newInstance( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad );
            ad.getCompositeSequences().add( cs );
        }
        ExpressionExperiment ee = randomExpressionExperiment( taxon, 8, ad );
        ExpressionDataDoubleMatrix matrix = RandomExpressionDataMatrixUtils.randomCountMatrix( ee );

        List<BioMaterial> samplesToKeep = matrix.getBioMaterials().stream()
                .sorted( Comparator.comparing( BioMaterial::getName ) )
                .limit( 2 ).collect( Collectors.toList() );

        assertThat( matrix.sliceColumns( samplesToKeep ) ).satisfies( slicedMatrix -> {
            assertThat( slicedMatrix.columns() ).isEqualTo( 2 );
            assertThat( slicedMatrix.rows() ).isEqualTo( 100 );
            assertThat( slicedMatrix.getBioMaterials() )
                    .containsExactlyElementsOf( samplesToKeep );
            assertThat( slicedMatrix.getBioAssayDimension().getBioAssays() )
                    .extracting( BioAssay::getSampleUsed )
                    .containsExactlyElementsOf( samplesToKeep );
        } );

        assertThat( matrix.sliceColumns( Collections.emptyList() ) )
                .satisfies( sm -> {
                    assertThat( sm.getBioAssayDimension().getBioAssays() )
                            .isEmpty();
                    assertThat( sm.columns() ).isZero();
                    assertThat( sm.rows() ).isEqualTo( 100 );
                } );
    }

    @Test
    public void testNumberOfCells() {
        Taxon taxon = Taxon.Factory.newInstance( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad );
            ad.getCompositeSequences().add( cs );
        }
        ExpressionExperiment ee = randomExpressionExperiment( taxon, 8, ad );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        RandomBulkDataUtils.setSeed( 123L );
        Collection<RawExpressionDataVector> vectors = RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class );
        // populate number of cells
        for ( RawExpressionDataVector vector : vectors ) {
            int[] noc = new int[vector.getBioAssayDimension().getBioAssays().size()];
            for ( int i = 0; i < noc.length; i++ ) {
                noc[i] = random.nextInt( 200 );
            }
            vector.setNumberOfCells( noc );
        }
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, vectors );
        assertThat( matrix.getNumberOfCells() )
                .isNotNull()
                .hasDimensions( 100, 8 );

        ExpressionDataDoubleMatrix slicedMatrix = new ExpressionDataDoubleMatrix( ee, vectors )
                .sliceRows( ad.getCompositeSequences().stream()
                        .sorted( Comparator.comparing( CompositeSequence::getName ) )
                        .limit( 10 ).collect( Collectors.toList() ) );
        assertThat( slicedMatrix.getNumberOfCells() )
                .isNotNull()
                .hasDimensions( 10, 8 );


        List<BioAssay> slicedAssays = ee.getBioAssays().stream()
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .limit( 2 ).collect( Collectors.toList() );
        List<BioMaterial> slicedSamples = slicedAssays.stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toList() );
        BioAssayDimension slicedBad = BioAssayDimension.Factory.newInstance( slicedAssays );
        ExpressionDataDoubleMatrix slicedMatrixByColumns = matrix.sliceColumns( slicedSamples, slicedBad );
        assertThat( slicedMatrixByColumns.getNumberOfCells() )
                .isNotNull()
                .hasDimensions( 100, 2 );
    }

    @Test
    public void testSlicingWhenNumberOfCellsIsNull() {
        Taxon taxon = Taxon.Factory.newInstance( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad );
            ad.getCompositeSequences().add( cs );
        }
        ExpressionExperiment ee = randomExpressionExperiment( taxon, 8, ad );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        RandomBulkDataUtils.setSeed( 123L );
        Collection<RawExpressionDataVector> vectors = RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class );
        // populate number of cells
        for ( RawExpressionDataVector vector : vectors ) {
            if ( random.nextBoolean() ) {
                int[] noc = new int[vector.getBioAssayDimension().getBioAssays().size()];
                for ( int i = 0; i < noc.length; i++ ) {
                    noc[i] = random.nextInt( 200 );
                }
                vector.setNumberOfCells( noc );
            }
        }
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, vectors );
        assertThat( matrix.getNumberOfCells() )
                .isNull();

        ExpressionDataDoubleMatrix slicedMatrix = new ExpressionDataDoubleMatrix( ee, vectors )
                .sliceRows( ad.getCompositeSequences().stream()
                        .sorted( Comparator.comparing( CompositeSequence::getName ) )
                        .limit( 10 ).collect( Collectors.toList() ) );
        assertThat( slicedMatrix.getNumberOfCells() )
                .isNull();


        List<BioAssay> slicedAssays = ee.getBioAssays().stream()
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .limit( 2 ).collect( Collectors.toList() );
        List<BioMaterial> slicedSamples = slicedAssays.stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toList() );
        BioAssayDimension slicedBad = BioAssayDimension.Factory.newInstance( slicedAssays );
        ExpressionDataDoubleMatrix slicedMatrixByColumns = matrix.sliceColumns( slicedSamples, slicedBad );
        assertThat( slicedMatrixByColumns.getNumberOfCells() )
                .isNull();
    }

    @Test
    public void testSlicingWhenNumberOfCellsIsPartiallyPopulated() {
        Taxon taxon = Taxon.Factory.newInstance( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        ad.setShortName( RandomStringUtils.insecure().nextAlphanumeric( 10 ) );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad );
            ad.getCompositeSequences().add( cs );
        }
        ExpressionExperiment ee = randomExpressionExperiment( taxon, 8, ad );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "counts" );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        RandomBulkDataUtils.setSeed( 123L );
        Collection<RawExpressionDataVector> vectors = RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class );
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( ee, vectors );
        assertThat( matrix.getNumberOfCells() )
                .isNull();

        ExpressionDataDoubleMatrix slicedMatrix = new ExpressionDataDoubleMatrix( ee, vectors )
                .sliceRows( ad.getCompositeSequences().stream()
                        .sorted( Comparator.comparing( CompositeSequence::getName ) )
                        .limit( 10 ).collect( Collectors.toList() ) );
        assertThat( slicedMatrix.getNumberOfCells() )
                .isNull();


        List<BioAssay> slicedAssays = ee.getBioAssays().stream()
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .limit( 2 ).collect( Collectors.toList() );
        List<BioMaterial> slicedSamples = slicedAssays.stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toList() );
        BioAssayDimension slicedBad = BioAssayDimension.Factory.newInstance( slicedAssays );
        ExpressionDataDoubleMatrix slicedMatrixByColumns = matrix.sliceColumns( slicedSamples, slicedBad );
        assertThat( slicedMatrixByColumns.getNumberOfCells() )
                .isNull();
    }
}