package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.Builder;
import lombok.Getter;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;

import javax.annotation.Nullable;

/**
 * Configuration for aggregating single-cell data.
 * @author poirigui
 */
@Getter
@Builder
public
class SingleCellAggregationConfig {
    /**
     * Categorical mask to use on data.
     * <p>
     * The category of the characteristics must be {@link ubic.gemma.model.common.description.Categories#MASK}.
     */
    @Nullable
    CellLevelCharacteristics mask;
    /**
     * Make the resulting QT preferred.
     */
    boolean makePreferred;
    /**
     * Perform adjustment of library sizes to better reflect the number of reads in the source sample.
     */
    boolean adjustLibrarySizes;
    /**
     * Include masked cells in the library size calculation.
     * <p>
     * The default is to exclude them as if they were simply filtered out.
     */
    boolean includeMaskedCellsInLibrarySize;
}
