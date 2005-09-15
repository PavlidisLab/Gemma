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
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.loaderutils.Converter;
import edu.columbia.gemma.loader.loaderutils.Persister;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetService {

    private static Log log = LogFactory.getLog( GeoDatasetService.class.getName() );
    private Persister expLoader;
    private Converter converter;

    /**
     * Given a GEO data set id:
     * <ol>
     * <li>Download, parse and convert the GDS file</li>
     * <li>Download, parse and convert the associated GSE family file(s)</li>
     * <li>Load the resulting data into Gemma</li>
     * </ol>
     * 
     * @param geoDataSetAccession
     */
    public void fetchAndLoad( String geoDataSetAccession ) throws ConfigurationException, SocketException, IOException {
        DatasetFetcher df = new DatasetFetcher();
        Collection<LocalFile> result = df.fetch( geoDataSetAccession );

        if ( result == null ) return;

        LocalFile dataSetFile = ( result.iterator() ).next();
        String dataSetPath;
        try {
            dataSetPath = ( new URI( dataSetFile.getLocalURI() ) ).getPath();
        } catch ( URISyntaxException e ) {
            throw new IllegalStateException( e );
        }

        GeoFamilyParser gfp = new GeoFamilyParser();

        gfp.parse( dataSetPath ); // this is like the "preparse" step.

        Map<String, GeoDataset> datasetMap = gfp.getDatasets();
        if ( !datasetMap.containsKey( geoDataSetAccession ) )
            throw new IllegalStateException( "Failed to get parse of " + geoDataSetAccession );

        GeoDataset gds = datasetMap.get( geoDataSetAccession );

        Collection<GeoSeries> seriesSet = gds.getSeries();
        SeriesFetcher sf = new SeriesFetcher();
        for ( GeoSeries series : seriesSet ) {
            log.info( "Processing series " + series );
            Collection<LocalFile> fullSeries = sf.fetch( series.getGeoAccesssion() ); // fetch series referred to by
            // the dataset.
            LocalFile seriesFile = ( fullSeries.iterator() ).next();
            String seriesPath;
            try {
                seriesPath = ( new URI( seriesFile.getLocalURI() ) ).getPath();
            } catch ( URISyntaxException e ) {
                throw new IllegalStateException( e );
            }
            gfp.parse( seriesPath );
        }

        ExpressionExperiment expexp = ( ExpressionExperiment ) converter.convert( gfp.getDatasets().values()
                .iterator().next() );

        if ( expexp == null ) throw new NullPointerException( "Got a null expressionExpression " );

        log.info( "Loading expressionExperiment: " + expexp.getAccession().getAccession() );
        assert expLoader != null;
        expLoader.persist( expexp );
    }

    /**
     * @param expressionLoader
     */
    public void setPersister( PersisterHelper expressionLoader ) {
        this.expLoader = expressionLoader;
    }

    /**
     * @param geoConv to set
     */
    public void setConverter( GeoConverter geoConv ) {
        this.converter = geoConv;
    }

}
