package ubic.gemma.model.common.description;

public enum DatabaseType {
    ONTOLOGY,
    SEQUENCE,
    LITERATURE,
    EXPRESSION,
    /**
     * Represents a genome database such as Golden Path or Ensembl
     */
    GENOME,
    OTHER,
    PROTEIN;
}