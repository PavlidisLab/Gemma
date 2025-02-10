package ubic.gemma.core.analysis.singleCell;

import cern.colt.list.DoubleArrayList;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.stats.DataVectorDescriptive;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.*;

/**
 * Descriptive statistics for single cell data.
 * <p>
 * The utilities here extend {@link DataVectorDescriptive} by providing per-assay statistics.
 * @author poirigui
 * @see DataVectorDescriptive
 */
public class SingleCellDescriptive {

    public static double[] max( SingleCellExpressionDataVector vector ) {
        return applyDescriptive( vector, DescriptiveWithMissing::max );
    }

    public static double[] min( SingleCellExpressionDataVector vector ) {
        return applyDescriptive( vector, DescriptiveWithMissing::min );
    }

    /**
     * Count the number of values in each assay.
     * <p>
     * Missing values are not counted.
     */
    public static int[] count( SingleCellExpressionDataVector vector ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case DOUBLE:
                return count( vector, vector.getDataAsDoubles() );
            // these data types don't have a missing value indicator, so we can simply use (end - start) to count the
            // number of values
            case CHAR:
            case INT:
            case LONG:
            case BOOLEAN:
            case STRING: // note that for string, we cannot store "null" in a vector due to encoding limitations
                return countFast( vector );
            default:
                throw new UnsupportedOperationException( "Don't know how to count data of type " + vector.getQuantitationType().getRepresentation() + "." );
        }
    }

    private static int[] count( SingleCellExpressionDataVector vector, double[] data ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( Double.isNaN( data[j] ) ) {
                    continue;
                }
                d[i]++;
            }
        }
        return d;
    }

    private static int[] countFast( SingleCellExpressionDataVector vector ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            d[i] = end - start;
        }
        return d;
    }

    /**
     * Compute the number of cells expressed strictly above the given threshold.
     */
    public static int[] countAboveThreshold( SingleCellExpressionDataVector vector, double threshold ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            for ( double s : getSampleDataAsDoubles( vector, i ) ) {
                if ( s > threshold ) {
                    d[i]++;
                }
            }
        }
        return d;
    }

    private static double[] applyDescriptive( SingleCellExpressionDataVector vector, ToDoubleFunction<DoubleArrayList> func ) {
        DoubleArrayList vec = null;
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            double[] data = getSampleDataAsDoubles( vector, i );
            if ( vec == null ) {
                vec = new DoubleArrayList( data );
            } else {
                vec.elements( data );
            }
            d[i] = func.applyAsDouble( vec );
        }
        return d;
    }

    public static double[] sumUnscaled( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            d[i] = DataVectorDescriptive.sumUnscaled( getSampleDataAsDoubles( vector, i ), scaleType );
        }
        return d;
    }

    /**
     * Calculate the mean of each assay for a given vector.
     */
    public static double[] mean( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            d[i] = DataVectorDescriptive.mean( getSampleDataAsDoubles( vector, i ), scaleType );
        }
        return d;
    }

    /**
     * Calculate the mean of a given assay.
     */
    public static double mean( SingleCellExpressionDataVector vector, BioAssay sample ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( "Sample not found in vector" );
        }
        return DataVectorDescriptive.mean( getSampleDataAsDoubles( vector, sampleIndex ), vector.getQuantitationType().getScale() );
    }

    /**
     * Calculate the median of each assay for a given vector.
     */
    public static double[] median( SingleCellExpressionDataVector vector ) {
        DoubleArrayList vec = null;
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            double[] data = getSampleDataAsDoubles( vector, i );
            Arrays.sort( data );
            if ( vec == null ) {
                vec = new DoubleArrayList( data );
            } else {
                vec.elements( data );
            }
            d[i] = DescriptiveWithMissing.median( vec );
        }
        return d;
    }

    public static double[] sampleStandardDeviation( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            d[i] = DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsDoubles( vector, i ), scaleType );
        }
        return d;
    }

    public static double sampleStandardDeviation( SingleCellExpressionDataVector vector, BioAssay sample ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( "Sample not found in vector" );
        }
        return DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsDoubles( vector, sampleIndex ), vector.getQuantitationType().getScale() );
    }

    public static double[] sampleVariance( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            d[i] = DataVectorDescriptive.sampleVariance( getSampleDataAsDoubles( vector, i ), scaleType );
        }
        return d;
    }

    /**
     * Calculate the variance of a given assay.
     */
    public static double sampleVariance( SingleCellExpressionDataVector vector, BioAssay sample ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( "Sample not found in vector" );
        }
        return DataVectorDescriptive.sampleVariance( getSampleDataAsDoubles( vector, sampleIndex ), vector.getQuantitationType().getScale() );
    }
}
