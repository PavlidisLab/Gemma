package ubic.gemma.core.analysis.preprocess.convert;


import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.analysis.preprocess.convert.ScaleTypeConversionUtils.convertVector;

public class ScaleTypeConversionUtilsTest {

    @Test
    public void testConvertData() {
        QuantitationType qt = new QuantitationType();
        qt.setScale( ScaleType.COUNT );
        assertThat( convertVector( new double[] { 1.0, 2.0, 3.0 }, qt, ScaleType.COUNT ) )
                .containsExactly( 1.0, 2.0, 3.0 );
    }
}