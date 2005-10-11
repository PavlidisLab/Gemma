/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDomainObjectGenerator implements SourceDomainObjectGenerator {

    private static Log log = LogFactory.getLog( GeoDomainObjectGenerator.class.getName() );

    private Fetcher datasetFetcher;
    private Fetcher seriesFetcher;

    /**
     * 
     *
     */
    public GeoDomainObjectGenerator() {
        try {
            datasetFetcher = new DatasetFetcher();
            seriesFetcher = new SeriesFetcher();
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.SourceDomainObjectGenerator#generate(java.lang.String)
     */

    public Collection<Object> generate( String geoDataSetAccession ) {
        Collection<LocalFile> result = datasetFetcher.fetch( geoDataSetAccession );

        if ( result == null ) return null;

        LocalFile dataSetFile = ( result.iterator() ).next();
        String dataSetPath;
        try {
            dataSetPath = ( new URI( dataSetFile.getLocalURI() ) ).getPath();
        } catch ( URISyntaxException e ) {
            throw new IllegalStateException( e );
        }

        GeoFamilyParser gfp = new GeoFamilyParser();

        try {
            gfp.parse( dataSetPath );

            GeoParseResult results = ( GeoParseResult ) gfp.getResults().iterator().next();

            Map<String, GeoDataset> datasetMap = results.getDatasets();
            if ( !datasetMap.containsKey( geoDataSetAccession ) )
                throw new IllegalStateException( "Failed to get parse of " + geoDataSetAccession );

            GeoDataset gds = datasetMap.get( geoDataSetAccession );

            Collection<GeoSeries> seriesSet = gds.getSeries();

            /*
             * FIXME note that when we do this, if the series has more than one data set, we get all the samples here.
             * That means that some samples will not be associated with the correct dataset information? For a dataset,
             * the samples are listed under the 'subset' information.
             */
            for ( GeoSeries series : seriesSet ) {
                log.info( "Processing series " + series );

                // fetch series referred to by the dataset.
                Collection<LocalFile> fullSeries = seriesFetcher.fetch( series.getGeoAccesssion() );
                LocalFile seriesFile = ( fullSeries.iterator() ).next();
                String seriesPath;
                try {
                    seriesPath = ( new URI( seriesFile.getLocalURI() ) ).getPath();
                } catch ( URISyntaxException e ) {
                    throw new IllegalStateException( e );
                }
                gfp.parse( seriesPath );

                // Fetch any raw data files
                RawDataFetcher rawFetcher = new RawDataFetcher();
                Collection<LocalFile> rawFiles = rawFetcher.fetch( series.getGeoAccesssion() );
                if ( rawFiles != null ) {
                    // FIXME do something useful. These are usually (always?) CEL files so they can be parsed and
                    // assembled or left alone.
                    log.info( "Downloaded raw data files" );
                }
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return gfp.getResults();
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
