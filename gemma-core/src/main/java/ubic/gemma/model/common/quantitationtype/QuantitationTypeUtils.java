package ubic.gemma.model.common.quantitationtype;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QuantitationTypeUtils {

    /**
     * Obtain the default to use for a given quantitation type.
     * <p>
     * This can be used as a placeholder for missing values in data vectors.
     * <p>
     * Missing double and float values are represented as NaNs unless they are counts in which case they are
     * represented as zeroes.
     * <p>
     * For counting data represented as double or float, the default value is transformed as per
     * {@link #getDefaultCountValueAsDouble(QuantitationType)} and {@link #getDefaultCountValueAsFloat(QuantitationType)}.
     */
    @Nonnull
    public static Object getDefaultValue( QuantitationType quantitationType ) {
        PrimitiveType pt = quantitationType.getRepresentation();
        switch ( pt ) {
            case DOUBLE:
                if ( quantitationType.getType() == StandardQuantitationType.COUNT ) {
                    return getDefaultCountValueAsDouble( quantitationType );
                }
                return Double.NaN;
            case FLOAT:
                if ( quantitationType.getType() == StandardQuantitationType.COUNT ) {
                    return getDefaultCountValueAsFloat( quantitationType );
                }
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

    public static double getDefaultValueAsDouble( QuantitationType qt ) {
        Assert.isTrue( qt.getRepresentation() == PrimitiveType.DOUBLE,
                "Only double representation is supported." );
        if ( qt.getType() == StandardQuantitationType.COUNT ) {
            return getDefaultCountValueAsDouble( qt );
        } else {
            return 0;
        }
    }

    /**
     * Obtain the value that indicates a missing value for counting data.
     * <p>
     * The default count is zero, but if the data is on a log-scale, it will be mapped to {@link Double#NEGATIVE_INFINITY}.
     */
    public static double getDefaultCountValueAsDouble( QuantitationType quantitationType ) {
        Assert.isTrue( quantitationType.getType() == StandardQuantitationType.COUNT,
                "Only counting data can be supplied." );
        Assert.isTrue( quantitationType.getRepresentation() == PrimitiveType.DOUBLE,
                "Only double representation is supported." );
        switch ( quantitationType.getScale() ) {
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return Double.NEGATIVE_INFINITY;
            case LOG1P: // in log1p, 0 is mapped back to 0
            default:
                return 0;
        }
    }

    /**
     * Obtain the value that indicates a missing value for counting data.
     */
    public static float getDefaultCountValueAsFloat( QuantitationType quantitationType ) {
        Assert.isTrue( quantitationType.getType() == StandardQuantitationType.COUNT,
                "Only counting data can be supplied." );
        Assert.isTrue( quantitationType.getRepresentation() == PrimitiveType.FLOAT,
                "Only float representation is supported." );
        switch ( quantitationType.getScale() ) {
            case LOG2:
            case LN:
            case LOG10:
            case LOGBASEUNKNOWN:
                return Float.NEGATIVE_INFINITY;
            case LOG1P: // in log1p, 0 is mapped back to 0
            default:
                return 0;
        }
    }

    /**
     * Merge a given collection of quantitation types.
     * <p>
     * All QTs must share the same general type, type, scale, representation and ratio status.
     * <p>
     * The {@link QuantitationType#getIsRecomputedFromRawData()} status is kept if all QTs are recomputed from raw data,
     * <p>
     * It is not possible to merge QTs that were either normalized or batch-corrected.
     *
     * @throws IllegalArgumentException if the QTs are incompatible or if less than two QTs are provided.
     */
    public static QuantitationType mergeQuantitationTypes( Collection<QuantitationType> quantitationTypes ) {
        Assert.isTrue( quantitationTypes.size() > 1, "Two or more quantitation types are needed for merging." );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( "Merged from " + quantitationTypes.size() + " quantitation types" );
        qt.setDescription( "Data was merged from the following quantitation types:\n" + quantitationTypes.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n" ) ) );
        qt.setGeneralType( getUniqueQuantitationTypeField( quantitationTypes, QuantitationType::getGeneralType ) );
        qt.setType( getUniqueQuantitationTypeField( quantitationTypes, QuantitationType::getType ) );
        qt.setScale( getUniqueQuantitationTypeField( quantitationTypes, QuantitationType::getScale ) );
        qt.setRepresentation( getUniqueQuantitationTypeField( quantitationTypes, QuantitationType::getRepresentation ) );
        qt.setIsRatio( getUniqueQuantitationTypeField( quantitationTypes, QuantitationType::getIsRatio ) );
        qt.setIsRecomputedFromRawData( quantitationTypes.stream().allMatch( QuantitationType::getIsRecomputedFromRawData ) );
        if ( quantitationTypes.stream().anyMatch( QuantitationType::getIsNormalized ) ) {
            throw new IllegalArgumentException( "One more quantitation types were normalized, cannot merge them. Use getQuantitationTypes() instead." );
        }
        if ( quantitationTypes.stream().anyMatch( QuantitationType::getIsBatchCorrected ) ) {
            throw new IllegalArgumentException( "One more quantitation types were batch-corrected, cannot merge them. Use getQuantitationTypes() instead." );
        }
        // TODO: background, backgroundSubtracted?
        return qt;
    }

    private static <S> S getUniqueQuantitationTypeField( Collection<QuantitationType> quantitationTypes, Function<QuantitationType, S> a ) {
        Set<S> uv = quantitationTypes.stream()
                .map( a )
                .collect( Collectors.toSet() );
        if ( uv.size() > 1 ) {
            throw new IllegalStateException( "There is more than one quantitation type in this matrix, use getQuantitationTypes() instead." );
        } else {
            return uv.iterator().next();
        }
    }

    /**
     * Append a suffix to a {@link QuantitationType} description.
     * <p>
     * This is used to describe the conversion that was performed.
     */
    public static void appendToDescription( QuantitationType qt, String s ) {
        String description;
        if ( StringUtils.isNotBlank( qt.getDescription() ) ) {
            description = StringUtils.appendIfMissing( StringUtils.strip( qt.getDescription() ), "." ) + " ";
        } else {
            description = "";
        }
        description += s;
        qt.setDescription( description + s );
    }
}
