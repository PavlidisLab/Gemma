/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.loader.expression.geo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.expression.geo.fetcher.DatasetFetcher;
import ubic.gemma.core.loader.expression.geo.fetcher.PlatformFetcher;
import ubic.gemma.core.loader.expression.geo.fetcher.SeriesFetcher;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.loader.expression.geo.fetcher2.GeoFetcher;
import ubic.gemma.core.loader.expression.geo.model.*;
import ubic.gemma.core.loader.expression.geo.service.GeoAmount;
import ubic.gemma.core.loader.expression.geo.service.GeoFormat;
import ubic.gemma.core.loader.expression.geo.service.GeoScope;
import ubic.gemma.core.loader.expression.geo.service.GeoSource;
import ubic.gemma.core.loader.expression.geo.service.GeoUtils;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.util.SimpleRetry;
import ubic.gemma.core.util.SimpleRetryPolicy;
import org.apache.commons.io.IOUtils;
import ubic.gemma.core.loader.util.fetcher.Fetcher;
import ubic.gemma.core.loader.util.sdo.SourceDomainObjectGenerator;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;

import org.springframework.lang.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Handle fetching and parsing GEO files.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class GeoDomainObjectGenerator implements SourceDomainObjectGenerator {

    protected static final Log log = LogFactory.getLog( GeoDomainObjectGenerator.class.getName() );

    /**
     * Same policy the MINiML reads use: three attempts, backing off from ~1 s.
     */
    private static final SimpleRetry<IOException> metadataRetry = new SimpleRetry<>(
            new SimpleRetryPolicy( 3, 1001, 1.5 ), IOException.class,
            GeoDomainObjectGenerator.class.getName() );

    /**
     * Where metadata-only records are cached; null means take it from the configuration.
     */
    @Nullable
    private File metadataCacheDir;

    /**
     * So a corpus sweep does not print the same permissions problem thousands of times; see
     * {@link #writeMetadataCacheFile(File, byte[])}.
     */
    private final AtomicBoolean cacheWriteFailureReported = new AtomicBoolean( false );

    private final Fetcher datasetFetcher;
    private final Fetcher seriesFetcher;
    private final Fetcher platformFetcher;

    /**
     * Resilient series-family SOFT downloader: tries FTP, falls back to HTTPS, then to GEO's
     * on-demand generator. When set (production path, wired by {@code GeoServiceImpl}) it is used
     * in preference to the legacy FTP-only {@link #seriesFetcher}; left null in bare test setups,
     * which then fall back to {@link #seriesFetcher}.
     */
    @Nullable
    private GeoFetcher seriesFamilySoftFetcher;

    private String ncbiApiKey;
    private boolean processPlatformsOnly;
    private boolean doSampleMatching = true;

    public GeoDomainObjectGenerator() {
        this( new DatasetFetcher(), new SeriesFetcher(), new PlatformFetcher() );
    }

    protected GeoDomainObjectGenerator( DatasetFetcher datasetFetcher, SeriesFetcher seriesFetcher, Fetcher platformFetcher ) {
        this.datasetFetcher = datasetFetcher;
        this.seriesFetcher = seriesFetcher;
        this.platformFetcher = platformFetcher;
    }

    /**
     * @param geoAccession, either a GPL, GDS or GSE value.
     * @return If processPlatformsOnly is true, a collection of GeoPlatforms. Otherwise a Collection of series (just
     * one). If the accession is a GPL then processPlatformsOnly is set to true and any sample data is ignored.
     */
    @Override
    public Collection<? extends GeoData> generate( String geoAccession ) {
        GeoDomainObjectGenerator.log
                .info( "Generating objects for " + geoAccession + " using " + this.getClass().getSimpleName() );
        GeoFamilyParser parser = new GeoFamilyParser();
        Collection<GeoData> result = new HashSet<>();
        if ( geoAccession.startsWith( "GPL" ) ) {
            this.processPlatformsOnly = true;
            GeoPlatform platform = this.processPlatform( geoAccession, parser );
            result.add( platform );
        } else if ( geoAccession.startsWith( "GDS" ) ) {
            // common starting point.
            Collection<String> seriesAccessions = DatasetCombiner.findGSEforGDS( geoAccession, ncbiApiKey );
            if ( processPlatformsOnly ) {
                return this.processSeriesPlatforms( seriesAccessions, parser );
            }
            GeoDomainObjectGenerator.log.info( geoAccession + " corresponds to " + seriesAccessions );
            for ( String seriesAccession : seriesAccessions ) {
                GeoSeries series = this.processSeries( seriesAccession, parser );
                if ( series == null )
                    continue;
                result.add( series );
            }
        } else if ( geoAccession.startsWith( "GSE" ) ) {
            if ( processPlatformsOnly ) {
                return this.processSeriesPlatforms( geoAccession, parser );
            }
            GeoSeries series = this.processSeries( geoAccession, parser );
            if ( series == null )
                return result;
            result.add( series );
            return result;
        } else {
            throw new IllegalArgumentException(
                    "Cannot handle accession: " + geoAccession + ", must be a GDS, GSE or GPL" );
        }
        return result;

    }

    /**
     * Determine the set of external accession values that will be generated during parsing. This can be used to
     * pre-empty time-consuming fetch and download of data we already have.
     *
     * @param geoAccession geo accession
     * @return database entries
     */
    @Nullable
    public Collection<DatabaseEntry> getProjectedAccessions( String geoAccession ) {
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( ExternalDatabases.GEO );
        Collection<DatabaseEntry> accessions = new HashSet<>();
        // DatabaseEntry

        StringBuilder seriesAccession = new StringBuilder();
        if ( geoAccession.startsWith( "GSE" ) ) {
            seriesAccession = new StringBuilder( geoAccession );
        } else if ( geoAccession.startsWith( "GPL" ) ) {
            GeoDomainObjectGenerator.log.warn( "Determining if the data already exist for a GPL (" + geoAccession
                    + ") is not implemented." );
            return null;
        } else if ( geoAccession.startsWith( "GDS" ) ) {
            Collection<String> seriesAccessions = DatasetCombiner.findGSEforGDS( geoAccession, ncbiApiKey );
            if ( seriesAccessions.isEmpty() ) {
                throw new InvalidAccessionException( "There is no series (GSE) for the accession " + geoAccession );
            }
            for ( String string : seriesAccessions ) {
                seriesAccession.append( string ).append( "," );
            }
            seriesAccession = new StringBuilder( StringUtils.removeEnd( seriesAccession.toString(), "," ) );
        } else {
            if ( StringUtils.isBlank( geoAccession ) ) {
                throw new InvalidAccessionException( "GEO accession must not be blank. Enter a  GSE, GDS or GPL" );
            }
            throw new InvalidAccessionException( "'" + geoAccession
                    + "' is not understood by Gemma; must be a GSE, GDS or GPL. Did you choose the right source database?" );
        }

        DatabaseEntry de = DatabaseEntry.Factory.newInstance( ed );

        de.setAccession( seriesAccession.toString() );
        accessions.add( de );

        return accessions;
    }

    /**
     * Process a data set and add it to the series
     */
    public void processDataSet( GeoSeries series, String dataSetAccession, GeoFamilyParser parser ) {
        GeoDomainObjectGenerator.log.info( "Processing " + dataSetAccession );
        GeoDataset gds = this.processDataSet( dataSetAccession, parser );
        assert gds != null;

        boolean ok = this.checkDatasetMatchesSeries( series, gds );
        if ( !ok ) {
            GeoDomainObjectGenerator.log
                    .warn( dataSetAccession + " does not use a platform associated with the series " + series
                            + ", ignoring." );
            return;
        }

        series.addDataSet( gds );
        gds.getSeries().add( series );
    }

    /**
     * Set the NCBI API key to use.
     * <p>
     * This is only used to resolve GEO datasets from series, so it is not critical for the good functioning of this
     * class, but it is preferable to set it.
     */
    public void setNcbiApiKey( String ncbiApiKey ) {
        this.ncbiApiKey = ncbiApiKey;
    }

    /**
     * Wire the resilient (FTP → HTTPS → GEO-generate) series-family SOFT downloader. NCBI has been
     * deprecating anonymous FTP, so relying on the legacy FTP-only fetcher alone fails on files that
     * are still served fine over HTTPS.
     */
    public void setSeriesFamilySoftFetcher( GeoFetcher seriesFamilySoftFetcher ) {
        this.seriesFamilySoftFetcher = seriesFamilySoftFetcher;
    }

    /**
     * Download the series-family SOFT file, preferring the resilient fetcher when wired and falling
     * back to the legacy FTP-only {@link #seriesFetcher} otherwise. Returns {@code null} only on the
     * legacy path (e.g. a cancelled fetch); the resilient path throws once all fallbacks are exhausted.
     */
    @Nullable
    private File fetchSeriesFamilySoftFile( String seriesAccession ) {
        if ( seriesFamilySoftFetcher != null ) {
            try {
                return seriesFamilySoftFetcher.fetchSeriesFamilySoftFile( seriesAccession ).toFile();
            } catch ( IOException e ) {
                throw new RuntimeException( "Failed to fetch the SOFT file for " + seriesAccession, e );
            }
        }
        Collection<File> fullSeries = seriesFetcher.fetch( seriesAccession );
        return fullSeries != null ? fullSeries.iterator().next() : null;
    }

    /**
     * Override where metadata-only records are cached; defaults to {@code geo.local.datafile.basepath},
     * the directory the family SOFT files already live in.
     */
    public void setMetadataCacheDir( @Nullable File metadataCacheDir ) {
        this.metadataCacheDir = metadataCacheDir;
    }

    public void setDoSampleMatching( boolean doSampleMatching ) {
        this.doSampleMatching = doSampleMatching;
    }

    public void setProcessPlatformsOnly( boolean b ) {
        this.processPlatformsOnly = b;
    }

    /**
     * It is possible for the GDS to use a platform not used by the GSE. Yep. GSE2121 is on GPL81, and is associated
     * with GDS1862; but GSE2122 (GPL11) is not, but GDS1862 is linked to GSE2122 anyway. There is no superseries
     * relationship there.
     *
     * @param series series
     * @param gds    geo dataset
     * @return true if the dataset uses a platform that the series uses.
     */
    private boolean checkDatasetMatchesSeries( GeoSeries series, GeoDataset gds ) {
        GeoPlatform platform = gds.getPlatform();
        assert platform != null;
        for ( GeoSample s : series.getSamples() ) {
            for ( GeoPlatform p : s.getPlatforms() ) {
                if ( p.equals( platform ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private String fetchDataSetToLocalFile( String geoDataSetAccession ) {
        Collection<File> result = datasetFetcher.fetch( geoDataSetAccession );

        if ( result == null )
            return null;

        if ( result.size() != 1 ) {
            throw new IllegalStateException(
                    "Got " + result.size() + " files for " + geoDataSetAccession + ", expected only one." );
        }

        File dataSetFile = ( result.iterator() ).next();
        String dataSetPath;

        dataSetPath = dataSetFile.getPath();

        return dataSetPath;
    }

    /**
     * Process a data set from an accession values
     *
     * @param dataSetAccession dataset accession
     * @return A GeoDataset object
     */
    private GeoDataset processDataSet( String dataSetAccession, GeoFamilyParser parser ) {
        if ( !dataSetAccession.startsWith( "GDS" ) ) {
            throw new IllegalArgumentException( "Invalid GEO dataset accession " + dataSetAccession );
        }
        String dataSetPath = this.fetchDataSetToLocalFile( dataSetAccession );
        GeoDataset gds;
        try {
            gds = this.processDataSet( dataSetAccession, dataSetPath, parser );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return gds;
    }

    /**
     * Parse a GEO GDS file, return the extracted GeoDataset.
     *
     * @param geoDataSetAccession geo dataset accession
     * @param dataSetPath         dataset path
     * @return GeoDataset
     * @throws IOException if there is a problem while manipulating the file
     */
    private GeoDataset processDataSet( String geoDataSetAccession, String dataSetPath, GeoFamilyParser parser ) throws IOException {
        parser.parse( dataSetPath );

        // first result is where we start.
        GeoParseResult results = parser.getResults().iterator().next();

        Map<String, GeoDataset> datasetMap = results.getDatasets();
        if ( !datasetMap.containsKey( geoDataSetAccession ) ) {
            throw new IllegalStateException( "Failed to get parse of " + geoDataSetAccession );
        }

        return datasetMap.get( geoDataSetAccession );
    }

    private GeoPlatform processPlatform( String geoAccession, GeoFamilyParser parser ) {
        assert platformFetcher != null;
        Collection<File> platforms = platformFetcher.fetch( geoAccession );
        if ( platforms == null ) {
            throw new RuntimeException( "No series file found for " + geoAccession );
        }
        File platformFile = ( platforms.iterator() ).next();
        String platformPath;

        platformPath = platformFile.getPath();

        parser.setProcessPlatformsOnly( true );
        try {
            parser.parse( platformPath );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        return parser.getResults().iterator().next().getPlatformMap().get( geoAccession );
    }

    /**
     * Download and parse a GEO series.
     */
    private GeoSeries processSeries( String seriesAccession, GeoFamilyParser parser ) {

        File seriesFile = fetchSeriesFamilySoftFile( seriesAccession );
        if ( seriesFile == null ) {
            GeoDomainObjectGenerator.log.warn( "No series file found for " + seriesAccession );
            return null;
        }
        String seriesPath = seriesFile.getPath();

        parser.setProcessPlatformsOnly( this.processPlatformsOnly );

        try {
            parser.parse( seriesPath );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }

        // Only allow one series...
        GeoSeries series = parser.getResults().iterator().next().getSeriesMap()
                .get( seriesAccession );

        if ( series == null ) {
            throw new RuntimeException( "No series was parsed for " + seriesAccession );
        }

        Collection<String> datasetsToProcess = DatasetCombiner.findGDSforGSE( seriesAccession, ncbiApiKey );
        if ( datasetsToProcess != null ) {
            for ( String dataSetAccession : datasetsToProcess ) {
                this.processDataSet( series, dataSetAccession, parser );
            }
        }

        DatasetCombiner datasetCombiner = new DatasetCombiner( this.doSampleMatching );

        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        assert correspondence != null;
        series.setSampleCorrespondence( correspondence );

        return series;
    }

    /**
     * Fetch and parse only the METADATA of a series: its own record plus its sample records, with no
     * data tables and no platform record.
     * <p>
     * The family SOFT file that {@link #processSeries} downloads embeds the platform table and every
     * sample's data table, which is the whole of its size: GSE1024's is 36 MB on disk, against 60 KB
     * for the two records read here (measured 2026-08-29). Nothing that reads a
     * {@code sourceMetadata} document needs either table, so the backfill pays 36 MB per experiment
     * for two fields it uses.
     * <p>
     * It is still SOFT, so {@link GeoFamilyParser} reads it unchanged; the two responses are parsed
     * into one parser, series first, because the sample records attach to the series the
     * {@code !Series_sample_id} lines named. What is NOT done here is everything conversion needs —
     * no GDS lookup, no sample correspondence — because no {@code ExpressionExperiment} is built.
     *
     * @throws RuntimeException if GEO answers with no series, which includes the case of an
     *                          accession that does not exist
     */
    public GeoSeries generateSeriesMetadataOnly( String seriesAccession ) {
        log.info( "Fetching metadata-only records for " + seriesAccession + " (no data tables)." );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setMetadataOnly( true );
        // series first: the sample records that follow attach to what it declared
        byte[] self = this.parseRecord( seriesAccession, GeoScope.SELF, parser, true );
        // A series GEO has retired lists no samples, and its sample records are then legitimately
        // empty -- GSE1829, titled "RETIRED", zero !Series_sample_id lines, and targ=gsm answers
        // with nothing at all. That is GEO having no samples to give, not a failed fetch, so the
        // series record is what says whether an empty answer is expected.
        boolean declaresSamples = countLines( self, "!Series_sample_id" ) > 0;
        this.parseRecord( seriesAccession, GeoScope.SAMPLES, parser, declaresSamples );
        if ( !declaresSamples ) {
            log.info( seriesAccession + " lists no samples at GEO; storing the series record alone." );
        }
        GeoSeries series = parser.getResults().iterator().next().getSeriesMap().get( seriesAccession );
        if ( series == null ) {
            throw new RuntimeException( "No series was parsed for " + seriesAccession );
        }
        return series;
    }

    /**
     * Read one acc.cgi record into the parser, from the local cache when it is there.
     * <p>
     * The body is buffered before parsing rather than streamed: {@link GeoFamilyParser#parse(InputStream)}
     * rejects a stream whose {@code available()} is zero, which is what a freshly opened URL stream
     * reports. These records are tens of kilobytes.
     */
    private byte[] parseRecord( String seriesAccession, GeoScope scope, GeoFamilyParser parser,
            boolean requireContent ) {
        URL url = GeoUtils.getUrl( seriesAccession, GeoSource.DIRECT, GeoFormat.SOFT, scope, GeoAmount.BRIEF );
        File cached = this.metadataCacheFile( seriesAccession, scope );
        try {
            byte[] body;
            if ( cached != null && cached.canRead() && cached.length() > 0 ) {
                log.debug( "Reading " + cached + " instead of " + url );
                body = Files.readAllBytes( cached.toPath() );
            } else {
                body = metadataRetry.execute( EntrezUtils.retryNicely( ctx -> {
                    try ( InputStream is = url.openStream() ) {
                        return IOUtils.toByteArray( is );
                    }
                }, ncbiApiKey ), "fetch " + url );
                    if ( body.length == 0 && requireContent ) {
                    throw new RuntimeException( "GEO returned an empty document for " + url );
                }
                this.writeMetadataCacheFile( cached, body );
            }
            // acc.cgi answers a withdrawn, private or unknown accession with an HTML page and a 200,
            // not an error. Without this the SOFT parser reads the page, finds no series, and the run
            // reports "No series was parsed" -- which reads as a parser bug rather than as GEO
            // declining to serve the record. Measured on GSE6959, 2026-08-29. Checked after the cache
            // read as well as the fetch, because a page stored once would otherwise be parsed forever.
            if ( looksLikeHtml( body ) ) {
                throw new RuntimeException( "GEO served an HTML page rather than a SOFT record for "
                        + url + "; the accession is withdrawn, private or unknown to GEO." );
            }
            if ( body.length > 0 ) {
                parser.parse( new ByteArrayInputStream( body ) );
            }
            return body;
        } catch ( IOException e ) {
            throw new RuntimeException( "Failed to read " + url, e );
        }
    }

    /**
     * How many lines of a SOFT record start with the given tag.
     */
    private static int countLines( byte[] body, String tag ) {
        int n = 0;
        for ( String line : new String( body, StandardCharsets.UTF_8 ).split( "\n" ) ) {
            if ( line.startsWith( tag ) ) {
                n++;
            }
        }
        return n;
    }

    /**
     * Whether a response body is an HTML document rather than SOFT.
     * <p>
     * Checked on the first bytes only, and tolerant of a leading byte-order mark or blank line: the
     * point is to tell a served record from a served error page, not to parse HTML.
     */
    private static boolean looksLikeHtml( byte[] body ) {
        String head = new String( body, 0, Math.min( body.length, 64 ), StandardCharsets.UTF_8 )
                .replace( "\uFEFF", "" ).trim().toLowerCase( Locale.ROOT );
        return head.startsWith( "<!doctype html" ) || head.startsWith( "<html" );
    }

    /**
     * Where a metadata-only record is cached: beside the family SOFT file for the same accession,
     * under a name that cannot collide with it.
     * <p>
     * 🛑 These records are NOT the family file and must never be mistaken for it. The full download
     * is {@code <ACC>/<ACC>.soft.gz} (and {@code <ACC>_family.soft.gz} is what
     * {@code LocalSeriesFetcher} seeks); a metadata-only record holds no platform table and no
     * sample data, so anything reading one as the family file would see an experiment with no data
     * and no probes. Hence {@code <ACC>.<targ>.brief.soft}: a different extension, an uncompressed
     * file, and the GEO target it came from in the name. The directory is shared on purpose — it is
     * where everything fetched for an accession already lives, and 57,212 accessions' worth of full
     * files sit there that this must not touch.
     *
     * @return the cache file, or {@code null} when no cache directory is configured, in which case
     *         the record is fetched and used without being stored
     */
    @Nullable
    private File metadataCacheFile( String seriesAccession, GeoScope scope ) {
        String basePath = metadataCacheDir != null ? metadataCacheDir.getPath()
                : Settings.getString( "geo.local.datafile.basepath" );
        if ( StringUtils.isBlank( basePath ) ) {
            return null;
        }
        String targ = scope == GeoScope.SELF ? "self" : "gsm";
        return new File( new File( basePath, seriesAccession ), seriesAccession + "." + targ + ".brief.soft" );
    }

    /**
     * Store a fetched record. A cache that cannot be written is not a failure: the record is already
     * in hand, and the run continues without it.
     */
    private void writeMetadataCacheFile( @Nullable File cached, byte[] body ) {
        if ( cached == null ) {
            return;
        }
        if ( !cached.getName().endsWith( ".brief.soft" ) ) {
            // belt and braces: this method only ever writes metadata-only records, and writing one
            // over a family file would replace real data with a metadata stub
            throw new IllegalStateException( "Refusing to write a metadata record to " + cached );
        }
        try {
            File dir = cached.getParentFile();
            if ( dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory() ) {
                log.warn( "Could not create " + dir + "; the record was fetched but not cached." );
                return;
            }
            Files.write( cached.toPath(), body );
        } catch ( IOException e ) {
            // An IOException's message here is usually just the path again, so name the class: an
            // AccessDeniedException on /cosmos means the accession's directory predates the
            // group-writable convention -- GSE28548's is drwxr-xr-x tomcat:pavlab from 2018, and the
            // CLI container runs as uid 999 in the caller's group, so it cannot create a file there.
            // Nothing is lost when this happens: the record was fetched and is in hand, it simply
            // will not be there for the next run.
            String why = e.getClass().getSimpleName() + ": " + e.getMessage();
            if ( cacheWriteFailureReported.compareAndSet( false, true ) ) {
                log.warn( "Could not cache the GEO metadata record at " + cached + " (" + why
                        + "). The record was still fetched and used. If this is a permissions error, the"
                        + " directory is not writable by the user this process runs as; further"
                        + " occurrences are logged at DEBUG." );
            } else {
                log.debug( "Could not cache the GEO metadata record at " + cached + " (" + why + ")." );
            }
        }
    }

    /**
     * Download and parse GEO platform(s) using series accession(s).
     */
    private Collection<GeoPlatform> processSeriesPlatforms( Collection<String> seriesAccessions, GeoFamilyParser parser ) {
        for ( String seriesAccession : seriesAccessions ) {
            this.processSeriesPlatforms( seriesAccession, parser );
        }
        return parser.getResults().iterator().next().getPlatformMap().values();

    }

    private Collection<GeoPlatform> processSeriesPlatforms( String seriesAccession, GeoFamilyParser parser ) {
        File seriesFile = fetchSeriesFamilySoftFile( seriesAccession );
        if ( seriesFile == null ) {
            throw new RuntimeException( "No series file found for " + seriesAccession );
        }
        String seriesPath = seriesFile.getPath();

        parser.setProcessPlatformsOnly( this.processPlatformsOnly );
        try {
            parser.parse( seriesPath );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }
        return parser.getResults().iterator().next().getPlatformMap().values();
    }
}
