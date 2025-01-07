package ubic.gemma.core.loader.expression.geo.service;

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
     * Retrieve a single GEO series using the default preset.
     */
    @Nullable
    default GeoRecord getGeoRecord( GeoRecordType recordType, String accession ) throws IOException {
        return getGeoRecord( recordType, accession, GeoRetrieveConfig.DEFAULT );
    }

    /**
     * Retrieve a single GEO record.
     */
    @Nullable
    GeoRecord getGeoRecord( GeoRecordType recordType, String accession, GeoRetrieveConfig config ) throws IOException;

    default Collection<GeoRecord> getGeoRecords( GeoRecordType recordType, Collection<String> accessions ) throws IOException {
        return getGeoRecords( recordType, accessions, GeoRetrieveConfig.DEFAULT );
    }

    /**
     * Retrieve a collection of GEO series.
     */
    Collection<GeoRecord> getGeoRecords( GeoRecordType recordType, Collection<String> accessions, GeoRetrieveConfig config ) throws IOException;

    /**
     * Retrieve all GEO records of a given type.
     * <p>
     * A bit hacky, can be improved. Limited to human, mouse, rat, is not guaranteed to get everything, though as of
     * 7/2021, this is sufficient (~8000 platforms)
     *
     * @param allowedTaxa a collection of allowed taxa, ignored if null or empty
     * @return all relevant platforms up to single-query limit of NCBI
     */
    Collection<GeoRecord> getAllGeoRecords( GeoRecordType recordType, @Nullable Collection<String> allowedTaxa, int maxRecords ) throws IOException;

    /**
     * Retrieve recent GEO records from <a href="https://www.ncbi.nlm.nih.gov/geo/browse/">GEO browser</a>.
     * <p>
     * The retrieved information is pretty minimal. Use {@link #searchGeoRecords(GeoRecordType, String, GeoSearchField, Collection, Collection, Collection)}
     * for detailed records.
     * @param  start    start page
     * @param  pageSize page size
     * @return a slice of GEO records
     */
    Slice<GeoRecord> getRecentGeoRecords( GeoRecordType recordType, int start, int pageSize ) throws IOException;

    /**
     * Search and retrieve GEO records.
     * @see #searchGeoRecords(GeoRecordType, String, GeoSearchField, Collection, Collection, Collection)
     * @see #retrieveGeoRecords(GeoQuery, int, int, GeoRetrieveConfig)
     */
    default Slice<GeoRecord> searchAndRetrieveGeoRecords( GeoRecordType recordType, @Nullable String searchTerms, @Nullable GeoSearchField field, @Nullable Collection<String> allowedTaxa, @Nullable Collection<String> limitPlatforms, @Nullable Collection<String> seriesTypes, int start, int pageSize, boolean detailed ) throws IOException {
        return retrieveGeoRecords( searchGeoRecords( recordType, searchTerms, field, allowedTaxa, limitPlatforms, seriesTypes ), start, pageSize, detailed ? GeoRetrieveConfig.DETAILED : GeoRetrieveConfig.DEFAULT );
    }

    /**
     * Search GEO records.
     * <p>
     * Provides more details than {@link #getRecentGeoRecords(GeoRecordType, int, int)}. Performs an E-utilities query
     * of the GEO database with the given search terms (search terms can be omitted). Returns at most pageSize records.
     * <p>
     * Note that the search is reversed in time. You get the most recent records first.
     *
     * @param recordType     the type of record to search for
     * @param searchTerms    search term, ignored if null or blank
     * @param field          a field to search in or null to search everywhere
     * @param allowedTaxa    restrict search to the given taxa if not null
     * @param limitPlatforms restrict search to the given platforms if not null
     * @param seriesTypes    restrict search to the given series types if not null (i.e. Expression profiling by array)
     * @return a GEO query that can be retrieved with {@link #retrieveGeoRecords(GeoQuery, int, int, GeoRetrieveConfig)}
     * @throws IOException if there is a problem obtaining or manipulating the file (some exceptions are not thrown and just logged)
     */
    GeoQuery searchGeoRecords( GeoRecordType recordType, @Nullable String searchTerms, @Nullable GeoSearchField field, @Nullable Collection<String> allowedTaxa, @Nullable Collection<String> limitPlatforms, @Nullable Collection<String> seriesTypes ) throws IOException;

    /**
     * Search GEO records.
     */
    GeoQuery searchGeoRecords( GeoRecordType recordType, String term ) throws IOException;

    default Slice<GeoRecord> retrieveGeoRecords( GeoQuery query, int start, int pageSize ) throws IOException {
        return retrieveGeoRecords( query, start, pageSize, GeoRetrieveConfig.DEFAULT );
    }

    /**
     * Retrieve records from a GEO query.
     * @param start    start at an offset to retrieve batches
     * @param pageSize number of results to retrieve in a batch
     * @param config   configuration for populating the records
     */
    Slice<GeoRecord> retrieveGeoRecords( GeoQuery query, int start, int pageSize, GeoRetrieveConfig config ) throws IOException;

    Collection<GeoRecord> retrieveAllGeoRecords( GeoQuery query, GeoRetrieveConfig config ) throws IOException;
}
