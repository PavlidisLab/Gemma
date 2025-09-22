package ubic.gemma.core.loader.expression.singleCell;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCell10xMexFilter;

import javax.annotation.Nullable;

@Getter
@SuperBuilder
public class MexSingleCellDataLoaderConfig extends SingleCellDataLoaderConfig {

    private boolean allowMappingDesignElementsToGeneSymbols;

    /**
     * Filter 10x MEX data to remove low-quality cells.
     * <p>
     * Default is to auto-detect.
     * <p>
     * This should only be applied to unfiltered MEX data from 10x Chromium platform.
     * @see SingleCell10xMexFilter
     */
    @Nullable
    private Boolean apply10xFilter;

    /**
     * Chemistry used for single-cell sequencing.
     * <p>
     * This affects the 10x MEX data filter.
     */
    @Nullable
    private String use10xChemistry;

    private boolean useDoublePrecision;
}
