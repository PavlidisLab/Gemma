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
package edu.columbia.gemma.loader.expression.geo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.loaderutils.Fetcher;
import edu.columbia.gemma.loader.loaderutils.SourceDomainObjectGenerator;

/**
 * Given a GEO data set id:
 * <ol>
 * <li>Download and parse GDS file</li>
 * <li>Download and parse the corresponding GSE family file(s)</li>
 * <li>Download and unpack the corresponding raw data files (if present)</li>
 * </ol>
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDomainObjectGenerator implements SourceDomainObjectGenerator {

    private static Log log = LogFactory.getLog( GeoDomainObjectGenerator.class.getName() );

    private Fetcher datasetFetcher;
    private Fetcher seriesFetcher;
    private GeoFamilyParser gfp = new GeoFamilyParser();

    private boolean generateHasBeenCalled = false;
    private Collection<String> doneDatasets;

    private Collection<String> doneSeries;

    /**
     * 
     *
     */
    public GeoDomainObjectGenerator() {
        reset();
    }

    /**
     * 
     */
    public void reset() {
        try {
            datasetFetcher = new DatasetFetcher();
            seriesFetcher = new SeriesFetcher();
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
        doneDatasets = new HashSet<String>();
        doneSeries = new HashSet<String>();
        this.gfp = new GeoFamilyParser();
        this.generateHasBeenCalled = false;
    }

    /**
     * @param geoDataSetAccession
     * @param addOn - Don't start a new generation, add it to the existing results.
     * @return
     */
    @SuppressWarnings("unused")
    private Collection<Object> generate( String geoDataSetAccession, boolean addOn ) {

        Collection<LocalFile> result = datasetFetcher.fetch( geoDataSetAccession );

        if ( result == null ) return null;

        LocalFile dataSetFile = ( result.iterator() ).next();
        String dataSetPath;
        try {
            dataSetPath = ( new URI( dataSetFile.getLocalURI() ) ).getPath();
        } catch ( URISyntaxException e ) {
            throw new IllegalStateException( e );
        }

        // FIXME we don't get a single ExpressionExperiment out of this - we get one for each dataset.
        try {
            gfp.parse( dataSetPath );

            GeoParseResult results = ( GeoParseResult ) gfp.getResults().iterator().next();

            Map<String, GeoDataset> datasetMap = results.getDatasets();
            if ( !datasetMap.containsKey( geoDataSetAccession ) )
                throw new IllegalStateException( "Failed to get parse of " + geoDataSetAccession );

            GeoDataset gds = datasetMap.get( geoDataSetAccession );

            Collection<GeoSeries> seriesSet = gds.getSeries();

            for ( GeoSeries series : seriesSet ) {
                if ( doneSeries.contains( series.getGeoAccession() ) ) continue;

                processSeries( series );

                processRawData( series );

                processSeriesDatasets( geoDataSetAccession, series );

            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        // fixme - we have to use this information.
        GeoSampleCorrespondence correspondence = DatasetCombiner.findGSECorrespondence( ( ( GeoParseResult ) gfp
                .getResults().iterator().next() ).getDatasetMap().values() );

        doneDatasets.add( geoDataSetAccession );
        return gfp.getResults();
    }

    /**
     * @param series
     * @throws IOException
     */
    private void processSeries( GeoSeries series ) throws IOException {
        log.info( "Processing series " + series );

        // fetch series referred to by the dataset.
        Collection<LocalFile> fullSeries = seriesFetcher.fetch( series.getGeoAccession() );
        LocalFile seriesFile = ( fullSeries.iterator() ).next();
        String seriesPath;
        try {
            seriesPath = ( new URI( seriesFile.getLocalURI() ) ).getPath();
        } catch ( URISyntaxException e ) {
            throw new IllegalStateException( e );
        }
        gfp.parse( seriesPath );
        doneSeries.add( series.getGeoAccession() );
    }

    /**
     * Fetch any raw data files
     * 
     * @param series
     */
    private void processRawData( GeoSeries series ) {

        RawDataFetcher rawFetcher = new RawDataFetcher();
        Collection<LocalFile> rawFiles = rawFetcher.fetch( series.getGeoAccession() );
        if ( rawFiles != null ) {
            // FIXME maybe do something more. These are usually (always?) CEL files so they can be parsed and
            // assembled or left alone.
            log.info( "Downloaded raw data files" );
        }
    }

    /**
     * @param geoDataSetAccession
     * @param series
     */
    private void processSeriesDatasets( String geoDataSetAccession, GeoSeries series ) {
        Collection<String> datasets = DatasetCombiner.findGDSforGSE( series.getGeoAccession() );

        // sanity check.
        if ( !datasets.contains( geoDataSetAccession ) ) {
            throw new IllegalStateException( "Somehow " + geoDataSetAccession + " isn't in "
                    + series.getGeoAccession() );
        }

        if ( datasets.size() > 1 ) {
            log.info( "Multiple datasets for " + series.getGeoAccession() );
            for ( Iterator iter = datasets.iterator(); iter.hasNext(); ) {
                String dataset = ( String ) iter.next();
                if ( doneDatasets.contains( dataset ) ) continue;
                this.generate( dataset, true );
            }
        }
    }

    /**
     * Unlike some other generators, this method can only be called once without calling 'reset'.
     * 
     * @see edu.columbia.gemma.loader.loaderutils.SourceDomainObjectGenerator#generate(java.lang.String)
     */
    public Collection<Object> generate( String geoDataSetAccession ) {
        if ( generateHasBeenCalled )
            throw new IllegalArgumentException(
                    "You can only call 'generate' once on this without calling 'reset()' first." );

        generateHasBeenCalled = true;

        return this.generate( geoDataSetAccession, false );

    }

    /**
     * @param datasetFetcher The datasetFetcher to set.
     */
    public void setDatasetFetcher( Fetcher df ) {
        this.datasetFetcher = df;
    }

    /**
     * @param seriesFetcher The seriesFetcher to set.
     */
    public void setSeriesFetcher( Fetcher seriesFetcher ) {
        this.seriesFetcher = seriesFetcher;
    }

}
