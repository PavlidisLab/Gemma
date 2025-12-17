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
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.BeanUtils;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.MatrixStats;
import ubic.gemma.core.analysis.preprocess.detect.InferredQuantitationMismatchException;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionException;
import ubic.gemma.core.analysis.preprocess.detect.SuspiciousValuesForQuantitationException;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.CheckReturnValue;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.detectSuspiciousValues;
import static ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils.lintQuantitationType;


/**
 * Perform various computations on ExpressionDataMatrices (usually in-place).
 *
 * @author pavlidis
 */
@CommonsLog
public class QuantitationTypeConversionUtils {

    public static ExpressionDataDoubleMatrix ensureLog2Scale( ExpressionDataDoubleMatrix expressionData ) throws QuantitationTypeConversionException {
        try {
            return ensureLog2Scale( expressionData, true );
        } catch ( QuantitationTypeDetectionException e ) {
            // never happening
            throw new RuntimeException( e );
        }
    }

    /**
     * Ensures that the given matrix is on a Log2 scale.
     * ! Does not update the QT !
     *
     * @param dmatrix                    the matrix to be transformed to a log2 scale if necessary.
     * @param ignoreQuantitationMismatch if true, ignore mismatch between matrix quantitation types and that inferred
     *                                   from data
     * @return a data matrix that is guaranteed to be on a log2 scale or the original input matrix if it was already the
     * case
     * @throws QuantitationTypeConversionException   if transformation to log2 scale is impossible
     * @throws InferredQuantitationMismatchException if the inferred scale type differs that inferred from data
     */
    @CheckReturnValue
    public static ExpressionDataDoubleMatrix ensureLog2Scale( ExpressionDataDoubleMatrix dmatrix, boolean ignoreQuantitationMismatch ) throws QuantitationTypeDetectionException, QuantitationTypeConversionException {
        QuantitationType quantitationType;
        if ( dmatrix.getQuantitationTypes().size() > 1 ) {
            quantitationType = QuantitationTypeUtils.mergeQuantitationTypes( dmatrix.getQuantitationTypes() );
        } else if ( !dmatrix.getQuantitationTypes().isEmpty() ) {
            quantitationType = dmatrix.getQuantitationTypes().iterator().next();
        } else {
            throw new IllegalArgumentException( "Expression data matrix lacks a quantitation type." );
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

        lintQuantitationType( quantitationType, dmatrix, ignoreQuantitationMismatch );

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
                /* FIXME: filter out rows from the count matrix that have all zero counts. Hree, this should already probably have been done */


                transformedMatrix = MatrixStats.convertToLog2Cpm( transformedMatrix, librarySize );
                // as we convert counts to log2cpm
                type = StandardQuantitationType.AMOUNT;
                break;
            default:
                throw new UnsupportedQuantitationScaleConversionException( quantitationType.getScale(), ScaleType.LOG2 );
        }

        StandardQuantitationType finalType = type;
        Map<QuantitationType, QuantitationType> log2Qts = dmatrix.getQuantitationTypes().stream()
                .collect( Collectors.toMap( qt -> qt, qt -> {
                    QuantitationType log2Qt = QuantitationType.Factory.newInstance( qt );
                    log2Qt.setType( finalType );
                    log2Qt.setScale( ScaleType.LOG2 );
                    QuantitationTypeUtils.appendToDescription( log2Qt, "Data was converted from " + quantitationType.getScale() + " to " + ScaleType.LOG2 + "." );
                    return log2Qt;
                } ) );

        ExpressionDataDoubleMatrix log2Matrix = dmatrix.withMatrix( transformedMatrix, log2Qts );

        try {
            detectSuspiciousValues( log2Matrix, log2Qts.values().iterator().next() );
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

    /**
     * Convert a collection of vectors.
     *
     * @param createQtFunc a function to create a converted {@link QuantitationType}
     * @param doToVector   a consumer to post-process the created vector (first argument) given the original vector
     *                     (second argument)
     * @param vectorType   the type of vector to produce
     */
    static <T extends DataVector> Collection<T> convertVectors( Collection<T> vectors, Function<QuantitationType, QuantitationType> createQtFunc, DoToVectorFunction<T> doToVector, Class<T> vectorType ) throws QuantitationTypeConversionException {
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
    static <T extends DataVector> T convertVector( T vector, Function<QuantitationType, QuantitationType> createQtFunc, DoToVectorFunction<T> doToVector, Class<T> vectorType ) throws QuantitationTypeConversionException {
        return createVector( vector, vectorType, createQtFunc.apply( vector.getQuantitationType() ), doToVector, getDataVectorIgnoredProperties( vectorType ) );
    }

    private static <T extends DataVector> T createVector( T vector, Class<T> vectorType, QuantitationType convertedQt, DoToVectorFunction<T> doToVector, String[] ignoredProperties ) throws QuantitationTypeConversionException {
        T convertedVector = BeanUtils.instantiate( vectorType );
        BeanUtils.copyProperties( vector, convertedVector, ignoredProperties );
        convertedVector.setQuantitationType( convertedQt );
        doToVector.doToVector( convertedVector, vector );
        return convertedVector;
    }

    interface DoToVectorFunction<T> {

        /**
         *
         * @param convertedVector the new vector being converted with the converted {@link QuantitationType}
         * @param vector          the original vector
         * @throws QuantitationTypeConversionException
         */
        void doToVector( T convertedVector, T vector ) throws QuantitationTypeConversionException;
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

}
