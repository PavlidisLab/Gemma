package ubic.gemma.core.loader.expression.geo.service;

/**
 * Amounts of data that can be retrieved from GEO.
 *
 * @author poirigui
 */
public enum GeoAmount {
    /**
     * Add only metadata, no data tables.
     */
    BRIEF,
    /**
     * Add the first 20 lines of all data tables.
     */
    QUICK,
    /**
     * Add all data tables.
     */
    FULL,
    /**
     * Only include data tables.
     */
    DATA
}
