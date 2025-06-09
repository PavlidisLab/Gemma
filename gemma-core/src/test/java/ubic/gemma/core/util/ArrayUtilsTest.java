package ubic.gemma.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayUtilsTest {

    @Test
    public void testBinarySearchFirst() {
        assertEquals( 0, ArrayUtils.binarySearchFirst( new double[] { 1.0, 1.0, 2.0, 3.0, 4.0 }, 1.0 ) );
        assertEquals( 4, ArrayUtils.binarySearchFirst( new double[] { 1.0, 1.0, 2.0, 3.0, 4.0, 4.0 }, 4.0 ) );
        assertEquals( -1, ArrayUtils.binarySearchFirst( new double[] { 1.0, 1.0, 2.0, 3.0, 4.0, 4.0 }, 0.5 ) );
        assertEquals( -5, ArrayUtils.binarySearchFirst( new double[] { 1.0, 1.0, 2.0, 3.0, 4.0, 4.0 }, 3.5 ) );
        assertEquals( -7, ArrayUtils.binarySearchFirst( new double[] { 1.0, 1.0, 2.0, 3.0, 4.0, 4.0 }, 4.5 ) );
    }
}