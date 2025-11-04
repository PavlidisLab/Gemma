package ubic.gemma.core.loader.expression.cellxgene.model;

import lombok.Data;

@Data
public class DatasetAssetDownloadMetadata {
    String datasetId;
    long fileSize;
    String url;
}
