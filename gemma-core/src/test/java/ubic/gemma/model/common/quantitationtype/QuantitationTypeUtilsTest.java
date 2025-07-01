package ubic.gemma.model.common.quantitationtype;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuantitationTypeUtilsTest {

    @Test
    public void testZscore() {
        assertEquals( Double.NaN, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.ZSCORE, ScaleType.OTHER, PrimitiveType.DOUBLE ) ) );
    }

    @Test
    public void testPresentAbsent() {
        assertEquals( Boolean.FALSE, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.PRESENTABSENT, ScaleType.OTHER, PrimitiveType.BOOLEAN ) ) );
    }

    @Test
    public void testAmount() {
        assertEquals( Double.NaN, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.AMOUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ) ) );
        assertEquals( Double.NaN, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.AMOUNT, ScaleType.LINEAR, PrimitiveType.DOUBLE ) ) );
        assertEquals( Double.NaN, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.AMOUNT, ScaleType.LOG2, PrimitiveType.DOUBLE ) ) );
    }

    @Test
    public void testCount() {
        assertEquals( 0.0, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.COUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ) ) );
        assertEquals( 0.0, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.COUNT, ScaleType.LINEAR, PrimitiveType.DOUBLE ) ) );
        assertEquals( Double.NEGATIVE_INFINITY, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.COUNT, ScaleType.LOG2, PrimitiveType.DOUBLE ) ) );
    }

    @Test
    public void testString() {
        assertEquals( "", QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.OTHER, ScaleType.OTHER, PrimitiveType.STRING ) ) );
    }

    private QuantitationType createQt( StandardQuantitationType type, ScaleType scaleType, PrimitiveType representation ) {
        QuantitationType qt = new QuantitationType();
        qt.setType( type );
        qt.setScale( scaleType );
        qt.setRepresentation( representation );
        return qt;
    }
}