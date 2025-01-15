package ubic.gemma.core.loader.expression.singleCell;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class MexSingleCellDataLoaderConfig extends SingleCellDataLoaderConfig {

    private boolean discardEmptyCells;

    private boolean allowMappingDesignElementsToGeneSymbols;
}
