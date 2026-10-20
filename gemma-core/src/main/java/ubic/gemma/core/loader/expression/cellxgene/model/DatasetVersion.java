package ubic.gemma.core.loader.expression.cellxgene.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identity of a CELLxGENE dataset as reported by the curation API.
 * <p>
 * The {@link #datasetId} is permanent and survives revisions, whereas the {@link #datasetVersionId} changes every time
 * the dataset is revised. The latter is what the data portal API reports as {@link DatasetMetadata#getId()} and what
 * download links are keyed on.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetVersion {
    String datasetId;
    String datasetVersionId;
    String title;
}
