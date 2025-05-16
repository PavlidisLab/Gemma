package ubic.gemma.core.analysis.stats;

import org.junit.Test;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.analysis.stats.DataVectorDescriptive.count;
import static ubic.gemma.core.analysis.stats.DataVectorDescriptive.countMissing;

public class DataVectorDescriptiveTest {

    @Test
    public void testCountMissing() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setScale( ScaleType.LINEAR );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        double[] data = new double[100];
        for ( int i = 0; i < 100; i++ ) {
            if ( i % 10 == 0 ) {
                data[i] = Double.NaN;
            } else {
                data[i] = 1.0;
            }
        }
        RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
        vector.setQuantitationType( qt );
        vector.setDataAsDoubles( data );
        assertArrayEquals( data, vector.getDataAsDoubles(), Double.MIN_VALUE );
        assertEquals( 90, count( vector ) );
        assertEquals( 10, countMissing( vector ) );
    }

    @Test
    public void testCountWithPresentAbsent() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.CATEGORICAL );
        qt.setType( StandardQuantitationType.PRESENTABSENT );
        qt.setScale( ScaleType.OTHER );
        qt.setRepresentation( PrimitiveType.BOOLEAN );
        boolean[] data = new boolean[100];
        for ( int i = 0; i < 100; i++ ) {
            if ( i % 10 == 0 ) {
                data[i] = false;
            } else {
                data[i] = true;
            }
        }
        RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
        vector.setQuantitationType( qt );
        vector.setDataAsBooleans( data );
        assertArrayEquals( data, vector.getDataAsBooleans() );
        assertEquals( 90, count( vector ) );
        assertEquals( 10, countMissing( vector ) );
    }

    @Test
    public void testCountWithBooleans() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.CATEGORICAL );
        qt.setType( StandardQuantitationType.FAILED );
        qt.setScale( ScaleType.OTHER );
        qt.setRepresentation( PrimitiveType.BOOLEAN );
        boolean[] data = new boolean[100];
        for ( int i = 0; i < 100; i++ ) {
            if ( i % 10 == 0 ) {
                data[i] = false;
            } else {
                data[i] = true;
            }
        }
        RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
        vector.setQuantitationType( qt );
        vector.setDataAsBooleans( data );
        assertArrayEquals( data, vector.getDataAsBooleans() );
        assertEquals( 100, count( vector ) );
        assertEquals( 0, countMissing( vector ) );
    }

    @Test
    public void testCountWithChars() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.CATEGORICAL );
        qt.setType( StandardQuantitationType.OTHER );
        qt.setScale( ScaleType.OTHER );
        qt.setRepresentation( PrimitiveType.CHAR );
        char[] data = new char[100];
        for ( int i = 0; i < 100; i++ ) {
            if ( i % 10 == 0 ) {
                data[i] = '\0';
            } else {
                data[i] = 'B';
            }
        }
        RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
        vector.setQuantitationType( qt );
        vector.setDataAsChars( data );
        assertArrayEquals( data, vector.getDataAsChars() );
        assertEquals( 90, count( vector ) );
        assertEquals( 10, countMissing( vector ) );
    }

    @Test
    public void testCountWithStrings() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.CATEGORICAL );
        qt.setScale( ScaleType.OTHER );
        qt.setRepresentation( PrimitiveType.STRING );
        String[] data = new String[100];
        for ( int i = 0; i < 100; i++ ) {
            if ( i % 10 == 0 ) {
                data[i] = "";
            } else {
                data[i] = "test";
            }
        }
        RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
        vector.setQuantitationType( qt );
        vector.setDataAsStrings( data );
        assertArrayEquals( data, vector.getDataAsStrings() );
        assertEquals( 90, count( vector ) );
        assertEquals( 10, countMissing( vector ) );
    }
}