package ubic.gemma.core.analysis.singleCell;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;
import org.springframework.util.Assert;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.analysis.stats.DataVectorDescriptive;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;

import java.util.function.ToDoubleFunction;

import static ubic.gemma.core.analysis.stats.DataVectorDescriptive.getMissingCountValue;
import static ubic.gemma.core.analysis.stats.DataVectorDescriptive.getMissingFloatCountValue;
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
        return applyDescriptive( vector, DescriptiveWithMissing::max, "max" );
    }

    public static double max( SingleCellExpressionDataVector vector, int sampleIndex ) {
        return applyDescriptive( vector, sampleIndex, DescriptiveWithMissing::max, "max" );
    }

    public static double max( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        return applyDescriptive( vector, sampleIndex, cellLevelCharacteristics, row, DescriptiveWithMissing::max, "max" );
    }

    public static double[] min( SingleCellExpressionDataVector vector ) {
        return applyDescriptive( vector, DescriptiveWithMissing::min, "min" );
    }

    public static double min( SingleCellExpressionDataVector vector, int sampleIndex ) {
        return applyDescriptive( vector, sampleIndex, DescriptiveWithMissing::min, "min" );
    }

    public static double min( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        return applyDescriptive( vector, sampleIndex, cellLevelCharacteristics, row, DescriptiveWithMissing::min, "min" );
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
                case FLOAT:
                    return countCount( vector, vector.getDataAsFloats(), getMissingFloatCountValue( vector.getQuantitationType() ) );
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
            case FLOAT:
                return count( vector, vector.getDataAsFloats() );
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

    private static int[] count( SingleCellExpressionDataVector vector, float[] data ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( Float.isNaN( data[j] ) ) {
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


    private static int[] countCount( SingleCellExpressionDataVector vector, float[] data, float defaultValue ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            int start = getSampleStart( vector, i, 0 );
            int end = getSampleEnd( vector, i, start );
            for ( int j = start; j < end; j++ ) {
                if ( Float.isNaN( data[j] ) || data[j] == defaultValue ) {
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
            d[i] = countFast( vector, i );
        }
        return d;
    }

    public static int countFast( SingleCellExpressionDataVector vector, int sampleIndex ) {
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        return end - start;
    }

    public static int countFast( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        Assert.isTrue( row >= -1 && row < cellLevelCharacteristics.getNumberOfCharacteristics() );
        int start = getSampleStart( vector, sampleIndex, 0 );
        int end = getSampleEnd( vector, sampleIndex, start );
        int count = 0;
        for ( int i = start; i < end; i++ ) {
            if ( cellLevelCharacteristics.getIndices()[i] == row ) {
                count++;
            }
        }
        return count;
    }

    /**
     * Compute the number of cells expressed strictly above the given threshold.
     * @param threshold a threshold value, assumed to be in the {@link ScaleType} of the vector
     */
    public static int[] countAboveThreshold( SingleCellExpressionDataVector vector, double threshold ) {
        int[] d = new int[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            switch ( vector.getQuantitationType().getRepresentation() ) {
                case FLOAT:
                    d[i] = countAboveThreshold( getSampleDataAsFloats( vector, i ), threshold );
                    break;
                case DOUBLE:
                    d[i] = countAboveThreshold( getSampleDataAsDoubles( vector, i ), threshold );
                    break;
                case INT:
                    d[i] = countAboveThreshold( getSampleDataAsInts( vector, i ), threshold );
                    break;
                case LONG:
                    d[i] = countAboveThreshold( getSampleDataAsLongs( vector, i ), threshold );
                    break;
                default:
                    throw new UnsupportedOperationException( "Unsupported representation " + vector.getQuantitationType().getRepresentation() );

            }
        }
        return d;
    }

    private static int countAboveThreshold( double[] data, double threshold ) {
        int d = 0;
        for ( double s : data ) {
            if ( s > threshold ) {
                d++;
            }
        }
        return d;
    }

    private static int countAboveThreshold( float[] data, double threshold ) {
        int d = 0;
        for ( float s : data ) {
            if ( s > threshold ) {
                d++;
            }
        }
        return d;
    }

    private static int countAboveThreshold( int[] data, double threshold ) {
        int d = 0;
        for ( int s : data ) {
            if ( s > threshold ) {
                d++;
            }
        }
        return d;
    }

    private static int countAboveThreshold( long[] data, double threshold ) {
        int d = 0;
        for ( long s : data ) {
            if ( s > threshold ) {
                d++;
            }
        }
        return d;
    }

    private static double[] applyDescriptive( SingleCellExpressionDataVector vector, ToDoubleFunction<DoubleArrayList> func, String operation ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return applyFloatDescriptive( vector, func );
            case DOUBLE:
                return applyDoubleDescriptive( vector, func );
            case INT:
                return applyIntDescriptive( vector, func );
            case LONG:
                return applyLongDescriptive( vector, func );
            default:
                throw unsupportedRepresentation( vector.getQuantitationType().getRepresentation(), operation );
        }
    }

    private static double[] applyFloatDescriptive( SingleCellExpressionDataVector vector, ToDoubleFunction<DoubleArrayList> func ) {
        DoubleArrayList vec = null;
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            double[] data = float2double( getSampleDataAsFloats( vector, i ) );
            if ( vec == null ) {
                vec = new DoubleArrayList( data );
            } else {
                vec.elements( data );
            }
            d[i] = func.applyAsDouble( vec );
        }
        return d;
    }

    private static double[] applyDoubleDescriptive( SingleCellExpressionDataVector vector, ToDoubleFunction<DoubleArrayList> func ) {
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

    private static double[] applyIntDescriptive( SingleCellExpressionDataVector vector, ToDoubleFunction<DoubleArrayList> func ) {
        DoubleArrayList vec = null;
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            double[] data = int2double( getSampleDataAsInts( vector, i ) );
            if ( vec == null ) {
                vec = new DoubleArrayList( data );
            } else {
                vec.elements( data );
            }
            d[i] = func.applyAsDouble( vec );
        }
        return d;
    }

    private static double[] applyLongDescriptive( SingleCellExpressionDataVector vector, ToDoubleFunction<DoubleArrayList> func ) {
        DoubleArrayList vec = null;
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            double[] data = long2double( getSampleDataAsLongs( vector, i ) );
            if ( vec == null ) {
                vec = new DoubleArrayList( data );
            } else {
                vec.elements( data );
            }
            d[i] = func.applyAsDouble( vec );
        }
        return d;
    }

    private static double applyDescriptive( SingleCellExpressionDataVector vector, int sampleIndex, ToDoubleFunction<DoubleArrayList> func, String operation ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return func.applyAsDouble( new DoubleArrayList( float2double( getSampleDataAsFloats( vector, sampleIndex ) ) ) );
            case DOUBLE:
                return func.applyAsDouble( new DoubleArrayList( getSampleDataAsDoubles( vector, sampleIndex ) ) );
            case INT:
                return func.applyAsDouble( new DoubleArrayList( int2double( getSampleDataAsInts( vector, sampleIndex ) ) ) );
            case LONG:
                return func.applyAsDouble( new DoubleArrayList( long2double( getSampleDataAsLongs( vector, sampleIndex ) ) ) );
            default:
                throw unsupportedRepresentation( vector.getQuantitationType().getRepresentation(), operation );
        }
    }

    private static double applyDescriptive( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row, ToDoubleFunction<DoubleArrayList> func, String operation ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return func.applyAsDouble( new DoubleArrayList( float2double( getSampleDataAsFloats( vector, sampleIndex, cellLevelCharacteristics, row ) ) ) );
            case DOUBLE:
                return func.applyAsDouble( new DoubleArrayList( getSampleDataAsDoubles( vector, sampleIndex, cellLevelCharacteristics, row ) ) );
            case INT:
                return func.applyAsDouble( new DoubleArrayList( int2double( getSampleDataAsInts( vector, sampleIndex, cellLevelCharacteristics, row ) ) ) );
            case LONG:
                return func.applyAsDouble( new DoubleArrayList( long2double( getSampleDataAsLongs( vector, sampleIndex, cellLevelCharacteristics, row ) ) ) );
            default:
                throw unsupportedRepresentation( vector.getQuantitationType().getRepresentation(), operation );
        }
    }

    public static double[] sum( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            switch ( representation ) {
                case FLOAT:
                    d[i] = DataVectorDescriptive.sum( getSampleDataAsFloats( vector, i ), scaleType );
                    break;
                case DOUBLE:
                    d[i] = DataVectorDescriptive.sum( getSampleDataAsDoubles( vector, i ), scaleType );
                    break;
                case INT:
                    d[i] = DataVectorDescriptive.sum( getSampleDataAsInts( vector, i ), scaleType );
                    break;
                case LONG:
                    d[i] = DataVectorDescriptive.sum( getSampleDataAsLongs( vector, i ), scaleType );
                    break;
                default:
                    throw unsupportedRepresentation( representation, "sum" );
            }
        }
        return d;
    }

    public static double[] sumUnscaled( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            switch ( representation ) {
                case FLOAT:
                    d[i] = DataVectorDescriptive.sumUnscaled( getSampleDataAsFloats( vector, i ), scaleType );
                    break;
                case DOUBLE:
                    d[i] = DataVectorDescriptive.sumUnscaled( getSampleDataAsDoubles( vector, i ), scaleType );
                    break;
                case INT:
                    d[i] = DataVectorDescriptive.sumUnscaled( getSampleDataAsInts( vector, i ), scaleType );
                    break;
                case LONG:
                    d[i] = DataVectorDescriptive.sumUnscaled( getSampleDataAsLongs( vector, i ), scaleType );
                    break;
                default:
                    throw unsupportedRepresentation( representation, "sumUnscaled" );
            }
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
            switch ( vector.getQuantitationType().getRepresentation() ) {
                case FLOAT:
                    d[i] = DataVectorDescriptive.mean( getSampleDataAsFloats( vector, i ), scaleType );
                    break;
                case DOUBLE:
                    d[i] = DataVectorDescriptive.mean( getSampleDataAsDoubles( vector, i ), scaleType );
                    break;
                case INT:
                    d[i] = DataVectorDescriptive.mean( getSampleDataAsInts( vector, i ), scaleType );
                    break;
                case LONG:
                    d[i] = DataVectorDescriptive.mean( getSampleDataAsLongs( vector, i ), scaleType );
                    break;
                default:
                    throw unsupportedRepresentation( vector.getQuantitationType().getRepresentation(), "mean" );
            }
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
        return mean( vector, sampleIndex );
    }

    public static double mean( SingleCellExpressionDataVector vector, int sampleIndex ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return DataVectorDescriptive.mean( getSampleDataAsFloats( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case DOUBLE:
                return DataVectorDescriptive.mean( getSampleDataAsDoubles( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case INT:
                return DataVectorDescriptive.mean( getSampleDataAsInts( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case LONG:
                return DataVectorDescriptive.mean( getSampleDataAsLongs( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            default:
                throw unsupportedRepresentation( vector.getQuantitationType().getRepresentation(), "mean" );
        }
    }

    public static double mean( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case FLOAT:
                return DataVectorDescriptive.mean( getSampleDataAsFloats( vector, sampleIndex, cellLevelCharacteristics, row ), vector.getQuantitationType().getScale() );
            case DOUBLE:
                return DataVectorDescriptive.mean( getSampleDataAsDoubles( vector, sampleIndex, cellLevelCharacteristics, row ), vector.getQuantitationType().getScale() );
            case INT:
                return DataVectorDescriptive.mean( getSampleDataAsInts( vector, sampleIndex, cellLevelCharacteristics, row ), vector.getQuantitationType().getScale() );
            case LONG:
                return DataVectorDescriptive.mean( getSampleDataAsLongs( vector, sampleIndex, cellLevelCharacteristics, row ), vector.getQuantitationType().getScale() );
            default:
                throw unsupportedRepresentation( vector.getQuantitationType().getRepresentation(), "mean" );
        }
    }

    /**
     * Calculate the median of each assay for a given vector.
     */
    public static double[] median( SingleCellExpressionDataVector vector ) {
        return quantile( vector, 0.5 );
    }

    public static double median( SingleCellExpressionDataVector vector, int column ) {
        return quantile( vector, 0.5 )[column];
    }

    public static double median( SingleCellExpressionDataVector vector, int column, CellLevelCharacteristics cellLevelCharacteristics, int row ) {
        return quantile( vector, column, cellLevelCharacteristics, row, 0.5 );
    }

    public static double[] quantile( SingleCellExpressionDataVector vector, double q ) {
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        DoubleArrayList vec = null;
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            double[] data;
            switch ( representation ) {
                case FLOAT:
                    data = float2double( getSampleDataAsFloats( vector, i ) );
                    break;
                case DOUBLE:
                    data = getSampleDataAsDoubles( vector, i );
                    break;
                case INT:
                    data = int2double( getSampleDataAsInts( vector, i ) );
                    break;
                case LONG:
                    data = long2double( getSampleDataAsLongs( vector, i ) );
                    break;
                default:
                    throw unsupportedRepresentation( representation, "median" );
            }
            if ( vec == null ) {
                vec = new DoubleArrayList( data );
            } else {
                vec.elements( data );
            }
            if ( representation == PrimitiveType.FLOAT || representation == PrimitiveType.DOUBLE ) {
                // baseCode will sort it for us
                d[i] = DescriptiveWithMissing.quantile( vec, q );
            } else {
                // colt does not sort data... :S
                vec.sort();
                d[i] = Descriptive.quantile( vec, q );
            }
        }
        return d;
    }

    public static double quantile( SingleCellExpressionDataVector vector, int sampleIndex, double v ) {
        return quantile( vector, v )[sampleIndex];
    }

    public static double[] quantile( SingleCellExpressionDataVector vector, CellLevelCharacteristics cellLevelCharacteristics, int row, double q ) {
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        DoubleArrayList vec = null;
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            double[] data;
            switch ( representation ) {
                case FLOAT:
                    data = float2double( getSampleDataAsFloats( vector, i, cellLevelCharacteristics, row ) );
                    break;
                case DOUBLE:
                    data = getSampleDataAsDoubles( vector, i, cellLevelCharacteristics, row );
                    break;
                case INT:
                    data = int2double( getSampleDataAsInts( vector, i, cellLevelCharacteristics, row ) );
                    break;
                case LONG:
                    data = long2double( getSampleDataAsLongs( vector, i, cellLevelCharacteristics, row ) );
                    break;
                default:
                    throw unsupportedRepresentation( representation, "median" );
            }
            if ( vec == null ) {
                vec = new DoubleArrayList( data );
            } else {
                vec.elements( data );
            }
            if ( representation == PrimitiveType.FLOAT || representation == PrimitiveType.DOUBLE ) {
                // baseCode will sort it for us
                d[i] = DescriptiveWithMissing.quantile( vec, q );
            } else {
                // colt does not sort data... :S
                vec.sort();
                d[i] = Descriptive.quantile( vec, q );
            }
        }
        return d;
    }

    public static double quantile( SingleCellExpressionDataVector vector, int sampleIndex, CellLevelCharacteristics cellLevelCharacteristics, int row, double q ) {
        return quantile( vector, cellLevelCharacteristics, row, q )[sampleIndex];
    }

    public static double[] sampleStandardDeviation( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            switch ( representation ) {
                case FLOAT:
                    d[i] = DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsFloats( vector, i ), scaleType );
                    break;
                case DOUBLE:
                    d[i] = DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsDoubles( vector, i ), scaleType );
                    break;
                case INT:
                    d[i] = DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsInts( vector, i ), scaleType );
                    break;
                case LONG:
                    d[i] = DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsLongs( vector, i ), scaleType );
                    break;
                default:
                    throw unsupportedRepresentation( representation, "sampleStandardDeviation" );
            }
        }
        return d;
    }

    public static double sampleStandardDeviation( SingleCellExpressionDataVector vector, BioAssay sample ) {
        int sampleIndex = vector.getSingleCellDimension().getBioAssays().indexOf( sample );
        if ( sampleIndex == -1 ) {
            throw new IllegalArgumentException( "Sample not found in vector" );
        }
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        switch ( representation ) {
            case FLOAT:
                return DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsFloats( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case DOUBLE:
                return DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsDoubles( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case INT:
                return DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsInts( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case LONG:
                return DataVectorDescriptive.sampleStandardDeviation( getSampleDataAsLongs( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            default:
                throw unsupportedRepresentation( representation, "sampleStandardDeviation" );
        }
    }

    public static double[] sampleVariance( SingleCellExpressionDataVector vector ) {
        ScaleType scaleType = vector.getQuantitationType().getScale();
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        double[] d = new double[vector.getSingleCellDimension().getBioAssays().size()];
        for ( int i = 0; i < d.length; i++ ) {
            switch ( representation ) {
                case FLOAT:
                    d[i] = DataVectorDescriptive.sampleVariance( getSampleDataAsFloats( vector, i ), scaleType );
                    break;
                case DOUBLE:
                    d[i] = DataVectorDescriptive.sampleVariance( getSampleDataAsDoubles( vector, i ), scaleType );
                    break;
                case INT:
                    d[i] = DataVectorDescriptive.sampleVariance( getSampleDataAsInts( vector, i ), scaleType );
                    break;
                case LONG:
                    d[i] = DataVectorDescriptive.sampleVariance( getSampleDataAsLongs( vector, i ), scaleType );
                    break;
                default:
                    throw unsupportedRepresentation( representation, "sampleVariance" );
            }
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
        PrimitiveType representation = vector.getQuantitationType().getRepresentation();
        switch ( representation ) {
            case FLOAT:
                return DataVectorDescriptive.sampleVariance( getSampleDataAsFloats( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case DOUBLE:
                return DataVectorDescriptive.sampleVariance( getSampleDataAsDoubles( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case INT:
                return DataVectorDescriptive.sampleVariance( getSampleDataAsInts( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            case LONG:
                return DataVectorDescriptive.sampleVariance( getSampleDataAsLongs( vector, sampleIndex ), vector.getQuantitationType().getScale() );
            default:
                throw unsupportedRepresentation( representation, "sampleVariance" );
        }
    }

    private static double[] float2double( float[] data ) {
        double[] dataAsDoubles = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            dataAsDoubles[i] = data[i];
        }
        return dataAsDoubles;
    }

    private static double[] int2double( int[] data ) {
        double[] dataAsDoubles = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            dataAsDoubles[i] = data[i];
        }
        return dataAsDoubles;
    }

    private static double[] long2double( long[] data ) {
        double[] dataAsDoubles = new double[data.length];
        for ( int i = 0; i < data.length; i++ ) {
            dataAsDoubles[i] = data[i];
        }
        return dataAsDoubles;
    }

    private static UnsupportedOperationException unsupportedRepresentation( PrimitiveType representation, String operation ) {
        return new UnsupportedOperationException( "Unsupported representation " + representation + " for " + operation + "." );
    }
}
