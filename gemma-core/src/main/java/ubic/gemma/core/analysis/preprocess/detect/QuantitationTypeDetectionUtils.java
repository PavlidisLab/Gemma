package ubic.gemma.core.analysis.preprocess.detect;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.hibernate.LazyInitializationException;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
public class QuantitationTypeDetectionUtils {

    public static void lintQuantitationType( QuantitationType quantitationType, ExpressionDataMatrix<?> dmatrix ) {
        try {
            lintQuantitationType( quantitationType, dmatrix, true );
        } catch ( InferredQuantitationMismatchException e ) {
            // never happens
            throw new RuntimeException( e );
        }
    }

    /**
     * Check if a given quantitation type adequately describes a given expression data matrix.
     */
    public static void lintQuantitationType( QuantitationType quantitationType, ExpressionDataMatrix<?> dmatrix, boolean ignoreQuantitationMismatch ) throws InferredQuantitationMismatchException {
        QuantitationTypeDetectionUtils.InferredQuantitationType inferredQuantitationType = infer( dmatrix, quantitationType );

        if ( quantitationType.getType() != inferredQuantitationType.getType() ) {
            String message = String.format( "The type %s differs from the one inferred from data: %s.",
                    quantitationType.getType(), inferredQuantitationType.getType() );
            // if data is meant to be detected, then
            if ( ignoreQuantitationMismatch ) {
                log.warn( message );
            } else {
                throw new InferredQuantitationMismatchException( quantitationType, inferredQuantitationType.asQuantitationType( quantitationType ), message );
            }
        }

        if ( quantitationType.getScale() != inferredQuantitationType.getScale() ) {
            String message = String.format( "The scale %s differs from the one inferred from data: %s.",
                    quantitationType.getScale(), inferredQuantitationType.getScale() );
            if ( ignoreQuantitationMismatch ) {
                log.warn( message );
            } else {
                throw new InferredQuantitationMismatchException( quantitationType, inferredQuantitationType.asQuantitationType( quantitationType ), message );
            }
        }

        if ( quantitationType.getIsRatio() != inferredQuantitationType.isRatio() ) {
            String message = String.format( "The expression data %s to ratiometric, but the quantitation says otherwise.",
                    inferredQuantitationType.isRatio() ? "appears" : "does not appear" );
            if ( ignoreQuantitationMismatch ) {
                log.warn( message );
            } else {
                throw new InferredQuantitationMismatchException( quantitationType, inferredQuantitationType.asQuantitationType( quantitationType ), message );
            }
        }
    }

    @Value
    private static class InferredQuantitationType {
        StandardQuantitationType type;
        ScaleType scale;
        boolean isRatio;

        public QuantitationType asQuantitationType( QuantitationType baseQt ) {
            QuantitationType qt = QuantitationType.Factory.newInstance( baseQt );
            qt.setType( type );
            qt.setScale( scale );
            qt.setIsRatio( isRatio );
            return qt;
        }
    }

    /**
     * Infer a {@link QuantitationType} from expression data.
     */
    public static QuantitationType inferQuantitationType( ExpressionDataMatrix<?> expressionDataDoubleMatrix ) {
        QuantitationType qt = new QuantitationType();
        InferredQuantitationType iqt = infer( expressionDataDoubleMatrix, null );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( iqt.type );
        qt.setScale( iqt.scale );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsRatio( iqt.isRatio );
        return qt;
    }

    private static InferredQuantitationType infer( ExpressionDataMatrix<?> expressionData, @Nullable QuantitationType qt ) {
        Object matrix;
        if ( expressionData instanceof ExpressionDataDoubleMatrix ) {
            matrix = new DenseDoubleMatrix2D( ( ( ExpressionDataDoubleMatrix ) expressionData ).asDoubleMatrix().asArray() );
        } else if ( expressionData instanceof SingleCellExpressionDataDoubleMatrix ) {
            matrix = ( ( SingleCellExpressionDataDoubleMatrix ) expressionData ).getMatrix();
        } else {
            throw new UnsupportedOperationException( "Unsupported expression data matrix type " + expressionData.getClass().getName() + "." );
        }

        boolean isMicroarray;
        try {
            isMicroarray = expressionData.getDesignElements().stream()
                    .map( CompositeSequence::getArrayDesign )
                    .distinct()
                    .map( ArrayDesign::getTechnologyType )
                    .anyMatch( TechnologyType.MICROARRAY::contains );
        } catch ( LazyInitializationException e ) {
            log.warn( String.format( "Failed to determine if the data matrix contains microarray platforms: %s.", e.getMessage() ) );
            isMicroarray = false;
        }

        // no data, there's nothing we can do
        if ( isEmpty( matrix ) ) {
            if ( qt == null ) {
                throw new IllegalArgumentException( "Cannot infer quantitation type without data." );
            } else {
                log.warn( String.format( "There is no data to infer quantitation type from, but a fallback QT was provided: %s", qt ) );
                return new InferredQuantitationType( qt.getType(), qt.getScale(), qt.getIsRatio() );
            }
        }

        boolean ip = isPercent( matrix );
        boolean ic = isCount( matrix );

        if ( ic && isMicroarray ) {
            log.warn( "Data appears to have been rounded and a microarray platform has been detected in the bioassays, will not treat as counts." );
            ic = false;
        }

        if ( ip && ic ) {
            log.warn( String.format( "Data only contains zeroes and ones, we don't have a binary scale so will report it as %s.",
                    ScaleType.OTHER ) );
            return new InferredQuantitationType( StandardQuantitationType.PRESENTABSENT, ScaleType.OTHER, false );
        }

        if ( ip ) {
            log.info( String.format( "Data contains values between 0 and 1, will report as %s.", ScaleType.PERCENT1 ) );
            return new InferredQuantitationType( StandardQuantitationType.AMOUNT, ScaleType.PERCENT1, false );
        }

        if ( ic ) {
            log.info( String.format( "Data appears to contains counts, will report as %s", ScaleType.COUNT ) );
            return new InferredQuantitationType( StandardQuantitationType.COUNT, ScaleType.COUNT, false );
        }

        // obtain min/max right now, they are used in tests below
        double maximum = getMaximum( matrix );

        if ( isPercent100( matrix, maximum ) ) {
            log.info( String.format( "Data contains values between 0 and 100 close enough to the boundaries, will report at %s.", ScaleType.PERCENT ) );
            return new InferredQuantitationType( StandardQuantitationType.AMOUNT, ScaleType.PERCENT, false );
        }

        if ( isZScore( matrix ) ) {
            log.info( "Data is normalized sample-wise: one or more sample has either zero mean or median." );
            return new InferredQuantitationType( StandardQuantitationType.ZSCORE, maximum < 20 ? ScaleType.LOGBASEUNKNOWN : ScaleType.OTHER, false );
        }

        // check for log-transformed counts
        InferredQuantitationType iqt;
        if ( ( iqt = isLogTransformedCount( matrix ) ) != null ) {
            return iqt;
        }

        // various upper-bounds to use for log-based scales
        // see https://github.com/PavlidisLab/Gemma/issues/600 for the rationale behind the following thresholds
        final double[] upperBounds = { 4.81, 11, 20 };
        final double[] ratiometricUpperBounds = { 2.5, 2.5, 12 };
        final ScaleType[] types = { ScaleType.LOG10, ScaleType.LOGBASEUNKNOWN, ScaleType.LOG2 };

        boolean ir = isRatiometric( matrix );
        double[] bounds = ir ? ratiometricUpperBounds : upperBounds;
        for ( int i = 0; i < bounds.length; i++ ) {
            if ( maximum < bounds[i] ) {
                log.info( String.format( "Data appears to be in the %s%s scale: maximum value observed is %f which is within bounds [%f, %f[.", ir ? "ratiometric " : "",
                        types[i], maximum, i > 0 ? bounds[i - 1] : 0.0, bounds[i] ) );
                return new InferredQuantitationType( StandardQuantitationType.AMOUNT, types[i], ir );
            }
        }

        double minimum = getMinimum( matrix );

        // negative values indicate log-transformation
        if ( minimum < 0 ) {
            return new InferredQuantitationType( StandardQuantitationType.AMOUNT, ScaleType.LOGBASEUNKNOWN, ir );
        }

        // from that point-on, it's clear that data is not log-transformed
        // there's a range of unknown between the end of LOG2 and beginning LINEAR
        ScaleType scaleType = maximum >= Math.pow( 10, 3.2 ) ? ScaleType.LINEAR : ScaleType.OTHER;

        // check if the data is log-normalized before reporting it as linear
        // because we're testing for zero (log-reported as 1 in linear scale)
        Object logMatrix = logTransform( matrix );
        if ( isZScore( logMatrix ) ) {
            log.info( "Data appears to be normalized sample-wise in the log-space." );
            return new InferredQuantitationType( StandardQuantitationType.ZSCORE, scaleType, false );
        }

        return new InferredQuantitationType( StandardQuantitationType.AMOUNT, scaleType, false );
    }

    private static double getMaximum( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return getMaximum( ( DoubleMatrix2D ) matrix );
        } else if ( matrix instanceof CompRowMatrix ) {
            return getMaximum( ( CompRowMatrix ) matrix );
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static double getMaximum( DoubleMatrix2D matrix ) {
        return Arrays.stream( matrix.toArray() )
                .flatMapToDouble( Arrays::stream )
                .filter( Double::isFinite )
                .max()
                .orElse( Double.NaN );
    }

    private static double getMaximum( CompRowMatrix matrix ) {
        return Arrays.stream( matrix.getData() )
                .filter( Double::isFinite )
                .max()
                .orElse( Double.NaN );
    }

    private static double getMinimum( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return getMinimum( ( DoubleMatrix2D ) matrix );
        } else if ( matrix instanceof CompRowMatrix ) {
            return getMinimum( ( CompRowMatrix ) matrix );
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static double getMinimum( DoubleMatrix2D matrix ) {
        return Arrays.stream( matrix.toArray() )
                .flatMapToDouble( Arrays::stream )
                .filter( Double::isFinite )
                .min()
                .orElse( Double.NaN );
    }

    private static double getMinimum( CompRowMatrix matrix ) {
        return Arrays.stream( matrix.getData() )
                .filter( Double::isFinite )
                .min()
                .orElse( Double.NaN );
    }

    private static Object logTransform( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return ( ( DoubleMatrix2D ) matrix )
                    .copy()
                    .assign( Functions.log );
        } else if ( matrix instanceof CompRowMatrix ) {
            // zero is -inf in the log-space
            CompRowMatrix copy = ( ( CompRowMatrix ) matrix ).copy();
            for ( int i = 0; i < copy.getRowPointers().length - 1; i++ ) {
                for ( int j = copy.getRowPointers()[i]; j < copy.getRowPointers()[i + 1]; j++ ) {
                    copy.set( i, copy.getColumnIndices()[j], Math.log( copy.getData()[j] ) );
                }
            }
            return copy;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Check if the given matrix is empty.
     */
    private static boolean isEmpty( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return isEmpty( ( DoubleMatrix2D ) matrix );
        } else if ( matrix instanceof CompRowMatrix ) {
            return isEmpty( ( CompRowMatrix ) matrix );
        } else {
            throw new UnsupportedOperationException( "Cannot check if matrix of type " + matrix.getClass().getName() + " is empty." );
        }
    }

    private static boolean isEmpty( DoubleMatrix2D matrix ) {
        return matrix.rows() == 0 || matrix.columns() == 0;
    }

    private static boolean isEmpty( CompRowMatrix matrix ) {
        return matrix.numRows() == 0 || matrix.numColumns() == 0;
    }

    /**
     * Check if data in the given matrix are counts.
     * <p>
     * At this stage, the data has already been transformed to double, so we only check if all the values are positive
     * and  equal to their closest integer using {@link Math#rint(double)}.
     */
    private static boolean isCount( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return isCount( ( DoubleMatrix2D ) matrix );
        } else if ( matrix instanceof CompRowMatrix ) {
            return isCount( new DenseDoubleMatrix1D( ( ( CompRowMatrix ) matrix ).getData() ) );
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean isCount( DoubleMatrix2D matrix ) {
        if ( matrix.size() < 10 ) {
            log.warn( "Matrix is too small to detect counts, will likely be reported as LINEAR." );
            return false;
        }
        boolean isCount = true;
        for ( int i = 0; i < matrix.rows(); i++ ) {
            isCount &= isCount( matrix.viewRow( i ) );
        }
        return isCount;
    }

    private static boolean isCount( DoubleMatrix1D vector ) {
        boolean isCount = true;
        for ( int j = 0; j < vector.size(); j++ ) {
            double x = vector.get( j );
            isCount &= x >= 0 && Math.rint( x ) == x;
        }
        return isCount;
    }

    /**
     * Check if the data is log-transformed counts.
     */
    @Nullable
    private static InferredQuantitationType isLogTransformedCount( Object matrix ) {
        // TODO:
        double[] distinctValues;
        if ( matrix instanceof DoubleMatrix2D ) {
            distinctValues = Arrays.stream( ( ( DoubleMatrix2D ) matrix ).viewColumn( 0 ).toArray() )
                    .filter( Double::isFinite )
                    .sorted()
                    .distinct()
                    .toArray();
        } else if ( matrix instanceof CompRowMatrix ) {
            distinctValues = Arrays.stream( ( ( CompRowMatrix ) matrix ).getData() )
                    .filter( Double::isFinite )
                    .sorted()
                    .distinct()
                    .toArray();
        } else {
            throw new UnsupportedOperationException();
        }

        if ( distinctValues.length < 10 ) {
            log.warn( "Too few values to detect log-transformed counts." );
            return null;
        }

        double[] logBases = { 2, Math.E, 10 };
        ScaleType[] scaleTypes = { ScaleType.LOG2, ScaleType.LN, ScaleType.LOG10 };
        // this is only to account for very little floating point differences between different math libraries and also
        // distinguishing log from log1p
        double atol = 1e-32;

        for ( int i = 0; i < logBases.length; i++ ) {
            double base = logBases[i];
            boolean allLogCounts = true;
            for ( double val : distinctValues ) {
                allLogCounts &= Math.abs( Math.log( Math.rint( Math.pow( base, val ) ) ) / Math.log( base ) - val ) < atol;
            }
            if ( allLogCounts ) {
                return new InferredQuantitationType( StandardQuantitationType.COUNT, scaleTypes[i], false );
            }
        }

        boolean allLogCounts = true;
        for ( double val : distinctValues ) {
            allLogCounts &= Math.abs( Math.log1p( Math.rint( Math.expm1( val ) ) ) ) - val < atol;
        }
        if ( allLogCounts ) {
            return new InferredQuantitationType( StandardQuantitationType.COUNT, ScaleType.LOG1P, false );
        }

        return null;
    }

    private static boolean isPercent( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return isPercent( ( DoubleMatrix2D ) matrix );
        } else {
            // TODO
            return false;
        }
    }

    private static boolean isPercent( DoubleMatrix2D matrix ) {
        // check if the data is within 0 and 1
        boolean isWithinZeroAndOne = true;
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                double x = matrix.get( i, j );
                isWithinZeroAndOne &= x >= 0.0 && x <= 1.0;
            }
        }
        return isWithinZeroAndOne;
    }

    private static boolean isPercent100( Object matrix, double maximum ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return isPercent100( ( DoubleMatrix2D ) matrix, maximum );
        } else {
            // TODO
            return false;
        }
    }

    /**
     * Check if data is constituted of percent ranging from 0 to 100.
     * <p>
     * To set apart those from regular log-transformed data, we also require the maximum to be close to 100 as per
     * {@link #isClose(double, double)}.
     */
    private static boolean isPercent100( DoubleMatrix2D matrix, double maximum ) {
        if ( !isClose( maximum, 100 ) ) {
            return false;
        }
        // check if the data is within 0 and 1
        boolean isWithinZeroAndOne = true;
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                double x = matrix.get( i, j );
                isWithinZeroAndOne &= x >= 0.0 && x <= 100.0;
            }
        }
        return isWithinZeroAndOne;
    }

    /**
     * Check if any of the rows of a given matrix are normalized.
     *
     * @see #isZScore(DoubleMatrix1D)
     */
    private static boolean isZScore( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return isZScore( ( DoubleMatrix2D ) matrix );
        } else if ( matrix instanceof CompRowMatrix ) {
            // TODO: but slicing column is inefficient :(
            return false;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean isZScore( DoubleMatrix2D matrix ) {
        // check if the data is Z-transformed sample-wise
        for ( int j = 0; j < matrix.columns(); j++ ) {
            if ( isZScore( matrix.viewColumn( j ) ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a vector is normalized by having either zero mean, one standard deviation or zero median.
     */
    private static boolean isZScore( DoubleMatrix1D vector ) {
        DoubleArrayList v = new DoubleArrayList( vector.toArray() );
        double mean = DescriptiveWithMissing.mean( v );
        if ( isCloseToZero( mean ) ) {
            double var = DescriptiveWithMissing.sampleVariance( v, mean );
            if ( !isClose( var, 1 ) ) {
                log.warn( String.format( "Mean is zero, but standard deviation %f is not close enough to one. Will still report as Z-score.", Math.sqrt( var ) ) );
            }
            return true;
        }
        // FIXME: use a faster algorithm for the median, there's a O(n) approach
        return isCloseToZero( DescriptiveWithMissing.median( v ) );
    }

    /**
     * If the data is centered around zero, but not exactly as per {@link #isZScore(DoubleMatrix1D)} it may be
     * ratiometric as it the log ratio (generally 2) of two raw signals. When that occurs, the max has to be interpreted
     * differently.
     */
    private static boolean isRatiometric( Object matrix ) {
        if ( matrix instanceof DoubleMatrix2D ) {
            return isRatiometric( ( DoubleMatrix2D ) matrix );
        } else {
            // TODO
            return false;
        }
    }

    private static boolean isRatiometric( DoubleMatrix2D matrix ) {
        boolean ratiometric = true;
        for ( int j = 0; j < matrix.columns(); j++ ) {
            double mean = DescriptiveWithMissing.mean( new DoubleArrayList( matrix.viewColumn( j ).toArray() ) );
            ratiometric &= Math.abs( mean ) < 2;
        }
        return ratiometric;
    }

    private static final double ATOL = 1e-8;
    private static final double RTOL = 1e-5;

    /**
     * Check if a double is close to zero.
     * <p>
     * The default is borrowed from Numpy's <a href="https://numpy.org/doc/stable/reference/generated/numpy.isclose.html">isclose</a>.
     * The relative tolerance does not have to be accounted for comparing to zero.
     */
    private static boolean isCloseToZero( double a ) {
        return a == 0.0 || Math.abs( a ) < ATOL;
    }

    private static boolean isClose( double a, double b ) {
        return a == b || Math.abs( a - b ) < RTOL * Math.abs( b ) + ATOL;
    }

    /**
     * Detect suspicious values for a given quantitation type.
     *
     * @throws SuspiciousValuesForQuantitationException if there are any suspicious values
     */
    public static void detectSuspiciousValues( ExpressionDataDoubleMatrix a, QuantitationType qt ) throws SuspiciousValuesForQuantitationException {
        DoubleMatrix2D matrix = new DenseDoubleMatrix2D( a.getMatrixAsDoubles() );

        List<SuspiciousValuesForQuantitationException.SuspiciousValueResult> flaggingResults = new ArrayList<>();

        // TODO: handle normalization and background-substracted linting
        if ( qt.getIsBackgroundSubtracted() || qt.getIsNormalized() ) {
            log.warn( "Expression data is either background subtracted or normalized, suspicious values will not be flagged." );
            return;
        }

        String[] columnNames = a.getBioAssayDimension().getBioAssays().stream()
                .map( BioAssay::getName )
                .toArray( String[]::new );

        switch ( qt.getScale() ) {
            case LOG2:
                if ( qt.getIsRatio() ) {
                    ensureWithin( matrix, columnNames, -15, 15, flaggingResults );
                    ensureWithin( matrix, columnNames, "mean", DescriptiveWithMissing::mean, -0.5, 0.5, flaggingResults );
                } else {
                    ensureWithin( matrix, columnNames, 0, 20, flaggingResults );
                    ensureOutside( matrix, columnNames, "mean", DescriptiveWithMissing::mean, -0.1, 0.1, flaggingResults );
                }
                break;
            case LOG10: // basically 0.3 time the log2 thresholds since log(2)/log(10) = 0.3
                if ( qt.getIsRatio() ) {
                    ensureWithin( matrix, columnNames, -4.5, 4.5, flaggingResults );
                    ensureWithin( matrix, columnNames, "mean", DescriptiveWithMissing::mean, -0.5, 0.5, flaggingResults );
                } else {
                    ensureWithin( matrix, columnNames, 0, 9, flaggingResults );
                    ensureOutside( matrix, columnNames, "mean", DescriptiveWithMissing::mean, -0.03, 0.03, flaggingResults );
                }
                break;
            case LINEAR:
                if ( qt.getIsRatio() ) {
                    flaggingResults.add( new SuspiciousValuesForQuantitationException.SuspiciousValueResult( -1, null, -1, null, "Linear data should not be ratiometric" ) );
                } else {
                    ensureWithin( matrix, columnNames, 1e-3, 1e6, flaggingResults );
                    ensureWithin( matrix, columnNames, "mean", DescriptiveWithMissing::mean, 50, 10000, flaggingResults );
                }
                break;
            case COUNT:
                ensureWithin( matrix, columnNames, 0, 1e8, flaggingResults );
                if ( !isCount( matrix ) ) {
                    flaggingResults.add( new SuspiciousValuesForQuantitationException.SuspiciousValueResult( -1, null, -1, null, "Counting data contains a non-integer valus." ) );
                }
                break;
        }

        if ( !flaggingResults.isEmpty() ) {
            throw new SuspiciousValuesForQuantitationException( qt, "Expression data matrix contains suspicious values.", flaggingResults );
        }
    }

    @FunctionalInterface
    private interface DescriptiveFunction {
        double apply( DoubleArrayList a );
    }

    private static void ensureWithin( DoubleMatrix2D a, String[] columnNames, double lowerBound, double upperBound, List<SuspiciousValuesForQuantitationException.SuspiciousValueResult> results ) {
        for ( int j = 0; j < a.columns(); j++ ) {
            DoubleArrayList col = new DoubleArrayList( a.viewColumn( j ).toArray() );
            double minimum = DescriptiveWithMissing.min( col );
            double maximum = DescriptiveWithMissing.max( col );
            if ( minimum < lowerBound ) {
                results.add( new SuspiciousValuesForQuantitationException.SuspiciousValueResult( -1, null, j, columnNames[j], String.format( "minimum of %.2f is too small; lower bound is %.2f", minimum, lowerBound ) ) );
            }
            if ( maximum > upperBound ) {
                results.add( new SuspiciousValuesForQuantitationException.SuspiciousValueResult( -1, null, j, columnNames[j], String.format( "maximum of %.2f is too high; upper bound is %.2f", maximum, upperBound ) ) );
            }
        }
    }

    private static void ensureWithin( DoubleMatrix2D a, String[] columnNames, String funcName, DescriptiveFunction func, double lowerBound, double upperBound, List<SuspiciousValuesForQuantitationException.SuspiciousValueResult> results ) {
        for ( int j = 0; j < a.columns(); j++ ) {
            DoubleArrayList col = new DoubleArrayList( a.viewColumn( j ).toArray() );
            double r = func.apply( col );
            if ( r < lowerBound || r > upperBound ) {
                results.add( new SuspiciousValuesForQuantitationException.SuspiciousValueResult( -1, null, j, columnNames[j], String.format( "%.2f is outside expected range of [%.2f, %.2f] for %f", r, lowerBound, upperBound, funcName ) ) );
            }
        }
    }

    private static void ensureOutside( DoubleMatrix2D a, String[] columnNames, String funcName, DescriptiveFunction func, double lowerBound, double upperBound, List<SuspiciousValuesForQuantitationException.SuspiciousValueResult> results ) {
        for ( int j = 0; j < a.columns(); j++ ) {
            DoubleArrayList col = new DoubleArrayList( a.viewColumn( j ).toArray() );
            double r = func.apply( col );
            if ( r >= lowerBound && r <= upperBound ) {
                results.add( new SuspiciousValuesForQuantitationException.SuspiciousValueResult( -1, null, j, columnNames[j], String.format( "%.2f is inside suspicious range of [%f, %f] for %s", r, lowerBound, upperBound, funcName ) ) );
            }
        }
    }
}
