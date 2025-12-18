package ubic.gemma.core.analysis.preprocess.slice;

import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ArrayUtils;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.persistence.util.ByteArrayUtils;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Slice bulk data vectors.
 *
 * @author poirigui
 */
public class BulkDataSlicerUtils {

    /**
     * Slice a collection of bulk data vectors.
     *
     * @param vectorType the type of vector produced
     */
    public static <T extends BulkExpressionDataVector> Collection<T> slice( Collection<T> vectors, List<BioAssay> assays, Class<T> vectorType, boolean allowMissing ) {
        return vectors.stream().map( createSlicer( assays, vectorType, allowMissing ) ).collect( Collectors.toList() );
    }

    /**
     * Create a slicer function for bulk data vectors that can be applied on a {@link Stream}.
     */
    public static <T extends BulkExpressionDataVector> Function<T, T> createSlicer( List<BioAssay> assays, Class<T> vectorType, boolean allowMissing ) {
        Map<BioAssayDimension, BioAssayDimension> badCache = new HashMap<>();
        Map<BioAssayDimension, BioAssayMapping> bioAssayMappingCache = new HashMap<>();
        Map<QuantitationType, byte[]> missingValueCache = new HashMap<>();
        return bulkDataVector -> slice( bulkDataVector, assays, badCache, bioAssayMappingCache, vectorType, getDataVectorIgnoredProperties( vectorType ), allowMissing, missingValueCache );
    }

    /**
     * Slice a collection of bulk data vectors into double arrays.
     */
    public static <T extends BulkExpressionDataVector> List<double[]> sliceDoubles( List<T> vector, List<BioAssay> assays, boolean allowMissing ) {
        Map<BioAssayDimension, BioAssayMapping> bioAssayMappingCache = new HashMap<>();
        Map<QuantitationType, byte[]> missingValueCache = new HashMap<>();
        List<double[]> result = new ArrayList<>( vector.size() );
        for ( T v : vector ) {
            BioAssayMapping bioAssayMapping = bioAssayMappingCache.computeIfAbsent( v.getBioAssayDimension(), k -> createSampleMapping( v.getBioAssayDimension().getBioAssays(), assays, allowMissing ) );
            byte[] missingValue = missingValueCache.computeIfAbsent( v.getQuantitationType(), QuantitationTypeUtils::getDefaultValueAsBytes );
            result.add( ByteArrayUtils.byteArrayToDoubles( sliceData( v, bioAssayMapping, allowMissing, missingValue ) ) );
        }
        return result;
    }

    /**
     * Slice a collection of bulk data vectors into boolean arrays.
     */
    public static <T extends BulkExpressionDataVector> List<boolean[]> sliceBooleans( List<T> vector, List<BioAssay> assays, boolean allowMissing ) {
        Map<BioAssayDimension, BioAssayMapping> bioAssayMappingCache = new HashMap<>();
        Map<QuantitationType, byte[]> missingValueCache = new HashMap<>();
        List<boolean[]> result = new ArrayList<>( vector.size() );
        for ( T v : vector ) {
            BioAssayMapping bioAssayMapping = bioAssayMappingCache.computeIfAbsent( v.getBioAssayDimension(), k -> createSampleMapping( v.getBioAssayDimension().getBioAssays(), assays, allowMissing ) );
            byte[] missingValue = missingValueCache.computeIfAbsent( v.getQuantitationType(), QuantitationTypeUtils::getDefaultValueAsBytes );
            result.add( ByteArrayUtils.byteArrayToBooleans( sliceData( v, bioAssayMapping, allowMissing, missingValue ) ) );
        }
        return result;
    }

    private static String[] getDataVectorIgnoredProperties( Class<?> vectorType ) {
        List<String> ignoredPropertiesList = new ArrayList<>();
        for ( PropertyDescriptor pd : BeanUtils.getPropertyDescriptors( vectorType ) ) {
            if ( pd.getName().equals( "bioAssayDimension" ) || ( pd.getName().startsWith( "data" ) && !pd.getName().equals( "dataIndices" ) ) ) {
                ignoredPropertiesList.add( pd.getName() );
            }
        }
        return ignoredPropertiesList.toArray( new String[0] );
    }

    private static <T extends BulkExpressionDataVector> T slice( T vec, List<BioAssay> assays, Map<BioAssayDimension, BioAssayDimension> badCache, Map<BioAssayDimension, BioAssayMapping> bioAssayMappingCache, Class<T> vectorType, String[] ignoredProperties, boolean allowMissing, Map<QuantitationType, byte[]> missingValueCache ) {
        T newVec = BeanUtils.instantiate( vectorType );
        BeanUtils.copyProperties( vec, newVec, ignoredProperties );
        newVec.setBioAssayDimension( badCache.computeIfAbsent( vec.getBioAssayDimension(), bad -> sliceDimension( bad, assays, allowMissing ) ) );
        BioAssayMapping bioAssayMapping = bioAssayMappingCache.computeIfAbsent( vec.getBioAssayDimension(), k -> createSampleMapping( vec.getBioAssayDimension().getBioAssays(), assays, allowMissing ) );
        byte[] missingValue = missingValueCache.computeIfAbsent( vec.getQuantitationType(), QuantitationTypeUtils::getDefaultValueAsBytes );
        newVec.setData( sliceData( vec, bioAssayMapping, allowMissing, missingValue ) );
        return newVec;
    }

    private static BioAssayMapping createSampleMapping( List<BioAssay> fromAssays, List<BioAssay> toAssays, boolean allowMissing ) {
        Map<BioAssay, Integer> ba2index = ListUtils.indexOfElements( fromAssays );
        int[] result = new int[toAssays.size()];
        for ( int i = 0; i < result.length; i++ ) {
            BioAssay ba = toAssays.get( i );
            result[i] = allowMissing ? ba2index.getOrDefault( ba, -1 ) : requireNonNull( ba2index.get( ba ), () -> ba + " was not found in source assays." );
        }
        return new BioAssayMapping( result, ArrayUtils.isContiguous( result ) );
    }

    private static byte[] sliceData( BulkExpressionDataVector vec, BioAssayMapping bioAssayMapping, boolean allowMissing, byte[] missingValue ) {
        int sizeInBytes = vec.getQuantitationType().getRepresentation().getSizeInBytes();
        if ( sizeInBytes == -1 ) {
            throw new UnsupportedOperationException( "Cannot slice data of unknown size." );
        }
        byte[] data = new byte[bioAssayMapping.indices.length * sizeInBytes];
        if ( bioAssayMapping.indices.length > 0 && bioAssayMapping.isContiguous && !allowMissing ) {
            System.arraycopy( vec.getData(), bioAssayMapping.indices[0] * sizeInBytes, data, 0, bioAssayMapping.indices.length * sizeInBytes );
        } else {
            for ( int i = 0; i < bioAssayMapping.indices.length; i++ ) {
                if ( bioAssayMapping.indices[i] >= 0 ) {
                    System.arraycopy( vec.getData(), bioAssayMapping.indices[i] * sizeInBytes, data, i * sizeInBytes, sizeInBytes );
                } else {
                    System.arraycopy( missingValue, 0, data, i * sizeInBytes, sizeInBytes ); // fill with zeros
                }
            }
        }
        return data;
    }

    private static BioAssayDimension sliceDimension( BioAssayDimension bioAssayDimension, List<BioAssay> bioAssays, boolean allowMissing ) {
        Assert.isTrue( allowMissing || new HashSet<>( bioAssayDimension.getBioAssays() ).containsAll( bioAssays ), "All the requested assays must be in " + bioAssayDimension + "." );
        Assert.isTrue( new HashSet<>( bioAssays ).size() == bioAssays.size(), "Requested assays must be unique." );
        return BioAssayDimension.Factory.newInstance( bioAssays );
    }

    @AllArgsConstructor
    private static class BioAssayMapping {
        private final int[] indices;
        private final boolean isContiguous;
    }
}
