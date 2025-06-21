package ubic.gemma.core.datastructure.matrix;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils.randomBulkVectors;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils.setSeed;

public class BulkExpressionDataMatrixTest {

    @Test
    public void testEmptyMatrix() {
        ExpressionExperiment ee = new ExpressionExperiment();
        BioAssayDimension dimension = new BioAssayDimension();
        dimension.getBioAssays().add( BioAssay.Factory.newInstance() );
        QuantitationType qt = new QuantitationType();
        EmptyBulkExpressionDataMatrix matrix = new EmptyBulkExpressionDataMatrix( ee, dimension, qt );
        assertFalse( matrix.hasMissingValues() );
        assertThrows( IndexOutOfBoundsException.class, () -> matrix.get( 0, 0 ) );
        assertThrows( IndexOutOfBoundsException.class, () -> matrix.getRow( 0 ) );
        assertEquals( 0, matrix.getColumn( 0 ).length );
        assertEquals( 0, matrix.getRawMatrix().length );
        assertEquals( -1, matrix.getRowIndex( new CompositeSequence() ) );
    }

    @Test
    public void testNoColumnMatrix() {
        setSeed( 123 );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "test" );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs1", ad ) );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        BulkExpressionDataDoubleMatrix matrix = new BulkExpressionDataDoubleMatrix( new ArrayList<>( randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) ) );
        assertEquals( 1, matrix.rows() );
        assertEquals( 0, matrix.columns() );
        assertFalse( matrix.hasMissingValues() );
        Assertions.assertThat( matrix.getRowAsDoubles( 0 ) )
                .isEmpty();
        Assertions.assertThatThrownBy( () -> matrix.getColumnAsDoubles( 0 ) )
                .isInstanceOf( IndexOutOfBoundsException.class );
    }

    @Test
    public void testDoubleMatrix() {
        setSeed( 123 );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 10; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        ee.setShortName( "test" );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs1", ad ) );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        BulkExpressionDataDoubleMatrix matrix = new BulkExpressionDataDoubleMatrix( new ArrayList<>( randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) ) );
        assertEquals( 1, matrix.rows() );
        assertEquals( 10, matrix.columns() );
        assertFalse( matrix.hasMissingValues() );
        Assertions.assertThat( matrix.getRowAsDoubles( 0 ) )
                .containsExactly( 8.750564224663629,
                        9.210589732060678,
                        7.911231181911094,
                        6.593639239427607,
                        8.447632012172322,
                        8.782003976839846,
                        8.248640829167844,
                        8.004097046903558,
                        7.789775245359376,
                        10.036688491881815 );
        Assertions.assertThat( matrix.getColumnAsDoubles( 0 ) )
                .containsExactly( 8.750564224663629 );
    }

    @Test
    public void testRepeatedDesignElements() {
        setSeed( 123 );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        for ( int i = 0; i < 10; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        ee.setShortName( "test" );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        for ( int i = 0; i < 10; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ArrayList<RawExpressionDataVector> vecs = new ArrayList<>();
        vecs.addAll( randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );
        vecs.addAll( randomBulkVectors( ee, ad, qt, RawExpressionDataVector.class ) );
        BulkExpressionDataDoubleMatrix matrix = new BulkExpressionDataDoubleMatrix( vecs );
        assertEquals( 20, matrix.rows() );
        CompositeSequence de = matrix.getDesignElements().get( 0 );
        Assertions.assertThat( matrix.getRowIndices( de ) ).containsExactly( 0, 10 );
    }
}