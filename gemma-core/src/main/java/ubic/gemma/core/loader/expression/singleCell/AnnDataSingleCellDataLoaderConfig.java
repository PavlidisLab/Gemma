package ubic.gemma.core.loader.expression.singleCell;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;

/**
 * A configuration for loading single-cell data form AnnData format.
 * @see AnnDataSingleCellDataLoader
 */
@Getter
@SuperBuilder
public class AnnDataSingleCellDataLoaderConfig extends SingleCellDataLoaderConfig {

    private String sampleFactorName;
    private String cellTypeFactorName;
    @Nullable
    private String unknownCellTypeIndicator;
}
