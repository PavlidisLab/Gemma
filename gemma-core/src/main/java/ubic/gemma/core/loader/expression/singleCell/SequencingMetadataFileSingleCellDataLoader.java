package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadataFileDataLoader;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Adapt a {@link SequencingMetadataFileDataLoader} so that it can be used as a {@link SingleCellDataLoader}.
 * @see SequencingMetadataFileDataLoader
 * @author poirigui
 */
public class SequencingMetadataFileSingleCellDataLoader extends AbstractDelegatingSingleCellDataLoader {

    private final SequencingMetadataFileDataLoader sequencingMetadataFileDataLoader;

    public SequencingMetadataFileSingleCellDataLoader( SingleCellDataLoader delegate, Path sequencingMetadataFile, @Nullable SequencingMetadata defaultMetadata, BioAssayMapper bioAssayMapper ) {
        super( delegate );
        this.sequencingMetadataFileDataLoader = new SequencingMetadataFileDataLoader( delegate, sequencingMetadataFile, defaultMetadata, bioAssayMapper );
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( Collection<BioAssay> samples ) throws IOException {
        return sequencingMetadataFileDataLoader.getSequencingMetadata( samples );
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( SingleCellDimension dimension ) throws IOException {
        return sequencingMetadataFileDataLoader.getSequencingMetadata( dimension.getBioAssays() );
    }
}

