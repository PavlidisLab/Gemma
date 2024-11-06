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
import ubic.gemma.core.loader.expression.geo.model.*;
import ubic.gemma.core.loader.util.fetcher.Fetcher;
import ubic.gemma.core.loader.util.sdo.SourceDomainObjectGenerator;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;

import java.io.File;
import java.io.IOException;
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

    protected static Log log = LogFactory.getLog( GeoDomainObjectGenerator.class.getName() );

    private final Fetcher datasetFetcher;
    private final Fetcher seriesFetcher;
    private final Fetcher platformFetcher;

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
    public Collection<DatabaseEntry> getProjectedAccessions( String geoAccession ) {
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GEO" );
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

        Collection<File> fullSeries = seriesFetcher.fetch( seriesAccession );
        if ( fullSeries == null ) {
            GeoDomainObjectGenerator.log.warn( "No series file found for " + seriesAccession );
            return null;
        }
        File seriesFile = ( fullSeries.iterator() ).next();
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
     * Download and parse GEO platform(s) using series accession(s).
     */
    private Collection<GeoPlatform> processSeriesPlatforms( Collection<String> seriesAccessions, GeoFamilyParser parser ) {
        for ( String seriesAccession : seriesAccessions ) {
            this.processSeriesPlatforms( seriesAccession, parser );
        }
        return parser.getResults().iterator().next().getPlatformMap().values();

    }

    private Collection<GeoPlatform> processSeriesPlatforms( String seriesAccession, GeoFamilyParser parser ) {
        Collection<File> fullSeries = seriesFetcher.fetch( seriesAccession );
        if ( fullSeries == null ) {
            throw new RuntimeException( "No series file found for " + seriesAccession );
        }
        File seriesFile = ( fullSeries.iterator() ).next();
        String seriesPath;

        seriesPath = seriesFile.getPath();

        parser.setProcessPlatformsOnly( this.processPlatformsOnly );
        try {
            parser.parse( seriesPath );
        } catch ( IOException e1 ) {
            throw new RuntimeException( e1 );
        }
        return parser.getResults().iterator().next().getPlatformMap().values();
    }
}
