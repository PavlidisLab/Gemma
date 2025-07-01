package ubic.gemma.core.datastructure.matrix;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiAssayBulkExpressionDataMatrixTest {

    @Test
    public void testMultiAssay() {
        ExpressionExperiment ee = new ExpressionExperiment();
        List<BioMaterial> bms = new ArrayList<>();
        for ( int i = 0; i < 8; i++ )
            bms.add( BioMaterial.Factory.newInstance( "bm" + i ) );

        QuantitationType qt1 = new QuantitationType();
        qt1.setName( "qt1" );
        qt1.setGeneralType( GeneralType.QUANTITATIVE );
        qt1.setType( StandardQuantitationType.AMOUNT );
        qt1.setScale( ScaleType.LOG2 );
        qt1.setRepresentation( PrimitiveType.DOUBLE );
        ArrayDesign ad1 = new ArrayDesign();
        ad1.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs1" ) );
        BioAssayDimension bad1 = new BioAssayDimension();
        for ( BioMaterial bm : bms ) {
            bad1.getBioAssays().add( BioAssay.Factory.newInstance( "ba1" + bm.getName(), ad1, bm ) );
        }

        QuantitationType qt2 = new QuantitationType();
        qt2.setName( "qt2" );
        qt2.setGeneralType( GeneralType.QUANTITATIVE );
        qt2.setType( StandardQuantitationType.AMOUNT );
        qt2.setScale( ScaleType.LOG2 );
        qt2.setRepresentation( PrimitiveType.DOUBLE );
        ArrayDesign ad2 = new ArrayDesign();
        ad2.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs2" ) );
        BioAssayDimension bad2 = new BioAssayDimension();
        for ( BioMaterial bm : bms ) {
            bad2.getBioAssays().add( BioAssay.Factory.newInstance( "ba2" + bm.getName(), ad2, bm ) );
        }

        List<RawExpressionDataVector> vectors = new ArrayList<>();
        vectors.addAll( RandomBulkDataUtils.randomBulkVectors( ee, bad1, ad1, qt1, RawExpressionDataVector.class ) );
        vectors.addAll( RandomBulkDataUtils.randomBulkVectors( ee, bad2, ad2, qt2, RawExpressionDataVector.class ) );
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( vectors, Arrays.asList( qt1, qt2 ) );
        assertThat( matrix.getQuantitationTypes() )
                .containsExactlyInAnyOrder( qt1, qt2 );
        assertThat( matrix.getQuantitationType() ).satisfies( qt -> {
            assertThat( qt.getName() ).isEqualTo( "Merged from 2 quantitation types" );
        } );
        assertThatThrownBy( () -> matrix.getBioAssayForColumn( 0 ) )
                .isInstanceOf( IllegalStateException.class );
        assertThat( matrix.getBioAssaysForColumn( 0 ) )
                .hasSize( 2 );
        assertThat( matrix.getBioAssayDimensions() )
                .containsExactlyInAnyOrder( bad1, bad2 )
                .hasSize( 2 );
        assertThat( matrix.getBioAssayDimension().getBioAssays() )
                .hasSize( 8 );
        assertThat( matrix.columns() ).isEqualTo( 8 );
        assertThat( matrix.rows() ).isEqualTo( 2 );
        assertThat( BulkExpressionDataMatrixUtils.toVectors( matrix, RawExpressionDataVector.class ) )
                .hasSize( 2 );
    }

    @Test
    public void testMultiAssayWithUnequalSize() {
        ExpressionExperiment ee = new ExpressionExperiment();
        List<BioMaterial> bms = new ArrayList<>();
        for ( int i = 0; i < 8; i++ )
            bms.add( BioMaterial.Factory.newInstance( "bm" + i ) );

        QuantitationType qt1 = new QuantitationType();
        qt1.setName( "qt1" );
        qt1.setGeneralType( GeneralType.QUANTITATIVE );
        qt1.setType( StandardQuantitationType.AMOUNT );
        qt1.setScale( ScaleType.LOG2 );
        qt1.setRepresentation( PrimitiveType.DOUBLE );
        ArrayDesign ad1 = new ArrayDesign();
        ad1.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs1" ) );
        BioAssayDimension bad1 = new BioAssayDimension();
        for ( BioMaterial bm : bms ) {
            bad1.getBioAssays().add( BioAssay.Factory.newInstance( "ba1" + bm.getName(), ad1, bm ) );
        }

        QuantitationType qt2 = new QuantitationType();
        qt2.setName( "qt2" );
        qt2.setGeneralType( GeneralType.QUANTITATIVE );
        qt2.setType( StandardQuantitationType.AMOUNT );
        qt2.setScale( ScaleType.LOG2 );
        qt2.setRepresentation( PrimitiveType.DOUBLE );
        ArrayDesign ad2 = new ArrayDesign();
        ad2.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs2" ) );
        BioAssayDimension bad2 = new BioAssayDimension();
        for ( BioMaterial bm : bms ) {
            bad2.getBioAssays().add( BioAssay.Factory.newInstance( "ba2" + bm.getName(), ad2, bm ) );
        }
        for ( BioMaterial bm : bms ) {
            bad2.getBioAssays().add( BioAssay.Factory.newInstance( "ba3" + bm.getName(), ad2, bm ) );
        }

        List<RawExpressionDataVector> vectors = new ArrayList<>();
        vectors.addAll( RandomBulkDataUtils.randomBulkVectors( ee, bad1, ad1, qt1, RawExpressionDataVector.class ) );
        vectors.addAll( RandomBulkDataUtils.randomBulkVectors( ee, bad2, ad2, qt2, RawExpressionDataVector.class ) );
        ExpressionDataDoubleMatrix matrix = new ExpressionDataDoubleMatrix( vectors, Arrays.asList( qt1, qt2 ) );
        assertThat( matrix.getQuantitationTypes() )
                .containsExactlyInAnyOrder( qt1, qt2 );
        assertThat( matrix.getQuantitationType() ).satisfies( qt -> {
            assertThat( qt.getName() ).isEqualTo( "Merged from 2 quantitation types" );
        } );
        assertThatThrownBy( () -> matrix.getBioAssayForColumn( 0 ) )
                .isInstanceOf( IllegalStateException.class );
        assertThat( matrix.getBioAssaysForColumn( 0 ) )
                .hasSize( 3 );
        assertThat( matrix.getBioAssayDimensions() )
                .containsExactlyInAnyOrder( bad1, bad2 )
                .hasSize( 2 );
        assertThat( matrix.getBioAssayDimension().getBioAssays() )
                .hasSize( 16 );
        assertThat( matrix.columns() ).isEqualTo( 8 );
        assertThat( matrix.rows() ).isEqualTo( 2 );
        assertThat( BulkExpressionDataMatrixUtils.toVectors( matrix, RawExpressionDataVector.class ) )
                .hasSize( 2 );
    }

    @Test
    public void testDuplicateElements() {
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < 100; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ExpressionExperiment ee = new ExpressionExperiment();
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        List<RawExpressionDataVector> vectors = new ArrayList<>();
        vectors.addAll( RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );
        vectors.addAll( RandomBulkDataUtils.randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );
        assertThatThrownBy( () -> new ExpressionDataDoubleMatrix( vectors ) )
                .isInstanceOf( IllegalStateException.class );
    }
}