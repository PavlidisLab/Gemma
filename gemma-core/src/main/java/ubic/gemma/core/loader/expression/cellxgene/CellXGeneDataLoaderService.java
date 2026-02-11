package ubic.gemma.core.loader.expression.cellxgene;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * High-level service for fetching and loading CELLxGENE datasets.
 *
 * @author poirigui
 */
public interface CellXGeneDataLoaderService {

    /**
     * Fetch a CELLxGENE dataset and load it into the database.
     *
     * @param datasetId          CELLxGENE dataset identifier
     * @param assetId            CELLxGENE dataset asset identifier
     * @param platform           platform to use for mapping design elements from the data, the primary taxon must
     *                           correspond to that of the dataset.
     * @param datasetShortName   short name to use for the resulting dataset
     * @param loadSingleCellData whether to load the single-cell data vectors, this can be done later if needed
     * @param keepPooledSample   whether to keep the "pooled" sample
     * @param keepUnknownSample  whether to keep the "unknown" sample
     * @return a persistent {@link ExpressionExperiment} pre-populated with CELLxGENE metadata and single-cell data (if
     * requested)
     * @throws IllegalArgumentException if a dataset with the given short name already exists in the database, or if the
     * platform taxon does not match that of the CELLxGENE dataset
     */
    ExpressionExperiment fetchAndLoad( String collectionId, @Nullable String datasetId, @Nullable String assetId, ArrayDesign platform, String datasetShortName, boolean loadSingleCellData, boolean keepPooledSample, boolean keepUnknownSample ) throws IOException;
}
