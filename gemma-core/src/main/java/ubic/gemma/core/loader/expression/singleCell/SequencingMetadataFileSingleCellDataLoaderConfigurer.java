package ubic.gemma.core.loader.expression.singleCell;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;

public class SequencingMetadataFileSingleCellDataLoaderConfigurer implements SingleCellDataLoaderConfigurer<SequencingMetadataFileSingleCellDataLoader> {

    private final SingleCellDataLoaderConfigurer<?> delegate;
    private final BioAssayMapper bioAssayMapper;

    public SequencingMetadataFileSingleCellDataLoaderConfigurer( SingleCellDataLoaderConfigurer<?> delegate, BioAssayMapper bioAssayMapper ) {
        this.delegate = delegate;
        this.bioAssayMapper = bioAssayMapper;
    }

    @Override
    public SequencingMetadataFileSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        Assert.isTrue( config.getSequencingMetadataFile() != null, "The sequencingMetadataFile field must be set." );
        return new SequencingMetadataFileSingleCellDataLoader( delegate.configureLoader( config ), config.getSequencingMetadataFile(), config.getDefaultSequencingMetadata(), bioAssayMapper );
    }
}
