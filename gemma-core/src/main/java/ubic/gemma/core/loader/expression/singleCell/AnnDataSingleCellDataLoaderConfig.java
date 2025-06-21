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

    @Nullable
    private String sampleFactorName;
    @Nullable
    private String cellTypeFactorName;
    private boolean ignoreCellTypeFactor;
    @Nullable
    private String unknownCellTypeIndicator;
    @Nullable
    private Boolean transpose;
    @Nullable
    private Boolean useRawX;
}
