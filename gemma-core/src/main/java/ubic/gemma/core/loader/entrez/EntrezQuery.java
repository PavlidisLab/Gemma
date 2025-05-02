package ubic.gemma.core.loader.entrez;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EntrezQuery {
    private final String queryId;
    private final String cookie;
    private final int totalRecords;
}
