package ubic.gemma.model.common.quantitationtype;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.util.Arrays;

import static org.junit.Assert.*;

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
        assertArrayEquals( ByteArrayUtils.doubleArrayToBytes( new double[] { Double.NaN } ), QuantitationTypeUtils.getDefaultValueAsBytes( createQt( StandardQuantitationType.AMOUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ) ) );
        assertEquals( Double.NaN, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.AMOUNT, ScaleType.LINEAR, PrimitiveType.DOUBLE ) ) );
        assertArrayEquals( ByteArrayUtils.doubleArrayToBytes( new double[] { Double.NaN } ), QuantitationTypeUtils.getDefaultValueAsBytes( createQt( StandardQuantitationType.AMOUNT, ScaleType.LINEAR, PrimitiveType.DOUBLE ) ) );
        assertEquals( Double.NaN, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.AMOUNT, ScaleType.LOG2, PrimitiveType.DOUBLE ) ) );
        assertArrayEquals( ByteArrayUtils.doubleArrayToBytes( new double[] { Double.NaN } ), QuantitationTypeUtils.getDefaultValueAsBytes( createQt( StandardQuantitationType.AMOUNT, ScaleType.LOG2, PrimitiveType.DOUBLE ) ) );
    }

    @Test
    public void testCount() {
        assertEquals( 0.0, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.COUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ) ) );
        assertArrayEquals( ByteArrayUtils.doubleArrayToBytes( new double[] { 0.0f } ), QuantitationTypeUtils.getDefaultValueAsBytes( createQt( StandardQuantitationType.COUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ) ) );
        assertEquals( 0.0, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.COUNT, ScaleType.LINEAR, PrimitiveType.DOUBLE ) ) );
        assertArrayEquals( ByteArrayUtils.doubleArrayToBytes( new double[] { 0.0f } ), QuantitationTypeUtils.getDefaultValueAsBytes( createQt( StandardQuantitationType.COUNT, ScaleType.LINEAR, PrimitiveType.DOUBLE ) ) );
        assertEquals( Double.NEGATIVE_INFINITY, QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.COUNT, ScaleType.LOG2, PrimitiveType.DOUBLE ) ) );
        assertArrayEquals( ByteArrayUtils.doubleArrayToBytes( new double[] { Double.NEGATIVE_INFINITY } ), QuantitationTypeUtils.getDefaultValueAsBytes( createQt( StandardQuantitationType.COUNT, ScaleType.LOG2, PrimitiveType.DOUBLE ) ) );
    }

    @Test
    public void testString() {
        assertEquals( "", QuantitationTypeUtils.getDefaultValue( createQt( StandardQuantitationType.OTHER, ScaleType.OTHER, PrimitiveType.STRING ) ) );
        assertArrayEquals( ByteArrayUtils.stringsToByteArray( new String[] { "" } ), QuantitationTypeUtils.getDefaultValueAsBytes( createQt( StandardQuantitationType.OTHER, ScaleType.OTHER, PrimitiveType.STRING ) ) );
    }

    @Test
    public void testMergeQuantitationTypes() {
        QuantitationType mergedQt = QuantitationTypeUtils.mergeQuantitationTypes( Arrays.asList(
                createQt( StandardQuantitationType.COUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ),
                createQt( StandardQuantitationType.COUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ),
                createQt( StandardQuantitationType.COUNT, ScaleType.COUNT, PrimitiveType.DOUBLE ),
                createQt( StandardQuantitationType.COUNT, ScaleType.COUNT, PrimitiveType.DOUBLE )
        ) );
        assertEquals( StandardQuantitationType.COUNT, mergedQt.getType() );
        Assertions.assertThat( mergedQt.getDescription() )
                .startsWith( "Data was merged from the following quantitation types:\n" )
                .hasLineCount( 5 );
        assertNull( mergedQt.getGeneralType() );
        assertEquals( StandardQuantitationType.COUNT, mergedQt.getType() );
        assertEquals( ScaleType.COUNT, mergedQt.getScale() );
        assertEquals( PrimitiveType.DOUBLE, mergedQt.getRepresentation() );
    }

    private QuantitationType createQt( StandardQuantitationType type, ScaleType scaleType, PrimitiveType representation ) {
        QuantitationType qt = new QuantitationType();
        qt.setName( RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        qt.setType( type );
        qt.setScale( scaleType );
        qt.setRepresentation( representation );
        return qt;
    }
}