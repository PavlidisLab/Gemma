package ubic.gemma.core.loader.expression.geo.service;

import lombok.Value;

/**
 * Represents a GEO query.
 * @author poirigui
 */
@Value
public class GeoQuery {
    /**
     * Type of record being queried.
     */
    GeoRecordType recordType;
    String queryId;
    String cookie;
    int totalRecords;
}
