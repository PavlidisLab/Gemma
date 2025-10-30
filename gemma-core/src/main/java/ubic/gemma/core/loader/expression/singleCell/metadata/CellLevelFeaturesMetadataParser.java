package ubic.gemma.core.loader.expression.singleCell.metadata;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface CellLevelFeaturesMetadataParser<T> {

    void setUseCellIdsIfSampleNameIsMissing( boolean useCellIdsIfSampleNameIsMissing );

    void setInferSamplesFromCellIdsOverlap( boolean inferSamplesFromCellIdsOverlap );

    void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples );

    void setIgnoreUnmatchedCellIds( boolean ignoreUnmatchedCellIds );

    Set<T> parse( Path metadataFile ) throws IOException;
}
