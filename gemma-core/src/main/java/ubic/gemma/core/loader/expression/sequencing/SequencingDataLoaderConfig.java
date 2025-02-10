package ubic.gemma.core.loader.expression.sequencing;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ubic.gemma.core.loader.expression.DataLoaderConfig;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Configuration for loading sequencing data.
 * @author poirigui
 */
@Getter
@SuperBuilder
public class SequencingDataLoaderConfig extends DataLoaderConfig {

    /**
     * Default sequencing metadata to use.
     * <p>
     * This can be overwritten by the metadata file {@link #sequencingMetadataFile} or by the delegate loader.
     */
    @Nullable
    private SequencingMetadata defaultSequencingMetadata;

    /**
     * File containing per-assay sequencing metadata.
     * <p>
     * Values from the file overrides the default metadata and the metadata from the delegate loader if any.
     * @see SequencingMetadataFileDataLoader
     */
    @Nullable
    private Path sequencingMetadataFile;
}
