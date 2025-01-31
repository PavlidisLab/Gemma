package ubic.gemma.core.analysis.preprocess.convert;

import org.junit.Before;
import org.junit.Test;
import ubic.gemma.core.analysis.preprocess.detect.InferredQuantitationMismatchException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils.ensureLog2Scale;

public class QuantitationTypeConversionUtilsTest {

    /* fixtures */
    private QuantitationType qt;
    private ExpressionDataDoubleMatrix matrix;

    @Before
    public void setUp() {
        RawExpressionDataVector ev = new RawExpressionDataVector();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        CompositeSequence cs = CompositeSequence.Factory.newInstance( "test", ad );
        ev.setDesignElement( cs );
        qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ev.setQuantitationType( qt );
        ev.setDataAsDoubles( new double[] { 4.0 } );
        BioAssayDimension bad = new BioAssayDimension();
        BioAssay ba = new BioAssay();
        BioMaterial bm = new BioMaterial();
        ba.setSampleUsed( bm );
        bad.setBioAssays( Collections.singletonList( ba ) );
        ev.setBioAssayDimension( bad );
        matrix = new ExpressionDataDoubleMatrix( Collections.singleton( ev ), Collections.singleton( qt ) );
        matrix.set( 0, 0, 4.0 );
        RandomExpressionDataMatrixUtils.setSeed( 123L );
    }

    @Test
    public void testLog2ShouldDoNothing() throws QuantitationTypeConversionException {
        qt.setScale( ScaleType.LOG2 );
        assertThat( ensureLog2Scale( matrix ) )
                .isSameAs( matrix );
    }

    @Test
    public void testLinearConversion() throws QuantitationTypeConversionException {
        qt.setScale( ScaleType.LINEAR );
        assertThat( ensureLog2Scale( matrix ) )
                .satisfies( this::basicBasicLog2MatrixChecks )
                .satisfies( m -> {
                    assertThat( m.getMatrix().get( 0, 0 ) ).isEqualTo( Math.log( 4.0 ) / Math.log( 2 ) );
                } );
    }

    @Test
    public void testLog10Conversion() throws QuantitationTypeConversionException {
        qt.setScale( ScaleType.LOG10 );
        assertThat( ensureLog2Scale( matrix ) )
                .satisfies( this::basicBasicLog2MatrixChecks )
                .satisfies( m -> {
                    assertThat( m.getMatrix().get( 0, 0 ) ).isEqualTo( 4.0 * Math.log( 10 ) / Math.log( 2 ), within( 1e-10 ) );
                } );
    }

    @Test
    public void testCountConversion() throws QuantitationTypeConversionException {
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        assertThat( ensureLog2Scale( matrix ) )
                .satisfies( this::basicBasicLog2MatrixChecks )
                .satisfies( m -> {
                    assertThat( m.getMatrix().get( 0, 0 ) )
                            .isEqualTo( Math.log( 1e6 * ( 4 + 0.5 ) / ( 4 + 1 ) ) / Math.log( 2 ), within( 1e-10 ) );
                } );
    }

    @Test
    public void testInferredLogbaseDifferFromQuantitations() {
        assertThatThrownBy( () -> ensureLog2Scale( matrix, false ) )
                .isInstanceOf( InferredQuantitationMismatchException.class );
    }


    private void basicBasicLog2MatrixChecks( ExpressionDataDoubleMatrix m ) {
        assertThat( m.getQuantitationTypes() )
                .hasSize( 1 )
                .allSatisfy( qt -> {
                    assertThat( qt.getGeneralType() ).isEqualTo( GeneralType.QUANTITATIVE );
                    assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
                    assertThat( qt.getRepresentation() ).isEqualTo( PrimitiveType.DOUBLE );
                    assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG2 );
                } );
    }
}