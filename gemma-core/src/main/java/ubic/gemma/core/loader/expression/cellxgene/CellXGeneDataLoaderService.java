package ubic.gemma.core.loader.expression.cellxgene;

import ubic.gemma.core.util.ProgressReporterFactory;
import ubic.gemma.model.common.description.DatabaseEntry;
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
     * @param datasetId          CELLxGENE permanent dataset ID, dataset version ID or dataset title, or {@code null} if
     *                           the collection holds a single dataset. The resulting accession records the permanent
     *                           ID, with the version ID as its accession version.
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
     * platform taxon does not match that of the CELLxGENE dataset, or if the dataset cannot be resolved
     */
    ExpressionExperiment fetchAndLoad( String collectionId, @Nullable String datasetId, @Nullable String assetId, ArrayDesign platform, String datasetShortName, boolean loadSingleCellData, boolean keepPooledSample, boolean keepUnknownSample, boolean dryRun ) throws IOException;

    /**
     * Replace a CELLxGENE accession that holds a dataset version ID by one that holds the permanent dataset ID.
     * <p>
     * Datasets loaded before permanent IDs were recorded store the version ID as their accession. The new accession
     * keeps that version ID as its accession version, since it designates the data that was actually loaded, and
     * keeps the same URI. The previous {@link DatabaseEntry} is removed.
     *
     * @param datasetId the permanent CELLxGENE dataset ID that the current accession is a version of
     * @return the new accession
     * @throws IllegalStateException if the experiment does not have a CELLxGENE accession, or if that accession
     *                               already has an accession version
     */
    DatabaseEntry replaceVersionAccession( ExpressionExperiment ee, String datasetId );

    /**
     * Set the progress reporter factory to use for reporting progress during data downloading.
     */
    void setProgressReporterFactory( ProgressReporterFactory progressReporterFactory );
}
