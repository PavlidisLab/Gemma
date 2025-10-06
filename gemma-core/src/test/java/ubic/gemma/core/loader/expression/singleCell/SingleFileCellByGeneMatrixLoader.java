package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.loader.util.mapper.DesignElementMapper;
import ubic.gemma.core.loader.util.mapper.EntityMapper;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Setter
public class SingleFileCellByGeneMatrixLoader extends AbstractCellByGeneMatrixLoader {

    private final Path file;

    private BioAssayMapper bioAssayToSampleNameMapper;

    private boolean ignoreUnmatchedSamples = true;

    private DesignElementMapper designElementToGeneMapper;

    public SingleFileCellByGeneMatrixLoader( Path file ) {
        this.file = file;
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException, IllegalArgumentException {
        Assert.notNull( bioAssayToSampleNameMapper, "A mapper between BioAssay and sample names must be supplied." );
        EntityMapper.StatefulEntityMapper<BioAssay> mapper = bioAssayToSampleNameMapper.forCandidates( bioAssays );
        List<String> cellIds = new ArrayList<>();
        readCellIds( file, cellIds );
        Map<String, BioAssay> mapping = mapper.matchOne( cellIds );
        LinkedHashMap<BioAssay, List<String>> cellIdsByAssay = new LinkedHashMap<>();
        for ( String cellId : cellIds ) {
            BioAssay ba = mapping.get( cellId );
            if ( ba != null ) {
                cellIdsByAssay
                        .computeIfAbsent( ba, k -> new ArrayList<>() )
                        .add( cellId );
            } else if ( ignoreUnmatchedSamples ) {
                log.warn( "Cannot match an assay for cell ID: " + cellId + "." );
            } else {
                throw new IllegalArgumentException( "Cannot match an assay for cell ID: " + cellId + "." );
            }
        }

        // reorganize cell IDs
        cellIds.clear();
        int i = 0;
        int k = 0;
        int[] usedAssaysOffset = new int[cellIdsByAssay.size()];
        for ( List<String> c : cellIdsByAssay.values() ) {
            cellIds.addAll( c );
            usedAssaysOffset[i++] = k;
            k += c.size();
        }

        // TODO: match bioassays against cell IDs
        SingleCellDimension dimension = new SingleCellDimension();
        dimension.setCellIds( cellIds );
        dimension.setNumberOfCells( cellIds.size() );
        dimension.setBioAssays( new ArrayList<>( cellIdsByAssay.keySet() ) );
        dimension.setBioAssaysOffset( usedAssaysOffset );
        return dimension;
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Collection<CompositeSequence> designElements, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException, IllegalArgumentException {
        if ( isTranspose() ) {
            throw new UnsupportedOperationException( "Loading single-cell vectors from a transposed matrix is not supported." );
        } else {
            EntityMapper.StatefulEntityMapper<CompositeSequence> mapper = designElementToGeneMapper.forCandidates( designElements );
            Collection<String> unresolvedGeneIds = new LinkedHashSet<>();
            return parseMatrix( file ).stream()
                    .map( record -> readVector( dimension, quantitationType, mapper, record, unresolvedGeneIds ) )
                    .filter( Objects::nonNull )
                    .onClose( () -> {
                        if ( !unresolvedGeneIds.isEmpty() ) {
                            //
                            log.warn( String.format( "Could not resolve a design elements for %d genes. Here's a few examples: %s.",
                                    unresolvedGeneIds.size(),
                                    unresolvedGeneIds.stream().limit( 10 ).collect( Collectors.joining( ", " ) ) ) );
                        }
                    } );
        }
    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        return Collections.singleton( detectQt( file ) );
    }

    @Override
    public Set<String> getGenes() throws IOException {
        Set<String> genes = new HashSet<>();
        readGenes( file, genes );
        return genes;
    }

    @Override
    public Stream<String> streamGenes() throws IOException {
        return streamGenes( file );
    }

    @Override
    public void close() throws IOException {

    }
}
