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
package ubic.gemma.core.analysis.preprocess.convert;

import cern.colt.matrix.DoubleMatrix1D;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.detect.InferredQuantitationMismatchException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils;
import ubic.gemma.core.analysis.preprocess.detect.SuspiciousValuesForQuantitationException;
import ubic.gemma.core.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.detectSuspiciousValues;
import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.infer;


/**
 * Perform various computations on ExpressionDataMatrices (usually in-place).
 *
 * @author pavlidis
 */
public class QuantitationTypeConversionUtils {

    /**
     * This threshold is used to determine if a row has too many identical value; a value of N means that the number of distinct values in the
     * expression vector of length M must be at least N * M.
     */
    private static final double MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT = 0.3;

    /**
     * We don't apply the "unique values" filter to matrices with fewer columns than this.
     */
    private static final int MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER = 4;


    private static final Log log = LogFactory.getLog( QuantitationTypeConversionUtils.class.getName() );

    /**
     * Log2 transform if necessary, do any required filtering prior to analysis. Count data is converted to log2CPM (but
     * we store log2cpm as the processed data, so that is what would generally be used).
     *
     * @param dmatrix matrix
     * @return ee data double matrix
     * @throws QuantitationTypeDetectionException if data cannot be converted to log2 scale
     */
    public static ExpressionDataDoubleMatrix filterAndLog2Transform( ExpressionDataDoubleMatrix dmatrix ) throws QuantitationTypeConversionException {
        dmatrix = QuantitationTypeConversionUtils.ensureLog2Scale( dmatrix );

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
            QuantitationTypeConversionUtils.log.info( ( r - dmatrix.rows() ) + " rows removed due to low variance" );
        }
        r = dmatrix.rows();

        if ( dmatrix.columns() > QuantitationTypeConversionUtils.MINIMUM_COLUMNS_TO_APPLY_UNIQUE_VALUES_FILTER ) {
            /* This threshold had been 10^-5, but it's probably too stringent. Also remember
             * the data are log transformed the threshold should be transformed as well (it's not that simple),
             * but that's a minor effect.
             * To somewhat counter the effect of lowering this stringency, increasing the stringency on VALUES_LIMIT may help */
            dmatrix = ExpressionExperimentFilter.tooFewDistinctValues( dmatrix, QuantitationTypeConversionUtils.MINIMUM_UNIQUE_VALUES_FRACTION_PER_ELEMENT, 0.001 );
            if ( dmatrix.rows() < r ) {
                QuantitationTypeConversionUtils.log.info( ( r - dmatrix.rows() ) + " rows removed due to too many identical values" );
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
    public static ExpressionDataDoubleMatrix ensureLog2Scale( ExpressionDataDoubleMatrix dmatrix, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
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
                QuantitationTypeConversionUtils.log.info( " **** Converting from ln to log2 **** " );
                MatrixStats.convertToLog2( transformedMatrix, Math.E );
                break;
            case LOG10:
                QuantitationTypeConversionUtils.log.info( " **** Converting from log10 to log2 **** " );
                MatrixStats.convertToLog2( transformedMatrix, 10 );
                break;
            case LOG1P:
                QuantitationTypeConversionUtils.log.info( " **** Converting from log1p to log2 **** " );
                for ( int i = 0; i < transformedMatrix.rows(); i++ ) {
                    for ( int j = 0; j < transformedMatrix.columns(); j++ ) {
                        transformedMatrix.set( i, j, Math.log( Math.expm1( transformedMatrix.get( i, j ) ) ) / Math.log( 2 ) );
                    }
                }
                break;
            case LINEAR:
                QuantitationTypeConversionUtils.log.info( " **** LOG TRANSFORMING **** " );
                MatrixStats.logTransform( transformedMatrix );
                break;
            case COUNT:
                /*
                 * Since we store log2cpm this shouldn't be reached any more. We don't do it in place.
                 */
                QuantitationTypeConversionUtils.log.info( " **** Converting from count to log2 counts per million **** " );
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

    public static ExpressionDataDoubleMatrix ensureLog2Scale( ExpressionDataDoubleMatrix expressionData ) throws QuantitationTypeConversionException {
        try {
            return ensureLog2Scale( expressionData, true );
        } catch ( QuantitationTypeDetectionException e ) {
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

    /**
     * Convert a collection of vectors.
     * @param createQtFunc a function to create a converted {@link QuantitationType}
     * @param doToVector   a consumer to post-process the created vector (first argument) given the original vector
     *                     (second argument)
     * @param vectorType   the type of vector to produce
     */
    public static <T extends DataVector> Collection<T> convertVectors( Collection<T> vectors, Function<QuantitationType, QuantitationType> createQtFunc, BiConsumer<T, T> doToVector, Class<T> vectorType ) {
        ArrayList<T> result = new ArrayList<>( vectors.size() );
        Map<QuantitationType, QuantitationType> convertedQts = new HashMap<>();
        String[] ignoredProperties = getDataVectorIgnoredProperties( vectorType );
        for ( T vector : vectors ) {
            QuantitationType convertedQt = convertedQts.computeIfAbsent( vector.getQuantitationType(), createQtFunc );
            result.add( createVector( vector, vectorType, convertedQt, doToVector, ignoredProperties ) );
        }
        return result;
    }


    /**
     * Convert a single vector.
     */
    public static <T extends DataVector> T convertVector( T vector, Function<QuantitationType, QuantitationType> createQtFunc, BiConsumer<T, T> doToVector, Class<T> vectorType ) {
        return createVector( vector, vectorType, createQtFunc.apply( vector.getQuantitationType() ), doToVector, getDataVectorIgnoredProperties( vectorType ) );
    }

    private static <T extends DataVector> T createVector( T vector, Class<T> vectorType, QuantitationType convertedQt, BiConsumer<T, T> doToVector, String[] ignoredProperties ) {
        T convertedVector = BeanUtils.instantiate( vectorType );
        BeanUtils.copyProperties( vector, convertedVector, ignoredProperties );
        convertedVector.setQuantitationType( convertedQt );
        doToVector.accept( convertedVector, vector );
        return convertedVector;
    }

    /**
     * List of properties to copy over when converting a vector to a different QT.
     */
    private static String[] getDataVectorIgnoredProperties( Class<? extends DataVector> vectorType ) {
        List<String> ignoredPropertiesList = new ArrayList<>();
        for ( PropertyDescriptor pd : BeanUtils.getPropertyDescriptors( vectorType ) ) {
            if ( pd.getName().equals( "quantitationType" ) || ( pd.getName().startsWith( "data" ) && !pd.getName().equals( "dataIndices" ) ) ) {
                ignoredPropertiesList.add( pd.getName() );
            }
        }
        return ignoredPropertiesList.toArray( new String[0] );
    }

    /**
     * Obtain the default to use for a given quantitation type if no value was provided.
     */
    @Nonnull
    public static Object getDefaultValue( QuantitationType quantitationType ) {
        PrimitiveType pt = quantitationType.getRepresentation();
        switch ( pt ) {
            case DOUBLE:
                return Double.NaN;
            case FLOAT:
                return Float.NaN;
            case STRING:
                return "";
            case CHAR:
                return ( char ) 0;
            case INT:
                return 0;
            case LONG:
                return 0L;
            case BOOLEAN:
                return false;
            default:
                throw new UnsupportedOperationException( "Missing values in data vectors of type " + quantitationType + " is not supported." );
        }
    }
}
