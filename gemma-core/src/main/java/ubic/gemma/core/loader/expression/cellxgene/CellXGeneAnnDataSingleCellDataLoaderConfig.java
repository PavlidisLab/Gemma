package ubic.gemma.core.loader.expression.cellxgene;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoaderConfig;

/**
 * A configuration for loading CELLxGENE data from AnnData files.
 *
 * @author poirigui
 */
@Getter
@SuperBuilder
public class CellXGeneAnnDataSingleCellDataLoaderConfig extends AnnDataSingleCellDataLoaderConfig {

    /**
     * Keep the pooled sample (if any) in the dataset.
     * <p>
     * This sample uses the "pooled" indicator as donor ID.
     */
    boolean keepPooledSample;
    /**
     * Keep the unknown sample (if any) in the dataset.
     * <p>
     * This sample uses the "unknown" indicator as donor ID.
     */
    boolean keepUnknownSample;
}
