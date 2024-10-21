package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface CellLevelCharacteristicsMetadataParser<T extends CellLevelCharacteristics> {

    Set<T> parse( Path file ) throws IOException;
}
