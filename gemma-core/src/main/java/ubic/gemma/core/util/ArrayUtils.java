package ubic.gemma.core.util;

/**
 * Utilities and algorithms for arrays.
 *
 * @author poirigui
 */
public class ArrayUtils {

    /**
     * A binary search that returns the index of the first occurrence of the value in the array.
     *
     * @see java.util.Arrays#binarySearch(double[], double)
     */
    public static int binarySearchFirst( double[] array, double value ) {
        int low = 0;
        int high = array.length;
        while ( true ) {
            // special case for insertion point at the end of the array
            if ( low == array.length ) {
                return -array.length - 1;
            }
            int mid = ( low + high ) / 2;
            double midVal = array[mid];
            if ( low == high ) {
                if ( midVal == value ) {
                    return mid;
                } else {
                    return -mid - 1;
                }
            } else if ( midVal < value ) {
                low = mid + 1; // Search in the right half
                assert low <= high;
            } else if ( midVal > value ) {
                high = mid - 1; // Search in the left half
                assert high >= low;
            } else {
                high = mid;
                assert high >= low;
            }
        }
    }

    public static boolean isContiguous( int[] sampleIndices ) {
        if ( sampleIndices.length <= 1 ) {
            return true;
        }
        for ( int i = 1; i < sampleIndices.length; i++ ) {
            if ( sampleIndices[i] != sampleIndices[i - 1] + 1 ) {
                return false;
            }
        }
        return true;
    }
}
