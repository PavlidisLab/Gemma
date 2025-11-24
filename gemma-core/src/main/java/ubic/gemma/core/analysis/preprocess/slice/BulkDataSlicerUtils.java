package ubic.gemma.core.analysis.preprocess.slice;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static <T extends BulkExpressionDataVector> Collection<T> slice( Collection<T> vectors, List<BioAssay> assays, Class<T> vectorType ) {
        return vectors.stream()
                .map( createSlicer( assays, vectorType ) )
                .collect( Collectors.toList() );
    }

    /**
     * Create a slicer function for bulk data vectors that can be applied on a {@link Stream}.
     */
    public static <T extends BulkExpressionDataVector> Function<T, T> createSlicer( List<BioAssay> assays, Class<T> vectorType ) {
        Map<BioAssayDimension, BioAssayDimension> badCache = new HashMap<>();
        Map<BioAssayDimension, int[]> sampleIndicesCache = new HashMap<>();
        return bulkDataVector -> slice( bulkDataVector, assays, badCache, sampleIndicesCache, vectorType, getDataVectorIgnoredProperties( vectorType ) );
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

    private static <T extends BulkExpressionDataVector> T slice( T vec, List<BioAssay> assays, Map<BioAssayDimension, BioAssayDimension> badCache, Map<BioAssayDimension, int[]> sampleIndicesCache, Class<T> vectorType, String[] ignoredProperties ) {
        T newVec = BeanUtils.instantiate( vectorType );
        BeanUtils.copyProperties( vec, newVec, ignoredProperties );
        newVec.setBioAssayDimension( badCache.computeIfAbsent( vec.getBioAssayDimension(), bad -> sliceDimension( bad, assays ) ) );
        int[] sampleIndices = sampleIndicesCache.computeIfAbsent( vec.getBioAssayDimension(), k -> {
            Map<BioAssay, Integer> ba2index = ListUtils.indexOfElements( vec.getBioAssayDimension().getBioAssays() );
            int[] result = new int[assays.size()];
            for ( int i = 0; i < assays.size(); i++ ) {
                BioAssay ba = assays.get( i );
                result[i] = ba2index.get( ba );
            }
            return result;
        } );
        newVec.setData( sliceData( vec, sampleIndices ) );
        return newVec;
    }

    private static byte[] sliceData( BulkExpressionDataVector vec, int[] sampleIndices ) {
        int sizeInBytes = vec.getQuantitationType().getRepresentation().getSizeInBytes();
        if ( sizeInBytes == -1 ) {
            throw new UnsupportedOperationException( "Cannot slice data of unknown size." );
        }
        byte[] data = new byte[sampleIndices.length * sizeInBytes];
        for ( int i = 0; i < sampleIndices.length; i++ ) {
            System.arraycopy( vec.getData(), sampleIndices[i] * sizeInBytes, data, i * sizeInBytes, sizeInBytes );
        }
        return data;
    }

    private static BioAssayDimension sliceDimension( BioAssayDimension bioAssayDimension, List<BioAssay> bioAssays ) {
        Assert.isTrue( new HashSet<>( bioAssayDimension.getBioAssays() ).containsAll( bioAssays ),
                "All the requested assays must be in " + bioAssayDimension + "." );
        Assert.isTrue( new HashSet<>( bioAssays ).size() == bioAssays.size(),
                "Requested assays must be unique." );
        return BioAssayDimension.Factory.newInstance( bioAssays );
    }
}
