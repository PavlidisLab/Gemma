package ubic.gemma.core.analysis.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.util.UninitializedList;
import ubic.gemma.model.util.UninitializedSet;

import javax.annotation.Nullable;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utilities for slicing single-cell data.
 *
 * @author poirigui
 * @see ubic.gemma.core.analysis.preprocess.slice.BulkDataSlicerUtils
 */
@CommonsLog
public class SingleCellSlicerUtils {

    /**
     * List of ignored properties when copying properties from a {@link SingleCellExpressionDataVector} to another.
     */
    private static final String[] IGNORED_PROPERTIES;

    static {
        List<String> ignoredPropertiesList = new ArrayList<>();
        for ( PropertyDescriptor pd : BeanUtils.getPropertyDescriptors( SingleCellExpressionDataVector.class ) ) {
            if ( pd.getName().equals( "singleCellDimension" ) || ( pd.getName().startsWith( "data" ) && !pd.getName().equals( "dataIndices" ) ) ) {
                ignoredPropertiesList.add( pd.getName() );
            }
        }
        IGNORED_PROPERTIES = ignoredPropertiesList.toArray( new String[0] );
    }

    public static Collection<SingleCellExpressionDataVector> slice( Collection<SingleCellExpressionDataVector> vectors, List<BioAssay> bioAssays ) {
        return slice( vectors, bioAssays, new HashMap<>() );
    }

    public static Collection<SingleCellExpressionDataVector> slice( Collection<SingleCellExpressionDataVector> vectors, List<BioAssay> bioAssays, Map<SingleCellDimension, SingleCellDimension> singleCellDimensionCache ) {
        return vectors.stream()
                .map( createSlicer( bioAssays, singleCellDimensionCache ) )
                .collect( Collectors.toList() );
    }

    /**
     * Create a slicer for single-cell data vectors.
     */
    public static Function<SingleCellExpressionDataVector, SingleCellExpressionDataVector> createSlicer( List<BioAssay> assays ) {
        return createSlicer( assays, null, null, null, new HashMap<>() );
    }

    public static Function<SingleCellExpressionDataVector, SingleCellExpressionDataVector> createSlicer( List<BioAssay> assays, Map<SingleCellDimension, SingleCellDimension> singleCellDimensionCache ) {
        return createSlicer( assays, null, null, null, singleCellDimensionCache );
    }

    /**
     * Create a slicer for single-cell data vectors whose cell IDs, CTAs, and CLCs are already pre-sliced.
     * <p>
     * Unlike sparse vectors, these structures can be sliced in the database.
     *
     * @param cellIds pre-sliced cell IDs
     * @param ctas    pre-sliced CTAs
     * @param clcs    pre-sliced CLCs
     */
    public static Function<SingleCellExpressionDataVector, SingleCellExpressionDataVector> createSlicer( List<BioAssay> assays,
            @Nullable List<String> cellIds, @Nullable Set<CellTypeAssignment> ctas, @Nullable Set<CellLevelCharacteristics> clcs ) {
        return createSlicer( assays, cellIds, ctas, clcs, new HashMap<>() );
    }

    public static Function<SingleCellExpressionDataVector, SingleCellExpressionDataVector> createSlicer( List<BioAssay> assays,
            @Nullable List<String> cellIds, @Nullable Set<CellTypeAssignment> ctas, @Nullable Set<CellLevelCharacteristics> clcs,
            Map<SingleCellDimension, SingleCellDimension> scdCache ) {
        Map<SingleCellDimension, int[]> sampleIndicesCache = new HashMap<>();
        return vec -> sliceVector( vec, assays, cellIds, ctas, clcs, scdCache, sampleIndicesCache );
    }

    private static SingleCellExpressionDataVector sliceVector( SingleCellExpressionDataVector vec, List<BioAssay> assays,
            @Nullable List<String> cellIds, @Nullable Set<CellTypeAssignment> ctas, @Nullable Set<CellLevelCharacteristics> clcs,
            Map<SingleCellDimension, SingleCellDimension> scdCache,
            Map<SingleCellDimension, int[]> sampleIndicesCache ) {
        SingleCellExpressionDataVector newVector = new SingleCellExpressionDataVector();
        BeanUtils.copyProperties( vec, newVector, IGNORED_PROPERTIES );
        int[] sampleIndicesInVec = sampleIndicesCache.computeIfAbsent( vec.getSingleCellDimension(), k -> {
            Map<BioAssay, Integer> ba2index = ListUtils.indexOfElements( vec.getSingleCellDimension().getBioAssays() );
            int[] result = new int[assays.size()];
            for ( int i = 0; i < assays.size(); i++ ) {
                BioAssay ba = assays.get( i );
                result[i] = ba2index.get( ba );
            }
            return result;
        } );
        newVector.setSingleCellDimension( scdCache.computeIfAbsent( vec.getSingleCellDimension(), k -> sliceDimension( vec.getSingleCellDimension(), assays, cellIds, ctas, clcs, sampleIndicesInVec ) ) );
        int nnz = 0;
        int[] starts = new int[assays.size()];
        int[] ends = new int[assays.size()];
        if ( ArrayUtils.isSorted( sampleIndicesInVec ) ) {
            // assays are ordered, we can use the start/end trick
            int lastEnd = 0;
            for ( int i = 0; i < assays.size(); i++ ) {
                starts[i] = SingleCellExpressionDataVectorUtils.getSampleStart( vec, sampleIndicesInVec[i], lastEnd );
                ends[i] = SingleCellExpressionDataVectorUtils.getSampleEnd( vec, sampleIndicesInVec[i], starts[i] );
                nnz += ends[i] - starts[i];
                lastEnd = ends[i];
            }
        } else {
            // cannot use the start/end trick since we cannot assume assays are in order
            for ( int i = 0; i < assays.size(); i++ ) {
                starts[i] = SingleCellExpressionDataVectorUtils.getSampleStart( vec, sampleIndicesInVec[i], 0 );
                ends[i] = SingleCellExpressionDataVectorUtils.getSampleEnd( vec, sampleIndicesInVec[i], starts[i] );
                nnz += ends[i] - starts[i];
            }
        }
        newVector.setData( sliceData( vec, assays, starts, ends, nnz ) );
        newVector.setDataIndices( sliceIndices( vec, assays, newVector.getSingleCellDimension().getBioAssaysOffset(), sampleIndicesInVec, starts, ends, nnz ) );
        return newVector;
    }

    private static byte[] sliceData( SingleCellExpressionDataVector vec, List<BioAssay> assays, int[] starts, int[] ends, int nnz ) {
        if ( vec.getData() == null ) {
            return null;
        }
        int sizeInBytes = vec.getQuantitationType().getRepresentation().getSizeInBytes();
        if ( sizeInBytes == -1 ) {
            throw new UnsupportedOperationException( "Slicing single-cell vectors with variable-length data types is not supported." );
        }
        byte[] data = new byte[sizeInBytes * nnz];
        int newSampleOffset = 0;
        for ( int i = 0; i < assays.size(); i++ ) {
            int sampleSize = ends[i] - starts[i];
            System.arraycopy( vec.getData(), starts[i] * sizeInBytes, data, newSampleOffset * sizeInBytes, sampleSize * sizeInBytes );
            newSampleOffset += sampleSize;
        }
        return data;
    }

    private static int[] sliceIndices( SingleCellExpressionDataVector vec, List<BioAssay> assays, int[] bioAssayOffsetInNewVec, int[] sampleIndicesInVec, int[] starts, int[] ends, int nnz ) {
        if ( vec.getDataIndices() == null ) {
            return null;
        }
        int[] indices = new int[nnz];
        int newSampleOffset = 0;
        for ( int i = 0; i < assays.size(); i++ ) {
            int sampleSize = ends[i] - starts[i];
            for ( int j = 0; j < sampleSize; j++ ) {
                indices[newSampleOffset + j] = vec.getDataIndices()[starts[i] + j]
                        - vec.getSingleCellDimension().getBioAssaysOffset()[sampleIndicesInVec[i]]
                        + bioAssayOffsetInNewVec[i];
            }
            newSampleOffset += sampleSize;
        }
        return indices;
    }

    private static SingleCellDimension sliceDimension( SingleCellDimension singleCellDimension, List<BioAssay> assays,
            @Nullable List<String> cellIds, @Nullable Set<CellTypeAssignment> ctas, @Nullable Set<CellLevelCharacteristics> clcs, int[] sampleIndices ) {
        Assert.isTrue( new HashSet<>( singleCellDimension.getBioAssays() ).containsAll( assays ),
                "All the requested assays must be in " + singleCellDimension + "." );
        Assert.isTrue( new HashSet<>( assays ).size() == assays.size(),
                "Requested assays must be unique." );
        SingleCellDimension newDimension = new SingleCellDimension();
        newDimension.setBioAssays( assays );
        int numCells = 0;
        int[] starts = new int[assays.size()];
        int[] ends = new int[assays.size()];
        int[] bioAssayOffsets = new int[assays.size()];
        for ( int i = 0; i < assays.size(); i++ ) {
            bioAssayOffsets[i] = numCells;
            starts[i] = singleCellDimension.getBioAssaysOffset()[sampleIndices[i]];
            ends[i] = starts[i] + singleCellDimension.getNumberOfCellIdsBySample( sampleIndices[i] );
            numCells += ends[i] - starts[i];
        }
        newDimension.setBioAssaysOffset( bioAssayOffsets );
        newDimension.setCellIds( cellIds != null ? cellIds : sliceCellIds( singleCellDimension, assays, starts, ends, numCells ) );
        newDimension.setNumberOfCellIds( numCells );
        newDimension.setCellTypeAssignments( ctas != null ? ctas : sliceCtas( singleCellDimension, assays, starts, ends, numCells ) );
        newDimension.setCellLevelCharacteristics( clcs != null ? clcs : sliceClcs( singleCellDimension, assays, starts, ends, numCells ) );
        log.info( "Sliced " + singleCellDimension + " to " + newDimension );
        return newDimension;
    }

    public static List<String> sliceCellIds( SingleCellDimension singleCellDimension, List<BioAssay> assays, int[] starts, int[] ends, int numCells ) {
        if ( singleCellDimension.getCellIds() instanceof UninitializedList ) {
            log.info( "Cell IDs are uninitialized, returning sized uninitialized lists." );
            int size = 0;
            for ( int i = 0; i < assays.size(); i++ ) {
                size += ends[i] - starts[i];
            }
            return new UninitializedList<>( size );
        }
        List<String> cellIds = new ArrayList<>( numCells );
        for ( int i = 0; i < assays.size(); i++ ) {
            cellIds.addAll( singleCellDimension.getCellIds().subList( starts[i], ends[i] ) );
        }
        return cellIds;
    }

    public static Set<CellTypeAssignment> sliceCtas( SingleCellDimension singleCellDimension, List<BioAssay> assays, int[] starts, int[] ends, int numCells ) {
        if ( singleCellDimension.getCellTypeAssignments() instanceof UninitializedSet ) {
            log.info( "CTAs are uninitialized, returning an uninitialized set." );
            return new UninitializedSet<>();
        }
        Set<CellTypeAssignment> ctas = new HashSet<>();
        for ( CellTypeAssignment cta : singleCellDimension.getCellTypeAssignments() ) {
            List<Characteristic> cellTypes = new ArrayList<>( cta.getCellTypes() );
            int newOffset = 0;
            int[] indices = new int[numCells];
            for ( int i = 0; i < assays.size(); i++ ) {
                int start = starts[i];
                int end = ends[i];
                System.arraycopy( cta.getCellTypeIndices(), start, indices, newOffset, end - start );
                newOffset += end - start;
            }
            CellTypeAssignment newCta = CellTypeAssignment.Factory.newInstance( cta.getName(), cellTypes, indices );
            newCta.setProtocol( cta.getProtocol() );
            newCta.setPreferred( cta.isPreferred() );
            newCta.setDescription( cta.getDescription() );
            log.info( "Sliced " + cta + " to " + newCta + "." );
            ctas.add( newCta );
        }
        return ctas;
    }

    public static Set<CellLevelCharacteristics> sliceClcs( SingleCellDimension singleCellDimension, List<BioAssay> assays, int[] starts, int[] ends, int numCells ) {
        if ( singleCellDimension.getCellTypeAssignments() instanceof UninitializedSet ) {
            log.info( "CLCs are uninitialized, returning an uninitialized set." );
            return new UninitializedSet<>();
        }
        Set<CellLevelCharacteristics> clcs = new HashSet<>();
        for ( CellLevelCharacteristics clc : singleCellDimension.getCellTypeAssignments() ) {
            List<Characteristic> characteristics = new ArrayList<>( clc.getCharacteristics() );
            int newOffset = 0;
            int[] indices = new int[numCells];
            for ( int i = 0; i < assays.size(); i++ ) {
                int start = starts[i];
                int end = ends[i];
                System.arraycopy( clc.getIndices(), start, indices, newOffset, end - start );
                newOffset += end - start;
            }
            CellLevelCharacteristics newClc = CellLevelCharacteristics.Factory.newInstance( clc.getName(), clc.getDescription(), characteristics, indices );
            log.info( "Sliced " + clc + " to " + newClc + "." );
            clcs.add( newClc );
        }
        return clcs;
    }
}
