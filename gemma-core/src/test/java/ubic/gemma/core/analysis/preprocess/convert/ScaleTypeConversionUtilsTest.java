package ubic.gemma.core.analysis.preprocess.convert;


import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils.convertVector;

public class ScaleTypeConversionUtilsTest {

    @Test
    public void testConvertCountingData() {
        QuantitationType qt = new QuantitationType();
        qt.setScale( ScaleType.COUNT );
        double[] vec = new double[] { 1.0, 2.0, 3.0 };

        assertThat( convertVector( vec, qt, ScaleType.COUNT ) )
                .isSameAs( vec );

        assertThat( convertVector( vec, qt, ScaleType.LINEAR ) )
                .isSameAs( vec );

        assertThat( convertVector( vec, qt, ScaleType.LOG2 ) )
                .containsExactly( 0.0, 1.0, Math.log( 3.0 ) / Math.log( 2.0 ) );
    }
}