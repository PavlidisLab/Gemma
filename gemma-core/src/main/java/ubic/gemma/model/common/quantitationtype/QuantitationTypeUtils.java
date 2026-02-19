package ubic.gemma.model.common.quantitationtype;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.util.Assert;
import ubic.gemma.persistence.util.ByteArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utilities for working with {@link QuantitationType}s.
 *
 * @author poirigui
 */
public class QuantitationTypeUtils {

    /**
     * Check if a given quantitation type holds log2 CPM.
     */
    public static boolean isLog2cpm( QuantitationType qt ) {
        return Strings.CI.contains( qt.getName(), "log2cpm" ) &&
                qt.getGeneralType() == GeneralType.QUANTITATIVE &&
                qt.getType() == StandardQuantitationType.AMOUNT &&
                qt.getScale() == ScaleType.LOG2;
    }

    /**
     * Check if a given QT holds log-transformed data.
     */
    public static boolean isLogTransformed( QuantitationType qt ) {
        return qt.getScale() == ScaleType.LOG2 || qt.getScale() == ScaleType.LN || qt.getScale() == ScaleType.LOG10
                || qt.getScale() == ScaleType.LOG1P || qt.getScale() == ScaleType.LOGBASEUNKNOWN;
    }

    /**
     * Check if a given quantitation type contains counting data.
     * <p>
     * Note that counting data might not necessarily use the {@link ScaleType#COUNT} scale.
     */
    public static boolean isCount( QuantitationType qt ) {
        return qt.getGeneralType() == GeneralType.QUANTITATIVE && qt.getType() == StandardQuantitationType.COUNT;
    }

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

    @Nonnull
    public static Number getDefaultValueAsNumber( QuantitationType quantitationType ) {
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
            case INT:
                return 0;
            case LONG:
                return 0L;
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
     * Obtain the default value for a vector as an encoded {@code byte[]}.
     */
    public static byte[] getDefaultValueAsBytes( QuantitationType quantitationType ) {
        PrimitiveType pt = quantitationType.getRepresentation();
        switch ( pt ) {
            case DOUBLE:
                double d;
                if ( quantitationType.getType() == StandardQuantitationType.COUNT ) {
                    d = getDefaultCountValueAsDouble( quantitationType );
                } else {
                    d = Double.NaN;
                }
                return ByteArrayUtils.doubleArrayToBytes( new double[] { d } );
            case FLOAT:
                float f;
                if ( quantitationType.getType() == StandardQuantitationType.COUNT ) {
                    f = getDefaultCountValueAsFloat( quantitationType );
                } else {
                    f = Float.NaN;
                }
                return ByteArrayUtils.floatArrayToBytes( new float[] { f } );
            case STRING:
            case BOOLEAN:
                return new byte[] { 0 };
            case CHAR:
                return new byte[] { 0, 0 };
            case INT:
                return new byte[] { 0, 0, 0, 0 };
            case LONG:
                return new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
            default:
                throw new UnsupportedOperationException( "Missing values in data vectors of type " + quantitationType + " is not supported." );
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
            description = Strings.CS.appendIfMissing( StringUtils.strip( qt.getDescription() ), "." ) + " ";
        } else {
            description = "";
        }
        description += s;
        qt.setDescription( description + s );
    }

    /**
     * @return a unit, or null if the QT is unitless (e.g. ratio)
     */
    @Nullable
    public static String getUnit( QuantitationType quantitationType ) {
        if ( quantitationType.getIsRatio() ) {
            // ratio are unitless
            return null;
        }
        if ( quantitationType.getName().contains( "log2cpm" ) && quantitationType.getScale() == ScaleType.LOG2 ) {
            return "log2cpm";
        }
        switch ( quantitationType.getScale() ) {
            case COUNT:
                return "count";
            case PERCENT:
                return "%";
            case LOG1P:
                return "log1p";
            case LOG10:
                return "log10";
            case LOG2:
                return "log2";
            case LN:
                return "ln";
            default:
                return "?";
        }
    }
}
