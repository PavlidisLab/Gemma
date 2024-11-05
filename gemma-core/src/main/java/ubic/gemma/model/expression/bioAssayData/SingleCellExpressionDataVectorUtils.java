package ubic.gemma.model.expression.bioAssayData;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.Arrays;

/**
 * Utilities for working with {@link SingleCellExpressionDataVector}.
 * @author poirigui
 */
public class SingleCellExpressionDataVectorUtils {

    /**
     * @see #getSampleStart(SingleCellExpressionDataVector, int, int)
     */
    public static int getSampleStart( SingleCellExpressionDataVector vector, BioAssay sample ) {
        return getSampleStart( vector, sample, 0 );
    }

    /**
     * @see #getSampleStart(SingleCellExpressionDataVector, int, int)
     */
    public static int getSampleStart( SingleCellExpressionDataVector vector, BioAssay sample, int after ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( sample + " is not found in the single-cell dimension." );
        }
        return getSampleStart( vector, sampleIndex, after );
    }

    /**
     * Get the index of the first cell for a given sample.
     * @param vector      vector
     * @param sampleIndex index of the sample in the single-cell dimension, see {@link SingleCellDimension#getBioAssays()}
     * @param after       starting position for the search
     */
    public static int getSampleStart( SingleCellExpressionDataVector vector, int sampleIndex, int after ) {
        int sampleOffset = vector.getSingleCellDimension().getBioAssaysOffset()[sampleIndex];
        // check where the next sample begins, only search past this sample starting point
        int start = Arrays.binarySearch( vector.getDataIndices(), after, vector.getDataIndices().length, sampleOffset );
        if ( start < 0 ) {
            start = -start - 1;
        }
        return start;
    }

    public static int getSampleEnd( SingleCellExpressionDataVector vector, BioAssay sample ) {
        return getSampleEnd( vector, sample, 0 );
    }

    public static int getSampleEnd( SingleCellExpressionDataVector vector, BioAssay sample, int after ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( sample + " is not found in the single-cell dimension." );
        }
        return getSampleEnd( vector, sampleIndex, after );
    }

    /**
     * Get the *exclusive* index of the last cell for a given sample.
     * @param vector      vector
     * @param sampleIndex index of the sample in the single-cell dimension, see {@link SingleCellDimension#getBioAssays()}
     * @param after       starting position for the search, if you previously called {@link #getSampleStart(SingleCellExpressionDataVector, int, int)},
     *                    you should use this value here to save some time
     */
    public static int getSampleEnd( SingleCellExpressionDataVector vector, int sampleIndex, int after ) {
        return getSampleEnd( vector.getSingleCellDimension(), vector.getDataIndices(), sampleIndex, after );
    }

    /**
     * Only exposed for internal use, prefer {@link #getSampleEnd(SingleCellExpressionDataVector, int, int)}.
     */
    public static int getSampleEnd( SingleCellDimension dimension, int[] dataIndices, int sampleIndex, int after ) {
        int sampleOffset = dimension.getBioAssaysOffset()[sampleIndex];
        int numberOfCells = dimension.getNumberOfCellsBySample( sampleIndex );
        int nextSampleOffset = sampleOffset + numberOfCells;
        // check where the next sample begins, only search past this sample starting point
        int end = Arrays.binarySearch( dataIndices, after, dataIndices.length, nextSampleOffset );
        if ( end < 0 ) {
            end = -end - 1;
        }
        return end;
    }

    /**
     * Check if a given cell has a given characteristic.
     */
    public static boolean hasCharacteristic( SingleCellExpressionDataVector vector, int cellIndex, Characteristic characteristic ) {
        for ( CellLevelCharacteristics clc : vector.getSingleCellDimension().getCellTypeAssignments() ) {
            if ( characteristic.equals( clc.getCharacteristic( cellIndex ) ) ) {
                return true;
            }
        }
        for ( CellLevelCharacteristics clc : vector.getSingleCellDimension().getCellLevelCharacteristics() ) {
            if ( characteristic.equals( clc.getCharacteristic( cellIndex ) ) ) {
                return true;
            }
        }
        return false;
    }
}
