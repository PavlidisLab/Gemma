package ubic.gemma.core.analysis.singleCell.aggregate;

import ubic.gemma.core.analysis.singleCell.SingleCellDescriptive;
import ubic.gemma.core.datastructure.matrix.BulkExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utilities for aggregating single-cell data vectors.
 * <p>
 * TODO: reuse these in {@link SingleCellExpressionExperimentAggregatorServiceImpl}
 * @author poirigui
 */
public class SingleCellDataVectorAggregatorUtils {

    /**
     * Create an aggregator that can be used on a {@link java.util.stream.Stream} of vectors.
     * <p>
     * Vectors from different experiments, QTs or dimension can be mixed.
     */
    public static Function<SingleCellExpressionDataVector, RawExpressionDataVector> createAggregator( SingleCellAggregationMethod method, @Nullable CellLevelCharacteristics cellLevelCharacteristics ) {
        Function<SingleCellExpressionDataVector, ?> func = createMethod( method, cellLevelCharacteristics );
        return new Function<SingleCellExpressionDataVector, RawExpressionDataVector>() {

            private final Map<QuantitationType, QuantitationType> qt2qt = new HashMap<>();
            private final Map<SingleCellDimension, BioAssayDimension> scd2bad = new HashMap<>();

            @Override
            public RawExpressionDataVector apply( SingleCellExpressionDataVector singleCellExpressionDataVector ) {
                QuantitationType qt = qt2qt.computeIfAbsent( singleCellExpressionDataVector.getQuantitationType(), k -> createQt( k, method ) );
                BioAssayDimension bad = scd2bad.computeIfAbsent( singleCellExpressionDataVector.getSingleCellDimension(), k -> createBad( k, cellLevelCharacteristics ) );
                if ( method == SingleCellAggregationMethod.COUNT || method == SingleCellAggregationMethod.COUNT_FAST ) {
                    //noinspection unchecked
                    return aggregateToIntByDescriptive( singleCellExpressionDataVector, qt, bad, ( Function<SingleCellExpressionDataVector, int[]> ) func );
                } else {
                    //noinspection unchecked
                    return aggregateToDoubleByDescriptive( singleCellExpressionDataVector, qt, bad, ( Function<SingleCellExpressionDataVector, double[]> ) func );
                }
            }
        };
    }

    /**
     * Aggregate a collection of vectors.
     * <p>
     * Vectors from different experiments, QTs or dimension can be mixed.
     */
    public static Collection<RawExpressionDataVector> aggregate( Collection<SingleCellExpressionDataVector> vectors, SingleCellAggregationMethod method, @Nullable CellLevelCharacteristics cellLevelCharacteristics ) {
        if ( vectors.isEmpty() ) {
            return Collections.emptySet();
        }
        Function<SingleCellExpressionDataVector, ?> func = createMethod( method, cellLevelCharacteristics );
        Map<QuantitationType, QuantitationType> qt2qt = new HashMap<>();
        Map<SingleCellDimension, BioAssayDimension> scd2bad = new HashMap<>();
        ArrayList<RawExpressionDataVector> result = new ArrayList<>( vectors.size() );
        for ( SingleCellExpressionDataVector vec : vectors ) {
            QuantitationType qt = qt2qt.computeIfAbsent( vec.getQuantitationType(), k -> createQt( k, method ) );
            BioAssayDimension bad = scd2bad.computeIfAbsent( vec.getSingleCellDimension(), k -> createBad( k, cellLevelCharacteristics ) );
            if ( method == SingleCellAggregationMethod.COUNT || method == SingleCellAggregationMethod.COUNT_FAST ) {
                //noinspection unchecked
                result.add( aggregateToIntByDescriptive( vec, qt, bad, ( Function<SingleCellExpressionDataVector, int[]> ) func ) );
            } else {
                //noinspection unchecked
                result.add( aggregateToDoubleByDescriptive( vec, qt, bad, ( Function<SingleCellExpressionDataVector, double[]> ) func ) );
            }
        }
        return result;
    }

    /**
     * Aggregate a single-cell data matrix.
     */
    public static <T> BulkExpressionDataMatrix<T> aggregate( SingleCellExpressionDataMatrix<T> scMatrix, SingleCellAggregationMethod method, Class<T> scalarType ) {
        throw new UnsupportedOperationException( "Aggregating single-cell data matrices is not supported yet." );
    }

    private static BioAssayDimension createBad( SingleCellDimension scDim, @Nullable CellLevelCharacteristics cellLevelCharacteristics ) {
        if ( cellLevelCharacteristics != null ) {
            List<BioAssay> assays = new ArrayList<>( scDim.getBioAssays().size() * cellLevelCharacteristics.getNumberOfCharacteristics() );
            for ( BioAssay ba : scDim.getBioAssays() ) {
                for ( Characteristic c : cellLevelCharacteristics.getCharacteristics() ) {
                    BioMaterial sample = BioMaterial.Factory.newInstance( ba.getSampleUsed().getName() + " - " + c.getValue(), ba.getSampleUsed().getSourceTaxon() );
                    sample.setSourceBioMaterial( ba.getSampleUsed() );
                    assays.add( BioAssay.Factory.newInstance( ba.getName() + " - " + c.getValue(), ba.getArrayDesignUsed(), sample ) );
                }
            }
            return BioAssayDimension.Factory.newInstance( assays );
        }
        return BioAssayDimension.Factory.newInstance( new ArrayList<>( scDim.getBioAssays() ) );
    }

    private static QuantitationType createQt( QuantitationType scQt, SingleCellAggregationMethod method ) {
        QuantitationType qt = QuantitationType.Factory.newInstance( scQt );
        switch ( method ) {
            case COUNT:
            case COUNT_FAST:
                qt.setType( StandardQuantitationType.COUNT );
                qt.setScale( ScaleType.COUNT );
                qt.setRepresentation( PrimitiveType.INT );
                break;
            case VARIANCE:
                // does not preserve scale due to X^2
                qt.setType( StandardQuantitationType.AMOUNT );
                qt.setScale( ScaleType.OTHER );
                qt.setRepresentation( PrimitiveType.DOUBLE );
                break;
            case MEAN:
            case STANDARD_DEVIATION:
                // scale is preserved
                qt.setType( StandardQuantitationType.AMOUNT );
                qt.setRepresentation( PrimitiveType.DOUBLE );
                break;
            case MEDIAN:
            case SUM:
            case MAX:
            case MIN:
                // these operation are scale-preserving
                qt.setRepresentation( PrimitiveType.DOUBLE );
                break;
            default:
                throw new UnsupportedOperationException( "Aggregating " + method + " is not supported for " + scQt );
        }
        return qt;
    }

    private static Function<SingleCellExpressionDataVector, ?> createMethod( SingleCellAggregationMethod method, @Nullable CellLevelCharacteristics cellLevelCharacteristics ) {
        if ( method == SingleCellAggregationMethod.COUNT || method == SingleCellAggregationMethod.COUNT_FAST ) {
            return createIntMethod( method, cellLevelCharacteristics );
        } else {
            return createDoubleMethod( method, cellLevelCharacteristics );
        }
    }

    private static Function<SingleCellExpressionDataVector, int[]> createIntMethod( SingleCellAggregationMethod method, @Nullable CellLevelCharacteristics cellLevelCharacteristics ) {
        if ( cellLevelCharacteristics != null ) {
            BiFunction<SingleCellExpressionDataVector, CellLevelCharacteristics, int[][]> m = createIntMethodWithClc( method );
            return vec -> Arrays.stream( m.apply( vec, cellLevelCharacteristics ) ).flatMapToInt( Arrays::stream ).toArray();
        }
        switch ( method ) {
            case COUNT:
                return SingleCellDescriptive::count;
            case COUNT_FAST:
                return SingleCellDescriptive::countFast;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static Function<SingleCellExpressionDataVector, double[]> createDoubleMethod( SingleCellAggregationMethod method, @Nullable CellLevelCharacteristics cellLevelCharacteristics ) {
        if ( cellLevelCharacteristics != null ) {
            BiFunction<SingleCellExpressionDataVector, CellLevelCharacteristics, double[][]> m = createDoubleMethodWithClc( method );
            return vec -> Arrays.stream( m.apply( vec, cellLevelCharacteristics ) ).flatMapToDouble( Arrays::stream ).toArray();
        }
        switch ( method ) {
            case MAX:
                return SingleCellDescriptive::max;
            case MIN:
                return SingleCellDescriptive::min;
            case MEAN:
                return SingleCellDescriptive::mean;
            case MEDIAN:
                return SingleCellDescriptive::median;
            case VARIANCE:
                return SingleCellDescriptive::sampleVariance;
            case STANDARD_DEVIATION:
                return SingleCellDescriptive::sampleStandardDeviation;
            case SUM:
                return SingleCellDescriptive::sum;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static BiFunction<SingleCellExpressionDataVector, CellLevelCharacteristics, int[][]> createIntMethodWithClc( SingleCellAggregationMethod method ) {
        switch ( method ) {
            case COUNT:
            case COUNT_FAST:
                throw new UnsupportedOperationException( "Cannot aggregate by cell-level characteristics with " + method );
            default:
                throw new IllegalArgumentException();
        }
    }

    private static BiFunction<SingleCellExpressionDataVector, CellLevelCharacteristics, double[][]> createDoubleMethodWithClc( SingleCellAggregationMethod method ) {
        switch ( method ) {
            case MAX:
                return SingleCellDescriptive::max;
            case MIN:
                return SingleCellDescriptive::min;
            case MEAN:
            case MEDIAN:
            case VARIANCE:
            case STANDARD_DEVIATION:
                throw new UnsupportedOperationException( "Cannot aggregate by cell-level characteristics with " + method );
            case SUM:
                return SingleCellDescriptive::sum;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static RawExpressionDataVector aggregateToDoubleByDescriptive( SingleCellExpressionDataVector vec, QuantitationType qt, BioAssayDimension bad, Function<SingleCellExpressionDataVector, double[]> func ) {
        RawExpressionDataVector newVec = RawExpressionDataVector.Factory.newInstance();
        newVec.setExpressionExperiment( vec.getExpressionExperiment() );
        newVec.setDesignElement( vec.getDesignElement() );
        newVec.setQuantitationType( qt );
        newVec.setBioAssayDimension( bad );
        newVec.setDataAsDoubles( func.apply( vec ) );
        return newVec;
    }

    private static RawExpressionDataVector aggregateToIntByDescriptive( SingleCellExpressionDataVector vec, QuantitationType qt, BioAssayDimension bad, Function<SingleCellExpressionDataVector, int[]> func ) {
        RawExpressionDataVector newVec = RawExpressionDataVector.Factory.newInstance();
        newVec.setExpressionExperiment( vec.getExpressionExperiment() );
        newVec.setDesignElement( vec.getDesignElement() );
        newVec.setQuantitationType( qt );
        newVec.setBioAssayDimension( bad );
        newVec.setDataAsInts( func.apply( vec ) );
        return newVec;
    }

    public enum SingleCellAggregationMethod {
        /**
         * Simply add up the unscaled values.
         */
        SUM,
        /**
         * @see SingleCellDescriptive#mean(SingleCellExpressionDataVector)
         */
        MEAN,
        /**
         * @see SingleCellDescriptive#median(SingleCellExpressionDataVector)
         */
        MEDIAN,
        /**
         * @see SingleCellDescriptive#sampleVariance(SingleCellExpressionDataVector)
         */
        VARIANCE,
        /**
         * @see SingleCellDescriptive#sampleStandardDeviation(SingleCellExpressionDataVector)
         */
        STANDARD_DEVIATION,
        /**
         * @see SingleCellDescriptive#max(SingleCellExpressionDataVector)
         */
        MAX,
        /**
         * @see SingleCellDescriptive#min(SingleCellExpressionDataVector)
         */
        MIN,
        /**
         * @see SingleCellDescriptive#count(SingleCellExpressionDataVector)
         */
        COUNT,
        /**
         * @see SingleCellDescriptive#countFast(SingleCellExpressionDataVector)
         */
        COUNT_FAST
    }
}
