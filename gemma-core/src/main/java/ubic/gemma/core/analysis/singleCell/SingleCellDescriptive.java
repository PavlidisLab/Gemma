package ubic.gemma.core.analysis.singleCell;

import cern.colt.list.DoubleArrayList;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.stats.DataVectorDescriptive;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

import static ubic.gemma.core.analysis.stats.DataVectorDescriptive.getMissingCountValue;
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
        if ( vector.getQuantitationType().getType() == StandardQuantitationType.PRESENTABSENT ) {
            return countPresentAbsent( vector, vector.getDataAsBooleans() );
        }
        if ( vector.getQuantitationType().getType() == StandardQuantitationType.COUNT ) {
            switch ( vector.getQuantitationType().getRepresentation() ) {
                case INT:
                    return countCount( vector, vector.getDataAsInts() );
                case LONG:
                    return countCount( vector, vector.getDataAsLongs() );
                case DOUBLE:
                    return countCount( vector, vector.getDataAsDoubles(), getMissingCountValue( vector.getQuantitationType() ) );
                default:
                    throw new UnsupportedOperationException( "Counting data represented as " + vector.getQuantitationType().getRepresentation() + " is not supported." );
            }
        }
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case BOOLEAN:
            case INT:
            case LONG:
                // these data types don't have a missing value indicator, so we can simply use (end - start) to count the
                // number of values
                return countFast( vector );
            case DOUBLE:
                return count( vector, vector.getDataAsDoubles() );
            case CHAR:
                return count( vector, vector.getDataAsChars() );
            case STRING:
                // note that for string, we cannot store "null" in a vector due to encoding limitations
                return count( vector, vector.getDataAsStrings() );
            default:
                throw new UnsupportedOperationException( "Don't know how to count data of type " + vector.getQuantitationType().getRepresentation() + "." );
        }
    }

    private static int[] countPresentAbsent( SingleCellExpressionDataVector vector, boolean[] data ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( !data[j] ) {
                    continue;
                }
                d[i]++;
            }
        }
        return d;
    }

    private static int[] count( SingleCellExpressionDataVector vector, String[] data ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( data[j].isEmpty() ) {
                    continue;
                }
                d[i]++;
            }
        }
        return d;
    }

    private static int[] count( SingleCellExpressionDataVector vector, char[] data ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( data[j] == '\0' ) {
                    continue;
                }
                d[i]++;
            }
        }
        return d;
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

    private static int[] countCount( SingleCellExpressionDataVector vector, int[] data ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( data[j] == 0 ) {
                    continue;
                }
                d[i]++;
            }
        }
        return d;
    }

    private static int[] countCount( SingleCellExpressionDataVector vector, long[] data ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( data[j] == 0L ) {
                    continue;
                }
                d[i]++;
            }
        }
        return d;
    }

    private static int[] countCount( SingleCellExpressionDataVector vector, double[] data, double defaultValue ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( Double.isNaN( data[j] ) || data[j] == defaultValue ) {
                    continue;
                }
                d[i]++;
            }
        }
        return d;
    }

    /**
     * Quickly count the non-zeroes for each assay.
     * <p>
     * Note: this is not accurate if the single-cell vector contains {@code NaN}s or actual zeroes or missing values as
     * per {@link DataVectorDescriptive#getMissingCountValue(QuantitationType)}}.
     */
    public static int[] countFast( SingleCellExpressionDataVector vector ) {
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
     * @param threshold a threshold value, assumed to be in the {@link ScaleType} of the vector
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

    public static double[] sum( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            d[i] = DataVectorDescriptive.sum( getSampleDataAsDoubles( vector, i ), scaleType );
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
