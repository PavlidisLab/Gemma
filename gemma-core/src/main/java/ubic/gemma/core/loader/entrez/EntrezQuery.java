package ubic.gemma.core.loader.entrez;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents an Entrez search query.
 * @author poirigui
 */
@Getter
@AllArgsConstructor
public class EntrezQuery {
    private final String queryId;
    private final String cookie;
    private final int totalRecords;
}
