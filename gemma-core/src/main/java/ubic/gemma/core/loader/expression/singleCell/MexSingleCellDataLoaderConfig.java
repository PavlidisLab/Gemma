package ubic.gemma.core.loader.expression.singleCell;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;

@Getter
@SuperBuilder
public class MexSingleCellDataLoaderConfig extends SingleCellDataLoaderConfig {

    @Nullable
    private Boolean discardEmptyCells;

    private boolean allowMappingDesignElementsToGeneSymbols;

    private boolean useDoublePrecision;
}
