package ubic.gemma.core.analysis.preprocess.convert;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;

import java.beans.PropertyDescriptor;
import java.util.*;

import static ubic.gemma.persistence.util.ByteArrayUtils.doubleArrayToBytes;

/**
 * Convert {@link ubic.gemma.model.expression.bioAssayData.DataVector} from different representations.
 * @author poirigui
 */
public class RepresentationConversionUtils {

    /**
     * Convert a collection of vectors to a desired representation.
     */
    public static <T extends DataVector> Collection<T> convertVectors( Collection<T> vectors, PrimitiveType toRepresentation, Class<T> vectorType ) {
        ArrayList<T> result = new ArrayList<>( vectors.size() );
        Map<QuantitationType, QuantitationType> convertedQts = new HashMap<>();
        List<String> ignoredPropertiesList = new ArrayList<>();
        for ( PropertyDescriptor pd : BeanUtils.getPropertyDescriptors( vectorType ) ) {
            if ( pd.getName().equals( "quantitationType" ) || ( pd.getName().startsWith( "data" ) && !pd.getName().equals( "dataIndices" ) ) ) {
                ignoredPropertiesList.add( pd.getName() );
            }
        }
        String[] ignoredProperties = ignoredPropertiesList.toArray( new String[0] );
        for ( T vector : vectors ) {
            QuantitationType qt = vector.getQuantitationType();
            QuantitationType convertedQt = convertedQts.computeIfAbsent( qt, qt2 -> {
                QuantitationType quantitationType = QuantitationType.Factory.newInstance( qt2 );
                String description;
                if ( StringUtils.isNotBlank( qt.getDescription() ) ) {
                    description = StringUtils.appendIfMissing( StringUtils.strip( qt.getDescription() ), "." ) + " ";
                } else {
                    description = "";
                }
                description += "Data was converted from " + qt.getRepresentation() + " to " + toRepresentation + ".";
                quantitationType.setDescription( description );
                quantitationType.setRepresentation( toRepresentation );
                return quantitationType;
            } );
            T convertedVector = BeanUtils.instantiate( vectorType );
            BeanUtils.copyProperties( vector, convertedVector, ignoredProperties );
            convertedVector.setQuantitationType( convertedQt );
            convertedVector.setData( convertData( vector, toRepresentation ) );
            result.add( convertedVector );
        }
        return result;
    }

    /**
     * Convert a single vector to a desired representation.
     */
    public static <T extends DataVector> T convertVector( T vector, PrimitiveType toRepresentation, Class<T> vectorType ) {
        QuantitationType qt = vector.getQuantitationType();
        QuantitationType convertedQt = QuantitationType.Factory.newInstance( qt );
        convertedQt.setRepresentation( toRepresentation );
        T convertedVector = BeanUtils.instantiate( vectorType );
        BeanUtils.copyProperties( vector, convertedVector );
        convertedVector.setQuantitationType( convertedQt );
        convertedVector.setData( convertData( vector, toRepresentation ) );
        return convertedVector;
    }

    private static byte[] convertData( DataVector vector, PrimitiveType to ) {
        PrimitiveType from = vector.getQuantitationType().getRepresentation();
        if ( from == to ) {
            return vector.getData();
        }
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case CHAR:
                return convertFromChar( vector.getDataAsChars(), to );
            case BOOLEAN:
                return convertFromBoolean( vector.getDataAsBooleans(), to );
            case INT:
                return convertFromInt( vector.getDataAsInts(), to );
            case LONG:
                return convertFromLong( vector.getDataAsLongs(), to );
            case FLOAT:
                return convertFromFloat( vector.getDataAsFloats(), to );
            case DOUBLE:
                return convertFromDouble( vector.getDataAsDoubles(), to );
            case STRING:
                return convertFromString( vector.getDataAsStrings(), to );
            default:
                throw unsupportedConversion( from, to );
        }
    }

    private static byte[] convertFromDouble( double[] dataAsDoubles, PrimitiveType to ) {
        throw unsupportedConversion( PrimitiveType.DOUBLE, to );
    }

    private static byte[] convertFromFloat( float[] dataAsFloats, PrimitiveType to ) {
        if ( to == PrimitiveType.DOUBLE ) {
            double[] result = new double[dataAsFloats.length];
            for ( int i = 0; i < dataAsFloats.length; i++ ) {
                result[i] = dataAsFloats[i];
            }
            return doubleArrayToBytes( result );
        }
        throw unsupportedConversion( PrimitiveType.FLOAT, to );
    }

    private static byte[] convertFromLong( long[] dataAsLongs, PrimitiveType to ) {
        if ( to == PrimitiveType.DOUBLE ) {
            double[] result = new double[dataAsLongs.length];
            for ( int i = 0; i < dataAsLongs.length; i++ ) {
                result[i] = dataAsLongs[i];
            }
            return doubleArrayToBytes( result );
        }
        throw unsupportedConversion( PrimitiveType.LONG, to );
    }

    private static byte[] convertFromInt( int[] dataAsInts, PrimitiveType to ) {
        if ( to == PrimitiveType.DOUBLE ) {
            double[] result = new double[dataAsInts.length];
            for ( int i = 0; i < dataAsInts.length; i++ ) {
                result[i] = dataAsInts[i];
            }
            return doubleArrayToBytes( result );
        }
        throw unsupportedConversion( PrimitiveType.INT, to );
    }

    private static byte[] convertFromBoolean( boolean[] dataAsBooleans, PrimitiveType to ) {
        throw unsupportedConversion( PrimitiveType.BOOLEAN, to );
    }

    private static byte[] convertFromChar( char[] dataAsChars, PrimitiveType to ) {
        throw unsupportedConversion( PrimitiveType.CHAR, to );
    }

    private static byte[] convertFromString( String[] dataAsStrings, PrimitiveType to ) {
        if ( to == PrimitiveType.DOUBLE ) {
            double[] resultAsDoubles = new double[dataAsStrings.length];
            for ( int i = 0; i < dataAsStrings.length; i++ ) {
                if ( dataAsStrings[i].isEmpty() ) {
                    resultAsDoubles[i] = Double.NaN;
                } else {
                    resultAsDoubles[i] = Double.parseDouble( dataAsStrings[i] );
                }
            }
            return doubleArrayToBytes( resultAsDoubles );
        }
        throw unsupportedConversion( PrimitiveType.STRING, to );
    }

    private static UnsupportedOperationException unsupportedConversion( PrimitiveType from, PrimitiveType to ) {
        throw new UnsupportedOperationException( "Converting data from " + from + " to " + to + " is not supported." );
    }
}
