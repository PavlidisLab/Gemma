package ubic.gemma.core.analysis.singleCell;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleCellDescriptiveTest {

    @Test
    public void testCountDataAsInts() {
        RandomSingleCellDataUtils.setSeed( 123 );
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 8; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.INT );
        CompositeSequence cs = new CompositeSequence();
        SingleCellExpressionDataVector vec = RandomSingleCellDataUtils.randomSingleCellVector( ee, cs, qt, 100, 0.9 );
        assertThat( SingleCellDescriptive.max( vec ) )
                .containsExactly( 12.0, 8.0, 12.0, 10.0, 10.0, 11.0, 11.0, 10.0 );
        assertThat( SingleCellDescriptive.min( vec ) )
                .containsExactly( 3.0, 1.0, 1.0, 3.0, 2.0, 1.0, 2.0, 1.0 );
        assertThat( SingleCellDescriptive.count( vec ) )
                .containsExactly( 10, 10, 10, 10, 10, 10, 10, 10 );
        assertThat( SingleCellDescriptive.countAboveThreshold( vec, 10 ) )
                .containsExactly( 1, 0, 2, 0, 0, 1, 1, 0 );
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly( 5.9, 4.5, 6.4, 6.7, 4.9, 5.4, 7.7, 4.7 );
        assertThat( SingleCellDescriptive.mean( vec, BioAssay.Factory.newInstance( "ba" + 0 ) ) )
                .isEqualTo( 5.9 );
        assertThat( SingleCellDescriptive.median( vec ) )
                .containsExactly( 5.5, 4.0, 7.0, 7.0, 4.5, 5.0, 9.0, 4.0 );
        assertThat( SingleCellDescriptive.sampleStandardDeviation( vec ) )
                .containsExactly(
                        2.7608344253694193, 2.286114713050507, 3.913423142346515, 2.52067379093804, 2.6744018224670185,
                        2.995468055436174, 3.3943179196285818, 2.9099507957398263 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        7.211111111111112, 4.944444444444445, 14.488888888888889, 6.011111111111112,
                        6.766666666666667, 8.488888888888889, 10.9, 8.011111111111113 );
    }

    @Test
    public void testCountDataAsLongs() {
        RandomSingleCellDataUtils.setSeed( 123 );
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 8; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.LONG );
        CompositeSequence cs = new CompositeSequence();
        SingleCellExpressionDataVector vec = RandomSingleCellDataUtils.randomSingleCellVector( ee, cs, qt, 100, 0.9 );
        assertThat( SingleCellDescriptive.count( vec ) )
                .containsExactly( 10, 10, 10, 10, 10, 10, 10, 10 );
        assertThat( SingleCellDescriptive.countAboveThreshold( vec, 10 ) )
                .containsExactly( 1, 0, 2, 0, 0, 1, 1, 0 );
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly( 5.9, 4.5, 6.4, 6.7, 4.9, 5.4, 7.7, 4.7 );
        assertThat( SingleCellDescriptive.mean( vec, BioAssay.Factory.newInstance( "ba" + 0 ) ) )
                .isEqualTo( 5.9 );
        assertThat( SingleCellDescriptive.sampleStandardDeviation( vec ) )
                .containsExactly(
                        2.7608344253694193, 2.286114713050507, 3.913423142346515, 2.52067379093804, 2.6744018224670185,
                        2.995468055436174, 3.3943179196285818, 2.9099507957398263 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        7.211111111111112, 4.944444444444445, 14.488888888888889, 6.011111111111112,
                        6.766666666666667, 8.488888888888889, 10.9, 8.011111111111113 );
    }

    @Test
    public void testCountDataAsFloats() {
        RandomSingleCellDataUtils.setSeed( 123 );
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 8; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.FLOAT );
        CompositeSequence cs = new CompositeSequence();
        SingleCellExpressionDataVector vec = RandomSingleCellDataUtils.randomSingleCellVector( ee, cs, qt, 100, 0.9 );
        assertThat( SingleCellDescriptive.count( vec ) )
                .containsExactly( 10, 10, 10, 10, 10, 10, 10, 10 );
        assertThat( SingleCellDescriptive.countAboveThreshold( vec, 10 ) )
                .containsExactly( 1, 0, 2, 0, 0, 1, 1, 0 );
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly(
                        5.417406547858262,
                        3.932614443274868,
                        5.057499991028314,
                        6.261318390911264,
                        4.291093550075774,
                        4.559236556052248,
                        6.657602986610237,
                        3.882179618340383 );
        assertThat( SingleCellDescriptive.mean( vec, BioAssay.Factory.newInstance( "ba" + 0 ) ) )
                .isEqualTo( 5.417406547858262 );
        assertThat( SingleCellDescriptive.sampleStandardDeviation( vec ) )
                .containsExactly(
                        1.2670894069772458, 1.3756134432155802, 1.5386461476207776, 1.2475112799514836,
                        1.3478628716207852, 1.4341849675226108, 1.4149035902402316, 1.4487652648472737 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        1.518923790782469,
                        1.790252491359375,
                        2.2397470422625165,
                        1.4723478499995486,
                        1.7187506872672615,
                        1.9459505341242995,
                        1.8939790085489623,
                        1.9857177319527692 );

        // add a missing value
        float[] d = vec.getDataAsFloats();
        d[4] = Float.NaN;
        vec.setDataAsFloats( d );
        assertThat( SingleCellDescriptive.count( vec ) )
                .containsExactly( 9, 10, 10, 10, 10, 10, 10, 10 );
        assertThat( SingleCellDescriptive.countAboveThreshold( vec, 10 ) )
                .containsExactly( 1, 0, 2, 0, 0, 1, 1, 0 );
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly( 5.187767180774936, 3.932614443274868, 5.057499991028314, 6.261318390911264, 4.291093550075774, 4.559236556052248, 6.657602986610237, 3.882179618340383 );
        assertThat( SingleCellDescriptive.mean( vec, BioAssay.Factory.newInstance( "ba" + 0 ) ) )
                .isEqualTo( 5.187767180774936 );
        assertThat( SingleCellDescriptive.sampleStandardDeviation( vec ) )
                .containsExactly( 1.2673608803482819, 1.3756134432155802, 1.5386461476207776, 1.2475112799514836,
                        1.3478628716207852, 1.4341849675226108, 1.4149035902402316, 1.4487652648472737 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        1.5195747180685528,
                        1.790252491359375,
                        2.2397470422625165,
                        1.4723478499995486,
                        1.7187506872672615,
                        1.9459505341242995,
                        1.8939790085489623,
                        1.9857177319527692 );
    }

    @Test
    public void testCountDataAsDoubles() {
        RandomSingleCellDataUtils.setSeed( 123 );
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 8; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.COUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        CompositeSequence cs = new CompositeSequence();
        SingleCellExpressionDataVector vec = RandomSingleCellDataUtils.randomSingleCellVector( ee, cs, qt, 100, 0.9 );
        assertThat( SingleCellDescriptive.count( vec ) )
                .containsExactly( 10, 10, 10, 10, 10, 10, 10, 10 );
        assertThat( SingleCellDescriptive.countAboveThreshold( vec, 10 ) )
                .containsExactly( 1, 0, 2, 0, 0, 1, 1, 0 );
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly(
                        5.417406547858262,
                        3.932614443274868,
                        5.057499991028314,
                        6.261318390911264,
                        4.291093550075774,
                        4.559236556052248,
                        6.657602986610237,
                        3.882179618340383 );
        assertThat( SingleCellDescriptive.mean( vec, BioAssay.Factory.newInstance( "ba" + 0 ) ) )
                .isEqualTo( 5.417406547858262 );
        assertThat( SingleCellDescriptive.sampleStandardDeviation( vec ) )
                .containsExactly(
                        1.2670894069772458, 1.3756134432155802, 1.5386461476207776, 1.2475112799514836,
                        1.3478628716207852, 1.4341849675226108, 1.4149035902402316, 1.4487652648472737 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        1.518923790782469,
                        1.790252491359375,
                        2.2397470422625165,
                        1.4723478499995486,
                        1.7187506872672615,
                        1.9459505341242995,
                        1.8939790085489623,
                        1.9857177319527692 );

        // add a missing value
        double[] d = vec.getDataAsDoubles();
        d[4] = Double.NaN;
        vec.setDataAsDoubles( d );
        assertThat( SingleCellDescriptive.count( vec ) )
                .containsExactly( 9, 10, 10, 10, 10, 10, 10, 10 );
        assertThat( SingleCellDescriptive.countAboveThreshold( vec, 10 ) )
                .containsExactly( 1, 0, 2, 0, 0, 1, 1, 0 );
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly( 5.187767180774936, 3.932614443274868, 5.057499991028314, 6.261318390911264, 4.291093550075774, 4.559236556052248, 6.657602986610237, 3.882179618340383 );
        assertThat( SingleCellDescriptive.mean( vec, BioAssay.Factory.newInstance( "ba" + 0 ) ) )
                .isEqualTo( 5.187767180774936 );
        assertThat( SingleCellDescriptive.sampleStandardDeviation( vec ) )
                .containsExactly( 1.2673608803482819, 1.3756134432155802, 1.5386461476207776, 1.2475112799514836,
                        1.3478628716207852, 1.4341849675226108, 1.4149035902402316, 1.4487652648472737 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        1.5195747180685528,
                        1.790252491359375,
                        2.2397470422625165,
                        1.4723478499995486,
                        1.7187506872672615,
                        1.9459505341242995,
                        1.8939790085489623,
                        1.9857177319527692 );
    }

    @Test
    public void testLogTransformedData() {
        RandomSingleCellDataUtils.setSeed( 123 );
        ExpressionExperiment ee = new ExpressionExperiment();
        for ( int i = 0; i < 8; i++ ) {
            ee.getBioAssays().add( BioAssay.Factory.newInstance( "ba" + i ) );
        }
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.COUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        CompositeSequence cs = new CompositeSequence();
        SingleCellExpressionDataVector vec = RandomSingleCellDataUtils.randomSingleCellVector( ee, cs, qt, 100, 0.9 );
        // the values here are basically log2 of the count data
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly(
                        2.437602361543811,
                        1.975488750216347,
                        2.3384244122243083,
                        2.646466464788573,
                        2.10134535360534,
                        2.1887922653969545,
                        2.735002840690517,
                        1.9568668693380502 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        0.38723552591627575,
                        0.7422983739117393,
                        1.3755441902593601,
                        0.33550231300196376,
                        0.6376400109514362,
                        0.9587718936948121,
                        0.8944178556282364,
                        1.0037397276273488 );

        // this is a common occurrence with log-transformed data, a zero becomes -inf
        double[] d = vec.getDataAsDoubles();
        d[4] = Double.NEGATIVE_INFINITY;
        vec.setDataAsDoubles( d );
        assertThat( SingleCellDescriptive.mean( vec ) )
                .containsExactly(
                        Double.NEGATIVE_INFINITY,
                        1.975488750216347,
                        2.3384244122243083,
                        2.646466464788573,
                        2.10134535360534,
                        2.1887922653969545,
                        2.735002840690517,
                        1.9568668693380502 );
        assertThat( SingleCellDescriptive.sampleVariance( vec ) )
                .containsExactly(
                        Double.NaN,
                        0.7422983739117393,
                        1.3755441902593601,
                        0.33550231300196376,
                        0.6376400109514362,
                        0.9587718936948121,
                        0.8944178556282364,
                        1.0037397276273488 );
    }
}