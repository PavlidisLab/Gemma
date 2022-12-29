

package ubic.gemma.model.common.description;

import java.util.*;

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