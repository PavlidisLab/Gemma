package ubic.gemma.core.loader.expression.geo.service;

import lombok.Builder;
import lombok.Value;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;

/**
 * Gets records from GEO and compares them to Gemma. This is used to identify data sets that are new in GEO and not in
 * Gemma.
 * <p>
 * See <a href="https://www.ncbi.nlm.nih.gov/geo/info/geo_paccess.html">Programmatic access to GEO</a> for some
 * information.
 * @author pavlidis
 * @author poirigui
 */
public interface GeoBrowser {

    /**
     * Configuration to use to retrieve GEO records.
     */
    @Value
    @Builder
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
    }

    /**
     * Retrieve a single GEO record using the default preset.
     */
    @Nullable
    default GeoRecord getGeoRecord( String accession ) throws IOException {
        return getGeoRecord( accession, GeoRetrieveConfig.DEFAULT );
    }

    /**
     * Retrieve a single GEO record.
     */
    @Nullable
    GeoRecord getGeoRecord( String accession, GeoRetrieveConfig config ) throws IOException;

    default Collection<GeoRecord> getGeoRecords( Collection<String> accessions ) throws IOException {
        return getGeoRecords( accessions, GeoRetrieveConfig.DEFAULT );
    }

    /**
     * Retrieve a collection of GEO records.
     */
    Collection<GeoRecord> getGeoRecords( Collection<String> accessions, GeoRetrieveConfig config ) throws IOException;

    /**
     * Retrieves and parses tab delimited file from GEO. File contains pageSize GEO records starting from startPage. The
     * retrieved information is pretty minimal.
     *
     * @param  start    start page
     * @param  pageSize page size
     * @return a slice of GEO records
     */
    Slice<GeoRecord> getRecentGeoRecords( int start, int pageSize ) throws IOException;

    /**
     * Search and retrieve GEO records.
     * @see #searchGeoRecords(String, GeoSearchField, Collection, Collection, Collection)
     * @see #retrieveGeoRecords(GeoQuery, int, int, GeoRetrieveConfig)
     */
    default Slice<GeoRecord> searchAndRetrieveGeoRecords( @Nullable String searchTerms, @Nullable GeoSearchField field, @Nullable Collection<String> allowedTaxa, @Nullable Collection<String> limitPlatforms, @Nullable Collection<String> seriesTypes, int start, int pageSize, boolean detailed ) throws IOException {
        return retrieveGeoRecords( searchGeoRecords( searchTerms, field, allowedTaxa, limitPlatforms, seriesTypes ), start, pageSize, detailed ? GeoRetrieveConfig.DETAILED : GeoRetrieveConfig.DEFAULT );
    }

    interface GeoQuery {
        String getQueryId();

        String getCookie();

        int getTotalRecords();
    }

    /**
     * Search GEO records.
     * <p>
     * Provides more details than {@link #getRecentGeoRecords(int, int)}. Performs an E-utilities query of the GEO
     * database with the given search terms (search terms can be omitted). Returns at most pageSize records. Does some
     * screening of results for expression studies, and (optionally) taxa. This is used for identifying data sets for
     * loading.
     * <p>
     * Note that the search is reversed in time. You get the most recent records first.
     *
     * @param searchTerms    search term, ignored if null or blank
     * @param field          a field to search in or null to search everywhere
     * @param allowedTaxa    restrict search to the given taxa if not null
     * @param limitPlatforms restrict search to the given platforms if not null
     * @param seriesTypes    restrict search to the given series types if not null (i.e. Expression profiling by array)
     * @return a GEO query that can be retrieved with {@link #retrieveGeoRecords(GeoQuery, int, int, GeoRetrieveConfig)}
     * @throws IOException if there is a problem obtaining or manipulating the file (some exceptions are not thrown and just logged)
     */
    GeoQuery searchGeoRecords( @Nullable String searchTerms, @Nullable GeoSearchField field, @Nullable Collection<String> allowedTaxa, @Nullable Collection<String> limitPlatforms, @Nullable Collection<String> seriesTypes ) throws IOException;

    default Slice<GeoRecord> retrieveGeoRecords( GeoQuery query, int start, int pageSize ) throws IOException {
        return retrieveGeoRecords( query, start, pageSize, GeoRetrieveConfig.DEFAULT );
    }

    /**
     * Retrieve records from a GEO query.
     * @param start    start at an offset to retrieve batches
     * @param pageSize number of results to retrieve in a batch
     * @param config   if true, additional information is fetched (slower)
     */
    Slice<GeoRecord> retrieveGeoRecords( GeoQuery query, int start, int pageSize, GeoRetrieveConfig config ) throws IOException;

    /**
     * Retrieve all GEO platforms.
     * <p>
     * A bit hacky, can be improved. Limited to human, mouse, rat, is not guaranteed to get everything, though as of
     * 7/2021, this is sufficient (~8000 platforms)
     *
     * @param allowedTaxa a collection of allowed taxa, ignored if null or empty
     * @return all relevant platforms up to single-query limit of NCBI
     */
    Collection<GeoRecord> getAllGeoPlatforms( @Nullable Collection<String> allowedTaxa ) throws IOException;
}
