package ubic.gemma.core.loader.expression.cellxgene.model;

import lombok.Data;

import java.util.List;

@Data
public class DatasetMetadata {
    /**
     * Dataset version identifier; it changes whenever the dataset is revised. Use {@link DatasetVersion#getDatasetId()}
     * for the permanent identifier.
     */
    String id;
    String collectionId;
    String name;
    /**
     * Publication timestamp (seconds since the epoch), or {@code null} if not provided.
     */
    Double publishedAt;
    int cellCount;
    List<OntologyTerm> organism;
    List<OntologyTerm> cellType;
    List<OntologyTerm> developmentStage;
    List<OntologyTerm> disease;
    List<OntologyTerm> tissue;
    List<OntologyTerm> assay;
    List<DatasetAsset> datasetAssets;
    List<String> donorId;
    double meanGenesPerCell;
}
