/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.datastructure.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.jet.math.Functions;
import lombok.Value;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LazyInitializationException;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.UnsupportedQuantitationScaleConversionException;
import ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Perform various computations on ExpressionDataMatrices (usually in-place).
 *
 * @author pavlidis
 */
public class ExpressionDataDoubleMatrixUtil {

    private static final double LOGARITHM_BASE = 2.0;


    /**
     * This threshold is used to determine if a row has too many identical value; a value of N means that the number of distinct values in the
     * expression vector of length M must be at least N * M.
     */
    private static final double MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT = 0.3;

    /**
     * We don't apply the "unique values" filter to matrices with fewer columns than this.
     */
    private static final int MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER = 4;


    private static final Log log = LogFactory.getLog( ExpressionDataDoubleMatrixUtil.class.getName() );

    /**
     * Log2 transform if necessary, do any required filtering prior to analysis. Count data is converted to log2CPM (but
     * we store log2cpm as the processed data, so that is what would generally be used).
     *
     * @param dmatrix matrix
     * @return ee data double matrix
     */
    public static ExpressionDataDoubleMatrix filterAndLog2Transform( ExpressionDataDoubleMatrix dmatrix ) {
        dmatrix = ExpressionDataDoubleMatrixUtil.ensureLog2Scale( dmatrix );

        /*
         * We do this second because doing it first causes some kind of subtle problem ... (round off? I could not
         * really track this down).
         *
         * Remove zero-variance rows, but also rows that have lots of equal values even if variance is non-zero.
         * Filtering out rows that have many identical values helps avoid p-values that clump around specific values in the data.
         * This happens especially for lowly-expressed genes in RNA-seq but can be observed in microarray
         * data that has been manipulated by the submitter e.g. when data is "clipped" (e.g., all values under 10 set to 10).
         */
        int r = dmatrix.rows();
        dmatrix = ExpressionExperimentFilter.zeroVarianceFilter( dmatrix );
        if ( dmatrix.rows() < r ) {
            ExpressionDataDoubleMatrixUtil.log.info( ( r - dmatrix.rows() ) + " rows removed due to low variance" );
        }
        r = dmatrix.rows();

        if ( dmatrix.columns() > ExpressionDataDoubleMatrixUtil.MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER ) {
            /* This threshold had been 10^-5, but it's probably too stringent. Also remember
             * the data are log transformed the threshold should be transformed as well (it's not that simple),
             * but that's a minor effect.
             * To somewhat counter the effect of lowering this stringency, increasing the stringency on VALUES_LIMIT may help */
            dmatrix = ExpressionExperimentFilter.tooFewDistinctValues( dmatrix, ExpressionDataDoubleMatrixUtil.MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT, 0.001 );
            if ( dmatrix.rows() < r ) {
                ExpressionDataDoubleMatrixUtil.log.info( ( r - dmatrix.rows() ) + " rows removed due to too many identical values" );
            }
        }

        return dmatrix;

    }

    /**
     * Ensures that the given matrix is on a Log2 scale.
     * ! Does not update the QT !
     *
     * @param dmatrix                    the matrix to be transformed to a log2 scale if necessary.
     * @param ignoreQuantitationMismatch if true, ignore mismatch between matrix quantitation types and that inferred
     *                                   from data
     * @throws QuantitationTypeConversionException if transformation to log2 scale is impossible
     * @throws InferredQuantitationMismatchException              if the inferred scale type differs that inferred from data
     * @return a data matrix that is guaranteed to be on a log2 scale or the original input matrix if it was already the
     * case
     */
    @CheckReturnValue
    public static ExpressionDataDoubleMatrix ensureLog2Scale( ExpressionDataDoubleMatrix dmatrix, boolean ignoreQuantitationMismatch ) throws QuantitationMismatchException {
        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();
        if ( quantitationType == null ) {
            throw new IllegalArgumentException( "Expression data matrix lacks a quantitation type." );
        }
        if ( isHeterogeneous( dmatrix ) ) {
            throw new IllegalArgumentException( "Transforming a dataset to log2 scale with mixed quantitation types is not supported." );
        }
        if ( quantitationType.getGeneralType() != GeneralType.QUANTITATIVE ) {
            throw new IllegalArgumentException( "Only quantitative data is supported on a log2 scale." );
        }
        if ( quantitationType.getType() != StandardQuantitationType.AMOUNT && quantitationType.getType() != StandardQuantitationType.COUNT ) {
            throw new IllegalArgumentException( "Only amounts and counts can be represented on a log2 scale." );
        }
        if ( quantitationType.getRepresentation() != PrimitiveType.DOUBLE ) {
            throw new IllegalArgumentException( ( String.format( "This method only support expression matrices of doubles, but the quantitation type claims the data contains %s.",
                    quantitationType.getRepresentation() ) ) );
        }

        InferredQuantitationType inferredQuantitationType = infer( dmatrix );

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

        if ( quantitationType.getIsRatio() != inferredQuantitationType.isRatio ) {
            String message = String.format( "The expression data %s to ratiometric, but the quantitation says otherwise.",
                    inferredQuantitationType.isRatio ? "appears" : "does not appear" );
            if ( ignoreQuantitationMismatch ) {
                log.warn( message );
            } else {
                throw new InferredQuantitationMismatchException( quantitationType, inferredQuantitationType.asQuantitationType( quantitationType ), message );
            }
        }

        // special case for log2, we don't need to transform anything
        if ( quantitationType.getScale() == ScaleType.LOG2 ) {
            log.info( "Data is already on a log2-scale, will not transform it." );
            return dmatrix;
        }

        StandardQuantitationType type = quantitationType.getType();
        DoubleMatrix<CompositeSequence, BioMaterial> transformedMatrix = dmatrix.getMatrix().copy();
        switch ( quantitationType.getScale() ) {
            case LOG2:
                log.warn( String.format( "Data was detected on a log2-scale, but the quantitation type indicate %s. No transformation is necessary.",
                        quantitationType.getScale() ) );
                break;
            case LN:
                ExpressionDataDoubleMatrixUtil.log.info( " **** Converting from ln to log2 **** " );
                MatrixStats.convertToLog2( transformedMatrix, Math.E );
                break;
            case LOG10:
                ExpressionDataDoubleMatrixUtil.log.info( " **** Converting from log10 to log2 **** " );
                MatrixStats.convertToLog2( transformedMatrix, 10 );
                break;
            case LINEAR:
                ExpressionDataDoubleMatrixUtil.log.info( " **** LOG TRANSFORMING **** " );
                MatrixStats.logTransform( transformedMatrix );
                break;
            case COUNT:
                /*
                 * Since we store log2cpm this shouldn't be reached any more. We don't do it in place.
                 */
                ExpressionDataDoubleMatrixUtil.log.info( " **** Converting from count to log2 counts per million **** " );
                DoubleMatrix1D librarySize = MatrixStats.colSums( transformedMatrix );
                transformedMatrix = MatrixStats.convertToLog2Cpm( transformedMatrix, librarySize );
                // as we convert counts to log2cpm
                type = StandardQuantitationType.AMOUNT;
                break;
            default:
                throw new UnsupportedQuantitationScaleConversionException( quantitationType.getScale(), ScaleType.LOG2 );
        }

        StandardQuantitationType finalType = type;
        List<QuantitationType> log2Qts = dmatrix.getQuantitationTypes().stream()
                .map( QuantitationType.Factory::newInstance )
                .peek( qt -> {
                    qt.setType( finalType );
                    qt.setScale( ScaleType.LOG2 );
                } )
                .collect( Collectors.toList() );

        ExpressionDataDoubleMatrix log2Matrix = new ExpressionDataDoubleMatrix( dmatrix, transformedMatrix, log2Qts );

        try {
            detectSuspiciousValues( log2Matrix, log2Qts.iterator().next() );
        } catch ( SuspiciousValuesForQuantitationException e ) {
            if ( ignoreQuantitationMismatch ) {
                log.warn( String.format( "Expression data matrix contains suspicious values:\n\n - %s",
                        e.getSuspiciousValues().stream()
                                .map( SuspiciousValuesForQuantitationException.SuspiciousValueResult::toString )
                                .collect( Collectors.joining( "\n - " ) ) ) );
            } else {
                throw e;
            }
        }

        return log2Matrix;
    }

    public static ExpressionDataDoubleMatrix ensureLog2Scale( ExpressionDataDoubleMatrix expressionData ) {
        try {
            return ensureLog2Scale( expressionData, true );
        } catch ( QuantitationMismatchException e ) {
            // never happening
            throw new RuntimeException( e );
        }
    }

    /**
     * Check if an expression data matrix has heterogeneous quantitations.
     * <p>
     * This happens when data from multiple platforms are mixed together. If the data is transformed in the same way,
     * it's generally okay to mix them together.
     */
    private static boolean isHeterogeneous( ExpressionDataDoubleMatrix expressionData ) {
        QuantitationType firstQt = expressionData.getQuantitationTypes().iterator().next();
        if ( firstQt == null ) {
            throw new IllegalArgumentException( "At least one quantitation type is needed." );
        }
        for ( QuantitationType qt : expressionData.getQuantitationTypes() ) {
            if ( qt.getRepresentation() != firstQt.getRepresentation()
                    || qt.getGeneralType() != firstQt.getGeneralType()
                    || qt.getType() != firstQt.getType()
                    || qt.getScale() != firstQt.getScale()
                    || qt.getIsNormalized() != firstQt.getIsNormalized()
                    || qt.getIsBackground() != firstQt.getIsBackground()
                    || qt.getIsBackgroundSubtracted() != firstQt.getIsBackgroundSubtracted()
                    || qt.getIsBatchCorrected() != firstQt.getIsBatchCorrected() ) {
                return true;
            }
        }
        return false;
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
    public static QuantitationType inferQuantitationType( ExpressionDataDoubleMatrix expressionDataDoubleMatrix ) {
        QuantitationType qt = new QuantitationType();
        InferredQuantitationType iqt = infer( expressionDataDoubleMatrix );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( iqt.type );
        qt.setScale( iqt.scale );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsRatio( iqt.isRatio );
        return qt;
    }

    private static InferredQuantitationType infer( ExpressionDataDoubleMatrix expressionData ) {
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
            throw new IllegalArgumentException( "Cannot infer quantitation type without data." );
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

    /**
     * Subtract two matrices. Ideally, they matrices are conformant, but if they are not (as some rows are sometimes
     * missing for some quantitation types), this method attempts to handle it anyway (see below). The rows and columns
     * do not have to be in the same order, but they do have to have the same column keys and row keys (with the
     * exception of missing rows). The result is stored in a. (a - b).
     * If the number of rows are not the same, and/or the rows have different keys in the two matrices, some rows will
     * simply not get subtracted and a warning will be issued.
     *
     * @param a matrix a
     * @param b matrix b
     * @throws IllegalArgumentException if the matrices are not column-conformant.
     */
    public static void subtractMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        // checkConformant( a, b );
        if ( a.columns() != b.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + a.columns() + " != " + b.columns() );

        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            int rowNum = el.getIndex();
            CompositeSequence del = el.getDesignElement();

            if ( b.getRow( del ) == null ) {
                ExpressionDataDoubleMatrixUtil.log.warn( "Matrix 'b' is missing a row for " + del + ", it will not be subtracted" );
                continue;
            }

            for ( int i = 0; i < columns; i++ ) {
                BioAssay assay = a.getBioAssaysForColumn( i ).iterator().next();
                double valA = a.get( del, assay );
                double valB = b.get( del, assay );
                a.set( rowNum, i, valA - valB );
            }
        }
    }

    /**
     * Log-transform the values in the matrix (base 2). Non-positive values (which have no logarithm defined) are
     * entered as NaN.
     *
     * @param matrix matrix
     */
    public static void logTransformMatrix( ExpressionDataDoubleMatrix matrix ) {
        int columns = matrix.columns();
        double log2 = Math.log( ExpressionDataDoubleMatrixUtil.LOGARITHM_BASE );
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                double valA = matrix.get( del, bm );
                if ( valA <= 0 ) {
                    matrix.set( del, bm, Double.NaN );
                } else {
                    matrix.set( del, bm, Math.log( valA ) / log2 );
                }
            }
        }

    }

    /**
     * Add two matrices. Ideally, they matrices are conformant, but if they are not (as some rows are sometimes missing
     * for some quantitation types), this method attempts to handle it anyway (see below). The rows and columns do not
     * have to be in the same order, but they do have to have the same column keys and row keys (with the exception of
     * missing rows). The result is stored in a.
     * If the number of rows are not the same, and/or the rows have different keys in the two matrices, some rows will
     * simply not get added and a warning will be issued.
     *
     * @param a matrix a
     * @param b matrix b
     * @throws IllegalArgumentException if the matrices are not column-conformant.
     */
    public static void addMatrices( ExpressionDataDoubleMatrix a, ExpressionDataDoubleMatrix b ) {
        // checkConformant( a, b );
        if ( a.columns() != b.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + a.columns() + " != " + b.columns() );
        int columns = a.columns();
        for ( ExpressionDataMatrixRowElement el : a.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();

            if ( b.getRow( del ) == null ) {
                ExpressionDataDoubleMatrixUtil.log.warn( "Matrix 'b' is missing a row for " + del + ", this row will not be added" );
                continue;
            }
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = a.getBioAssaysForColumn( i ).iterator().next();
                double valA = a.get( del, bm );
                double valB = b.get( del, bm );
                a.set( del, bm, valA + valB );
            }
        }
    }

    /**
     * Divide all values by the dividend
     *
     * @param matrix matrix
     * @param dividend dividend
     * @throws IllegalArgumentException if dividend == 0.
     */
    public static void scalarDivideMatrix( ExpressionDataDoubleMatrix matrix, double dividend ) {
        if ( dividend == 0 ) throw new IllegalArgumentException( "Can't divide by zero" );
        int columns = matrix.columns();
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                double valA = matrix.get( del, bm );
                matrix.set( del, bm, valA / dividend );

            }
        }
    }

    /**
     * Use the mask matrix to turn some values in a matrix to NaN. Ideally, they matrices are conformant, but if they
     * are not (as some rows are sometimes missing for some quantitation types), this method attempts to handle it
     * anyway (see below). The rows and columns do not have to be in the same order, but they do have to have the same
     * column keys and row keys (with the exception of missing rows). The result is stored in matrix.
     *
     * @param matrix matrix
     * @param mask if null, masking is not attempted.
     */
    public static void maskMatrix( ExpressionDataDoubleMatrix matrix, ExpressionDataBooleanMatrix mask ) {
        if ( mask == null ) return;
        // checkConformant( a, b );
        if ( matrix.columns() != mask.columns() )
            throw new IllegalArgumentException( "Unequal column counts: " + matrix.columns() + " != " + mask.columns() );
        int columns = matrix.columns();
        for ( ExpressionDataMatrixRowElement el : matrix.getRowElements() ) {
            CompositeSequence del = el.getDesignElement();
            if ( mask.getRow( del ) == null ) {
                ExpressionDataDoubleMatrixUtil.log.warn( "Mask Matrix is missing a row for " + del );
                continue;
            }
            for ( int i = 0; i < columns; i++ ) {
                BioAssay bm = matrix.getBioAssaysForColumn( i ).iterator().next();
                boolean present = mask.get( del, bm );
                if ( !present ) {
                    matrix.set( del, bm, Double.NaN );
                }

            }
        }
    }

}
