package ubic.gemma.core.datastructure.matrix;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.*;
import static ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrixUtil.*;

public class ExpressionDataDoubleMatrixUtilTest {

    private final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    /* fixtures */
    private QuantitationType qt;
    private ExpressionDataDoubleMatrix matrix;

    @Before
    public void setUp() {
        RawExpressionDataVector ev = new RawExpressionDataVector();
        CompositeSequence cs = new CompositeSequence();
        ev.setDesignElement( cs );
        ev.setData( byteArrayConverter.doubleArrayToBytes( new Double[] { 4.0 } ) );
        qt = new QuantitationTypeImpl();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        ev.setQuantitationType( qt );
        BioAssayDimension bad = new BioAssayDimension();
        BioAssay ba = new BioAssay();
        BioMaterial bm = new BioMaterial();
        ba.setSampleUsed( bm );
        bad.setBioAssays( Collections.singletonList( ba ) );
        ev.setBioAssayDimension( bad );
        matrix = new ExpressionDataDoubleMatrix( Collections.singleton( ev ), Collections.singleton( qt ) );
        matrix.set( 0, 0, 4.0 );
    }

    @Test
    public void testLog2ShouldDoNothing() {
        qt.setScale( ScaleType.LOG2 );
        assertThat( ExpressionDataDoubleMatrixUtil.ensureLog2Scale( matrix, false ) )
                .isSameAs( matrix );
    }

    @Test
    public void testLinearConversion() {
        qt.setScale( ScaleType.LINEAR );
        assertThat( ExpressionDataDoubleMatrixUtil.ensureLog2Scale( matrix, false ) )
                .satisfies( this::basicBasicLog2MatrixChecks )
                .satisfies( m -> {
                    assertThat( m.getMatrix().get( 0, 0 ) ).isEqualTo( Math.log( 4.0 ) / Math.log( 2 ) );
                } );
    }

    @Test
    public void testLog10Conversion() {
        qt.setScale( ScaleType.LOG10 );
        assertThat( ExpressionDataDoubleMatrixUtil.ensureLog2Scale( matrix, false ) )
                .satisfies( this::basicBasicLog2MatrixChecks )
                .satisfies( m -> {
                    assertThat( m.getMatrix().get( 0, 0 ) ).isEqualTo( 4.0 * Math.log( 10 ) / Math.log( 2 ), within( 1e-10 ) );
                } );
    }

    @Test
    public void testCountConversion() {
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        assertThat( ExpressionDataDoubleMatrixUtil.ensureLog2Scale( matrix, false ) )
                .satisfies( this::basicBasicLog2MatrixChecks )
                .satisfies( m -> {
                    assertThat( m.getMatrix().get( 0, 0 ) )
                            .isEqualTo( Math.log( 1e6 * ( 4 + 0.5 ) / ( 4 + 1 ) ) / Math.log( 2 ), within( 1e-10 ) );
                } );
    }

    @Test
    public void testInferredLogbase() {
        assertThat( ExpressionDataDoubleMatrixUtil.ensureLog2Scale( matrix, true ) )
                .satisfies( this::basicBasicLog2MatrixChecks );
    }

    @Test
    public void testCounts() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE110031" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.COUNT );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.COUNT );
        assertThat( inferIsRatio( data ) ).isFalse();
    }

    @Test
    public void testCountsWhenValueIsNegativeTreatAsLogbaseUnknown() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE107314" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.LOGBASEUNKNOWN );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( inferIsRatio( data ) ).isFalse();
    }

    @Test
    public void testCountsWhenPlatformIsMicroarrayThenReportAsLinear() {
        // TODO
    }

    @Test
    public void testPercent1() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE11135" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.PERCENT1 );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( inferIsRatio( data ) ).isFalse();
    }

    @Test
    public void testPercent() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE2" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.PERCENT );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( inferIsRatio( data ) ).isFalse();
    }

    @Test
    public void testZtransformed() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE97677" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.LOGBASEUNKNOWN );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.ZSCORE );
        assertThat( inferIsRatio( data ) ).isFalse();
    }

    @Test
    public void testZTransformedByMedian() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE100366" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.LOGBASEUNKNOWN );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.ZSCORE );
        assertThat( inferIsRatio( data ) ).isFalse();
    }

    @Test
    public void testZtransformedInLogSpace() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE109035" );
        // should be LINEAR, but the values in this dataset are not large enough
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.OTHER );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.ZSCORE );
        assertThat( inferIsRatio( data ) ).isFalse();
    }

    @Test
    public void testIsLog10Ratiometric() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE3087" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.LOG10 );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( inferIsRatio( data ) ).isTrue();
    }

    @Test
    @Ignore
    public void testLog2Ratiometric() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE2178" );
        assertThat( inferScaleType( data ) ).isEqualTo( ScaleType.LOG2 );
        assertThat( inferStandardQuantitationType( data ) ).isEqualTo( StandardQuantitationType.AMOUNT );
        assertThat( inferIsRatio( data ) ).isTrue();
    }

    private ExpressionDataDoubleMatrix readTestMatrix( String shortName ) {
        try ( InputStream is = new GZIPInputStream( new ClassPathResource( "/data/analysis/scale/" + shortName + ".gz" ).getInputStream() ) ) {
            DoubleMatrix<String, String> rawMatrix = new DoubleMatrixReader().read( is );
            DenseDoubleMatrix<CompositeSequence, BioMaterial> matrix = new DenseDoubleMatrix<>( rawMatrix.getRawMatrix() );
            BioAssay ba = BioAssay.Factory.newInstance();
            matrix.setRowNames( rawMatrix.getRowNames().stream().map( CompositeSequence.Factory::newInstance ).collect( Collectors.toList() ) );
            matrix.setColumnNames( rawMatrix.getColNames().stream().map( name -> BioMaterial.Factory.newInstance( name, ba ) ).collect( Collectors.toList() ) );
            ExpressionExperiment ee = new ExpressionExperiment();
            ee.setShortName( shortName );
            ee.setBioAssays( Collections.singleton( ba ) );
            QuantitationType qt = new QuantitationTypeImpl();
            return new ExpressionDataDoubleMatrix( ee, qt, matrix );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
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