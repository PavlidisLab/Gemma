package ubic.gemma.core.analysis.preprocess.detect;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.LazyInitializationException;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
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

    @Value
    public static class InferredQuantitationType {
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
    public static QuantitationType inferQuantitationType( ExpressionDataDoubleMatrix expressionDataDoubleMatrix ) {
        QuantitationType qt = new QuantitationType();
        InferredQuantitationType iqt = infer( expressionDataDoubleMatrix, null );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( iqt.type );
        qt.setScale( iqt.scale );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsRatio( iqt.isRatio );
        return qt;
    }

    public static InferredQuantitationType infer( ExpressionDataDoubleMatrix expressionData, @Nullable QuantitationType qt ) {
        DoubleMatrix2D matrix = new DenseDoubleMatrix2D( expressionData.getMatrix().asArray() );

        boolean isMicroarray;
        try {
            isMicroarray = expressionData.getRowNames().stream()
                    .map( CompositeSequence::getArrayDesign )
                    .distinct()
                    .map( ArrayDesign::getTechnologyType )
                    .anyMatch( TechnologyType.MICROARRAY::contains );
        } catch ( LazyInitializationException e ) {
            log.warn( String.format( "Failed to determine if the data matrix contains microarray platforms: %s.", e.getMessage() ) );
            isMicroarray = false;
        }

        // no data, there's nothing we can do
        if ( matrix.rows() == 0 || matrix.columns() == 0 ) {
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
        double maximum = Arrays.stream( matrix.toArray() )
                .flatMapToDouble( Arrays::stream )
                .filter( Double::isFinite )
                .max()
                .orElseThrow( RuntimeException::new );

        if ( isPercent100( matrix, maximum ) ) {
            log.info( String.format( "Data contains values between 0 and 100 close enough to the boundaries, will report at %s.", ScaleType.PERCENT ) );
            return new InferredQuantitationType( StandardQuantitationType.AMOUNT, ScaleType.PERCENT, false );
        }

        if ( isZScore( matrix ) ) {
            log.info( "Data is normalized sample-wise: one or more sample has either zero mean or median." );
            return new InferredQuantitationType( StandardQuantitationType.ZSCORE, maximum < 20 ? ScaleType.LOGBASEUNKNOWN : ScaleType.OTHER, false );
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

        double minimum = Arrays.stream( matrix.toArray() )
                .flatMapToDouble( Arrays::stream )
                .filter( Double::isFinite )
                .min()
                .orElseThrow( RuntimeException::new );

        // negative values indicate log-transformation
        if ( minimum < 0 ) {
            return new InferredQuantitationType( StandardQuantitationType.AMOUNT, ScaleType.LOGBASEUNKNOWN, ir );
        }

        // from that point-on, it's clear that data is not log-transformed
        // there's a range of unknown between the end of LOG2 and beginning LINEAR
        ScaleType scaleType = maximum >= Math.pow( 10, 3.2 ) ? ScaleType.LINEAR : ScaleType.OTHER;

        // check if the data is log-normalized before reporting it as linear
        // because we're testing for zero (log-reported as 1 in linear scale)
        DoubleMatrix2D logMatrix = matrix
                .copy()
                .assign( Functions.log );
        if ( isZScore( logMatrix ) ) {
            log.info( "Data appears to be normalized sample-wise in the log-space." );
            return new InferredQuantitationType( StandardQuantitationType.ZSCORE, scaleType, false );
        }

        return new InferredQuantitationType( StandardQuantitationType.AMOUNT, scaleType, false );
    }

    /**
     * Check if data in the given matrix are counts.
     * <p>
     * At this stage, the data has already been transformed to double, so we only check if all the values are positive
     * and  equal to their closest integer using {@link Math#rint(double)}.
     */
    private static boolean isCount( DoubleMatrix2D matrix ) {
        if ( matrix.size() < 10 ) {
            log.warn( "Matrix is too small to detect counts, will likely be reported as LINEAR." );
            return false;
        }
        boolean isCount = true;
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                double x = matrix.get( i, j );
                isCount &= x >= 0 && Math.rint( x ) == x;
            }
        }
        return isCount;
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
     * @see #isZScore(DoubleMatrix1D)
     */
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
     * @throws SuspiciousValuesForQuantitationException if there are any suspicious values
     */
    public static void detectSuspiciousValues( ExpressionDataDoubleMatrix a, QuantitationType qt ) throws SuspiciousValuesForQuantitationException {
        DoubleMatrix2D matrix = new DenseDoubleMatrix2D( a.getMatrix().asArray() );

        List<SuspiciousValuesForQuantitationException.SuspiciousValueResult> flaggingResults = new ArrayList<>();

        // TODO: handle normalization and background-substracted linting
        if ( qt.getIsBackgroundSubtracted() || qt.getIsNormalized() ) {
            log.warn( "Expression data is either background subtracted or normalized, suspicious values will not be flagged." );
            return;
        }

        String[] columnNames = a.getBestBioAssayDimension().getBioAssays().stream()
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