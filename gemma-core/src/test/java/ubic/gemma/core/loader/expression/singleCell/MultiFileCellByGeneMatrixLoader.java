package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.loader.util.mapper.DesignElementMapper;
import ubic.gemma.core.loader.util.mapper.EntityMapper;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load a cell-by-gene matrices
 */
@Setter
public class MultiFileCellByGeneMatrixLoader extends AbstractCellByGeneMatrixLoader {

    /**
     * If non-null, those are the sample names present in the data.
     */
    private final List<String> sampleNames;

    private final List<Path> multipleFiles;

    private BioAssayMapper bioAssayToSampleNameMapper;

    private boolean ignoreUnmatchedSamples = true;

    private DesignElementMapper designElementToGeneMapper;

    public MultiFileCellByGeneMatrixLoader( List<String> sampleNames, List<Path> multipleFiles ) {
        Assert.isTrue( sampleNames.size() == multipleFiles.size() );
        this.sampleNames = sampleNames;
        this.multipleFiles = multipleFiles;
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException, IllegalArgumentException {
        EntityMapper.StatefulEntityMapper<BioAssay> mapper = bioAssayToSampleNameMapper.forCandidates( bioAssays );
        Map<String, BioAssay> mapping = mapper.matchOne( sampleNames );
        // TODO: match bioassays against sample names
        List<BioAssay> usedAssays = new ArrayList<>();
        List<String> cellIds = new ArrayList<>();
        for ( int i = 0; i < multipleFiles.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            Path sample = multipleFiles.get( i );
            BioAssay ba = mapping.get( sampleName );
            if ( ba != null ) {
                readCellIds( sample, cellIds );
                usedAssays.add( ba );
                // TODO: populate assay offsets
            } else if ( ignoreUnmatchedSamples ) {
                log.warn( "Cannot match a sample for " + sampleName + "." );
            } else {
                throw new IllegalArgumentException( "Cannot match a sample for " + sampleName + "." );
            }
        }
        SingleCellDimension scd = new SingleCellDimension();
        scd.setCellIds( cellIds );
        scd.setNumberOfCells( cellIds.size() );
        scd.setBioAssays( usedAssays );
        // TODO: scd.setBioAssaysOffset( bioAssayOffset... );
        return scd;
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Collection<CompositeSequence> designElements, SingleCellDimension dimension, QuantitationType quantitationType ) throws IOException, IllegalArgumentException {
        Map<String, BioAssay> mapping = bioAssayToSampleNameMapper
                .forCandidates( dimension.getBioAssays() )
                .matchOne( sampleNames );
        for ( Path e : multipleFiles ) {

        }
        return Stream.empty();
    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        return new HashSet<>( sampleNames );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because data originates from multiple matrices, the resulting QTs will be merged with
     * {@link QuantitationTypeUtils#mergeQuantitationTypes(Collection)}. It is possible that the data is incompatible.
     */
    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
        return Collections.singleton( QuantitationTypeUtils.mergeQuantitationTypes( multipleFiles.stream().map( this::detectQt ).collect( Collectors.toSet() ) ) );
    }

    @Override
    public Set<String> getGenes() throws IOException {
        Set<String> genes = new HashSet<>();
        for ( Path f : multipleFiles ) {
            readGenes( f, genes );
        }
        return genes;
    }

    public Stream<String> streamGenes() throws IOException {
        try {
            return multipleFiles.stream()
                    .flatMap( file -> {
                        try {
                            return streamGenes( file );
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }
                    } )
                    .distinct();
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof IOException ) {
                throw ( IOException ) e.getCause();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void close() throws IOException {

    }
}
