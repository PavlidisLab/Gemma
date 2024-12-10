package ubic.gemma.core.loader.expression.geo.service;

import lombok.Builder;
import lombok.Value;

/**
 * Configuration to use to retrieve GEO records from a {@link GeoQuery}.
 * @author poirigui
 */
@Value
@Builder
public
class GeoRetrieveConfig {
    /**
     * Default preset for retrieving GEO records.
     */
    public static GeoRetrieveConfig DEFAULT = GeoRetrieveConfig.builder().build();
    /**
     * A preset configuration for retrieving detailed GEO records.
     */
    public static GeoRetrieveConfig DETAILED = GeoRetrieveConfig.builder()
            .subSeriesStatus( true )
            .libraryStrategy( true )
            .sampleSources( true )
            .sampleCharacteristics( true )
            .meshHeadings( true )
            .build();
    /**
     * Fill MeSH headings.
     * <p>
     * This requires an additional query to PubMed.
     */
    boolean meshHeadings;
    /**
     * Fill sub-series status.
     * <p>
     * This requires a detailed sample query.
     */
    boolean subSeriesStatus;
    /**
     * FIll library strategy.
     */
    boolean libraryStrategy;
    /**
     * Fill sample sources.
     * <p>
     * This requires a detailed sample query.
     */
    boolean sampleSources;
    /**
     * Fill sample characteristics.
     * <p>
     * This requires a detailed sample query.
     */
    boolean sampleCharacteristics;
    /**
     * Ignore errors when retrieving additional information.
     * <p>
     * If set to true, any errors while retrieving additional information will be logged instead of thrown.
     */
    boolean ignoreErrors;
}
