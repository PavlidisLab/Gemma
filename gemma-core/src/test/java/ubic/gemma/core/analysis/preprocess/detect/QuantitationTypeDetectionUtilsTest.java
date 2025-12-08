package ubic.gemma.core.analysis.preprocess.detect;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.detectSuspiciousValues;
import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.inferQuantitationType;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils.*;

public class QuantitationTypeDetectionUtilsTest {

    /* fixtures */
    private QuantitationType qt;
    private ExpressionDataDoubleMatrix matrix;

    @Test
    public void testCounts() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE110031", TechnologyType.SEQUENCING );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.COUNT );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testCountsWhenPlatformIsNotSequencing() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE110031", TechnologyType.ONECOLOR );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LINEAR );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testCountsWhenValueIsNegativeTreatAsLogbaseUnknown() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE107314", TechnologyType.SEQUENCING );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOGBASEUNKNOWN );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testCountsWhenPlatformIsMicroarrayThenReportAsLinear() {
        // TODO
    }

    @Test
    public void testPercent1() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE11135", TechnologyType.ONECOLOR );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.PERCENT1 );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testPercent() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE2", TechnologyType.ONECOLOR );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.PERCENT );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testZtransformed() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE97677", TechnologyType.ONECOLOR );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.ZSCORE );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOGBASEUNKNOWN );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testZTransformedByMedian() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE100366", TechnologyType.ONECOLOR );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.ZSCORE );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOGBASEUNKNOWN );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testZtransformedInLogSpace() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE109035", TechnologyType.ONECOLOR );
        // should be LINEAR, but the values in this dataset are not large enough
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.ZSCORE );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.OTHER );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testIsLog10Ratiometric() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE3087", TechnologyType.ONECOLOR );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG10 );
            assertThat( qt.getIsRatio() ).isTrue();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testLog2Ratiometric() {
        ExpressionDataDoubleMatrix data = readTestMatrix( "GSE2178", TechnologyType.ONECOLOR );
        assertThat( inferQuantitationType( data ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.AMOUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG2 );
            assertThat( qt.getIsRatio() ).isTrue();
            detectSuspiciousValues( data, qt );
        } );
    }

    @Test
    public void testRandomLinearMatrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.ONECOLOR );
        matrix = randomLinearMatrix( ee );
        assertThat( matrix.rows() ).isEqualTo( 10000 );
        assertThat( matrix.columns() ).isEqualTo( 2 );
        assertThat( inferQuantitationType( matrix ) ).satisfies( qt -> {
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LINEAR );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( matrix, qt );
        } );
    }

    @Test
    public void testRandomLog2Matrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.ONECOLOR );
        matrix = randomLog2Matrix( ee );
        assertThat( matrix.rows() ).isEqualTo( 10000 );
        assertThat( matrix.columns() ).isEqualTo( 2 );
        assertThat( inferQuantitationType( matrix ) ).satisfies( qt -> {
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG2 );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( matrix, qt );
        } );
    }


    @Test
    public void testRandomRatiometricLog2Matrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.ONECOLOR );
        matrix = randomLog2RatiometricMatrix( ee );
        assertThat( matrix.rows() ).isEqualTo( 10000 );
        assertThat( matrix.columns() ).isEqualTo( 2 );
        assertThat( inferQuantitationType( matrix ) ).satisfies( qt -> {
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG2 );
            assertThat( qt.getIsRatio() ).isTrue();
            detectSuspiciousValues( matrix, qt );
        } );
    }

    @Test
    public void testRandomCountMatrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.SEQUENCING );
        matrix = randomCountMatrix( ee );
        assertThat( matrix.rows() ).isEqualTo( 10000 );
        assertThat( matrix.columns() ).isEqualTo( 2 );
        assertThat( inferQuantitationType( matrix ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.COUNT );
            assertThat( qt.getIsRatio() ).isFalse();
            detectSuspiciousValues( matrix, qt );
        } );
    }

    @Test
    public void testRandomLogTransformedCountMatrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.SEQUENCING );
        matrix = randomCountMatrix( ee, ScaleType.LOG2 );
        assertThat( matrix.rows() ).isEqualTo( 10000 );
        assertThat( matrix.columns() ).isEqualTo( 2 );
        assertThat( inferQuantitationType( matrix ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG2 );
            assertThat( qt.getIsRatio() ).isFalse();
        } );
    }

    @Test
    public void testRandomLnTransformedCountMatrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.SEQUENCING );
        matrix = randomCountMatrix( ee, ScaleType.LN );
        assertThat( matrix.rows() ).isEqualTo( 10000 );
        assertThat( matrix.columns() ).isEqualTo( 2 );
        assertThat( inferQuantitationType( matrix ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LN );
            assertThat( qt.getIsRatio() ).isFalse();
        } );
    }

    @Test
    public void testRandomLog1pTransformedCountMatrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.SEQUENCING );
        matrix = randomCountMatrix( ee, ScaleType.LOG1P );
        assertThat( matrix.rows() ).isEqualTo( 10000 );
        assertThat( matrix.columns() ).isEqualTo( 2 );
        assertThat( inferQuantitationType( matrix ) ).satisfies( qt -> {
            assertThat( qt.getType() ).isEqualTo( StandardQuantitationType.COUNT );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG1P );
            assertThat( qt.getIsRatio() ).isFalse();
        } );
    }

    @Test
    public void testRandomSuspiciousLog2Matrix() {
        ExpressionExperiment ee = getTestExpressionExperiment( TechnologyType.ONECOLOR );
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        matrix = randomExpressionMatrix( ee, qt, new NormalDistribution( 25, 2 ) );
        assertThatThrownBy( () -> detectSuspiciousValues( matrix, qt ) )
                .isInstanceOf( SuspiciousValuesForQuantitationException.class ).satisfies( e -> {
                    assertThat( ( ( SuspiciousValuesForQuantitationException ) e ).getSuspiciousValues() ).isNotEmpty();
                } );
    }

    private ExpressionExperiment getTestExpressionExperiment( TechnologyType technologyType ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        Set<BioAssay> bioAssays = new HashSet<>();
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setTechnologyType( technologyType );
        Set<CompositeSequence> seqs = new HashSet<>();
        for ( int j = 0; j < 10000; j++ ) {
            seqs.add( CompositeSequence.Factory.newInstance( String.valueOf( j ), ad ) );
        }
        ad.setCompositeSequences( seqs );
        for ( int i = 0; i < 2; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance( String.valueOf( i ) );
            ba.setArrayDesignUsed( ad );
            BioMaterial bm = BioMaterial.Factory.newInstance( String.valueOf( i ) );
            bm.setBioAssaysUsedIn( Collections.singleton( ba ) );
            ba.setSampleUsed( bm );
            bioAssays.add( ba );
        }
        ee.setBioAssays( bioAssays );
        ee.setNumberOfSamples( 2 );
        ee.setNumberOfDataVectors( 10000 );
        return ee;
    }

    private ExpressionDataDoubleMatrix readTestMatrix( String shortName, TechnologyType technologyType ) {
        try ( InputStream is = new GZIPInputStream( new ClassPathResource( "/data/analysis/scale/" + shortName + ".gz" ).getInputStream() ) ) {
            ArrayDesign ad = ArrayDesign.Factory.newInstance();
            ad.setTechnologyType( technologyType );
            Set<BioAssay> bas = new HashSet<>();
            DoubleMatrix<String, String> rawMatrix = new DoubleMatrixReader().read( is );
            DenseDoubleMatrix<CompositeSequence, BioMaterial> matrix = new DenseDoubleMatrix<>( rawMatrix.getRawMatrix() );
            matrix.setRowNames( rawMatrix.getRowNames().stream().map( n -> CompositeSequence.Factory.newInstance( n, ad ) ).collect( Collectors.toList() ) );
            matrix.setColumnNames( rawMatrix.getColNames().stream().map( name -> {
                BioMaterial bm = BioMaterial.Factory.newInstance( name );

                BioAssay ba = BioAssay.Factory.newInstance();
                ba.setArrayDesignUsed( ad );
                ba.setSampleUsed( bm );
                bas.add( ba );

                bm.setBioAssaysUsedIn( Collections.singleton( ba ) );
                return bm;
            } ).collect( Collectors.toList() ) );
            ExpressionExperiment ee = new ExpressionExperiment();
            ee.setShortName( shortName );
            ee.setBioAssays( bas );
            ee.setNumberOfSamples( bas.size() );
            QuantitationType qt = new QuantitationType();
            return new ExpressionDataDoubleMatrix( ee, matrix, qt );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}