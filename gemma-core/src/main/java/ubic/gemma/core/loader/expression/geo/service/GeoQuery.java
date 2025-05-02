package ubic.gemma.core.loader.expression.geo.service;

import lombok.Getter;
import ubic.gemma.core.loader.entrez.EntrezQuery;

/**
 * Represents a GEO query.
 * @author poirigui
 */
@Getter
public class GeoQuery extends EntrezQuery {
    /**
     * Type of record being queried.
     */
    private final GeoRecordType recordType;

    public GeoQuery( GeoRecordType recordType, String queryId, String cookie, int totalRecords ) {
        super( queryId, cookie, totalRecords );
        this.recordType = recordType;
    }
}
