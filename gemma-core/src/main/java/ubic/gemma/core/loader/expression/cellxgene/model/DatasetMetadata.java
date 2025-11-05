package ubic.gemma.core.loader.expression.cellxgene.model;

import lombok.Data;

import java.util.List;

@Data
public class DatasetMetadata {
    String id;
    String collectionId;
    String name;
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
