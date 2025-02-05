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
    public void testCountData() {
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